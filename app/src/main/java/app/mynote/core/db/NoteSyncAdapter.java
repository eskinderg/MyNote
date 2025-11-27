package app.mynote.core.db;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import app.mynote.auth.AuthConfig;
import app.mynote.core.callback.AppCallback;
import app.mynote.core.db.auth.AccountGeneral;
import app.mynote.fragments.note.Note;
import app.mynote.fragments.note.NoteService;
import app.mynote.fragments.note.NotesDataService;
import app.mynote.service.RetroInstance;
import retrofit2.Call;
import retrofit2.Retrofit;

public class NoteSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SYNC_ADAPTER";

    private final ContentResolver resolver;

    public NoteSyncAdapter(Context c, boolean autoInit) {
        this(c, autoInit, false);
    }

    public NoteSyncAdapter(Context c, boolean autoInit, boolean parallelSync) {
        super(c, autoInit, parallelSync);
        this.resolver = c.getContentResolver();
    }

    /**
     * Manual force Android to perform a sync with our SyncAdapter.
     */
    public static void performSync() {
        // First cancel any ongoing sync.
        cancelSync();
        Bundle b = new Bundle();
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(AccountGeneral.getAccount(),
                NoteContract.CONTENT_AUTHORITY, b);
    }

    public static void cancelSync() {
        ContentResolver.cancelSync(AccountGeneral.getAccount(), NoteContract.CONTENT_AUTHORITY);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        if (AuthConfig.USER != null) {

            Log.i(TAG, "Starting synchronization...");

            try {
                syncNotes(syncResult);
                // Add any other things you may want to sync

            } catch (IOException ex) {
                Log.e(TAG, "Error synchronizing!", ex);
                syncResult.stats.numIoExceptions++;
            } catch (JSONException ex) {
                Log.e(TAG, "Error synchronizing!", ex);
                syncResult.stats.numParseExceptions++;
            } catch (RemoteException | OperationApplicationException ex) {
                Log.e(TAG, "Error synchronizing!", ex);
                syncResult.stats.numAuthExceptions++;
            }

        }
    }

    private void syncNotes(SyncResult syncResult) throws IOException, JSONException, RemoteException, OperationApplicationException {
        Map<String, Note> localEntries = new HashMap<>();
        Map<String, Note> pushEntries = new HashMap<>();

        // We need to collect all the network items in a hash table
        Log.i(TAG, "Fetching server entries...");

        Retrofit retrofit = RetroInstance.getRetrofitInstance();
        NotesDataService notesApi = retrofit.create(NotesDataService.class);
        Call<Note[]> call = notesApi.getNotes();
        call.enqueue(new AppCallback<Note[]>(getContext()) {
            @Override
            public void onResponse(Note[] response) {

                try {
                    Note found;
                    Map<String, Note> networkEntries = new HashMap<>();

                    ArrayList<ContentProviderOperation> batch = new ArrayList<>();

                    // Compare the hash table of network entries to all the local entries
                    Log.i(TAG, "Fetching local entries...");
                    Cursor c = resolver.query(NoteContract.Notes.CONTENT_URI, null, NoteContract.Notes.COL_USER_ID + " = '" + AuthConfig.USER.getId() + "'", null, null, null);
                    assert c != null;

                    for (Note remoteNote : response) {
                        networkEntries.put(remoteNote.getId() + remoteNote.getUserId(), remoteNote);
                        if (!remoteNote.getActive()) {
                            batch.add(ContentProviderOperation.newDelete(NoteContract.Notes.CONTENT_URI) // delete notes locally  that are not active on the server
                                    .withSelection(NoteContract.Notes.COL_ID + "='" + remoteNote.getId() + "'", null)
                                    .build());
                        }
                    }

                    c.moveToFirst();

                    for (int i = 0; i < c.getCount(); i++) {
                        syncResult.stats.numEntries++;
                        Note noteLocal = NoteService.noteMapper(c);

                        if (!noteLocal.getActive()) {
                            batch.add(ContentProviderOperation.newDelete(NoteContract.Notes.CONTENT_URI) // delete notes that are not active locally
                                    .withSelection(NoteContract.Notes.COL_ID + "='" + noteLocal.getId() + "'", null)
                                    .build());
                        }

                        // Try to retrieve the local entry from network entries
                        found = networkEntries.get(noteLocal.getId() + noteLocal.getUserId());

                        archives(localEntries, batch, noteLocal, found);
                        pinOrder(localEntries, batch, noteLocal, found);
                        deleted(localEntries, batch, noteLocal, found);

                        if (!(found == null)) {
                            int des = found.getDateModified().compareTo(noteLocal.getDateModified());

                            if (des > 0) {
                                addOperation(batch, ContentProviderOperation.newUpdate(NoteContract.Notes.CONTENT_URI), found);

                            } else if (des < 0) {
                                Log.i(TAG, found.getHeader() + ": New update found and Need to be sent to the server");
                                localEntries.put(noteLocal.getId() + noteLocal.getUserId(), noteLocal);
                            } else {
//                                Log.w("Eskinder", found.getHeader() + "=0");
                            }

                            networkEntries.remove(noteLocal.getId() + noteLocal.getUserId());
                        } else {
                            // new items found on local
                            pushEntries.put(noteLocal.getId() + noteLocal.getUserId(), noteLocal);
                        }

                        c.moveToNext();
                    }
                    c.close();

                    if (!pushEntries.isEmpty()) {
                        Retrofit localToRemoteRetrofit = RetroInstance.getRetrofitInstance();
                        NotesDataService localToRemoteNotesApi = localToRemoteRetrofit.create(NotesDataService.class);
                        Call<Note[]> callLocalToRemote = localToRemoteNotesApi.insert(new ArrayList<>(pushEntries.values()));
                        callLocalToRemote.enqueue(new AppCallback<Note[]>(getContext()) {
                            @Override
                            public void onResponse(Note[] response) {

                                try {
                                    //resolver.applyBatch(NoteContract.CONTENT_AUTHORITY, ops);
                                    ArrayList<ContentProviderOperation> responseBatch = new ArrayList<>();

                                    for (Note note : response) {

//                                        responseBatch.add(ContentProviderOperation.newDelete(NoteContract.Notes.CONTENT_URI) //
//                                                .withSelection(NoteContract.Notes.COL_ID + "='" + note.getId() + "'", null)
//                                                .build());
//                                        responseBatch.add(ContentProviderOperation.newDelete(NoteContract.BASE_CONTENT_URI).withSelection(NoteContract.Notes.COL_ID + "=", ) .build());
//                                        addOperation(responseBatch, ContentProviderOperation.newDelete(NoteContract.Notes.CONTENT_URI), note);
                                        addOperation(responseBatch, ContentProviderOperation.newUpdate(NoteContract.Notes.CONTENT_URI), note);
                                    }

                                    resolver.applyBatch(NoteContract.CONTENT_AUTHORITY, responseBatch);
                                    resolver.notifyChange(NoteContract.Notes.CONTENT_URI, null);
//                                    resolver.notifyChange(NoteContract.Notes.CONTENT_URI, // URI where data was modified
//                                            null, // No local observer
//                                            ContentResolver.NOTIFY_INSERT); // IMPORTANT: Do not sync to network
                                }catch (Exception e) {
                                    Log.e(TAG, response.toString() );
                                }
                                Log.e(TAG, response.toString() );
                                pushEntries.clear();
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                Log.e(TAG, "Error pushing local changes " + throwable.getMessage());
                            }
                        });
                    }
                    pushEntries.clear();

                    // Add all the new entries
                    for (Note note : networkEntries.values()) {
                        Log.i(TAG, "Scheduling insert: " + note.getHeader());
                        if (note.getActive()) {
                            batch.add(ContentProviderOperation.newInsert(NoteContract.Notes.CONTENT_URI)
                                    .withValue(NoteContract.Notes.COL_ID, note.getId())
                                    .withValue(NoteContract.Notes.COL_HEADER, note.getHeader())
                                    .withValue(NoteContract.Notes.COL_TEXT, note.getText())
                                    .withValue(NoteContract.Notes.COL_USER_ID, note.getUserId())
                                    .withValue(NoteContract.Notes.COL_COLOUR, note.getColour())
                                    .withValue(NoteContract.Notes.COL_SELECTION, note.getSelection())
                                    .withValue(NoteContract.Notes.COL_ARCHIVED, note.getArchived())
                                    .withValue(NoteContract.Notes.COL_PINNED, note.getPinned())
                                    .withValue(NoteContract.Notes.COL_ACTIVE, note.getActive())
                                    .withValue(NoteContract.Notes.COL_SPELL_CHECK, note.getSpellCheck())
                                    .withValue(NoteContract.Notes.COL_PIN_ORDER, note.getPinOrder())
                                    .withValue(NoteContract.Notes.COL_DATE_CREATED, note.getDateCreated().toString())
                                    .withValue(NoteContract.Notes.COL_DATE_ARCHIVED, note.getDateArchived().toString())
                                    .withValue(NoteContract.Notes.COL_DATE_MODIFIED, note.getDateModified().toString())
                                    .withValue(NoteContract.Notes.COL_DATE_DELETED, note.getDateDeleted().toString())
                                    .withValue(NoteContract.Notes.COL_LAST_MODIFIED_DATE, note.getLastModifiedDate().toString())
                                    .withValue(NoteContract.Notes.COL_DATE_SYNC, note.getDateSync())
                                    .withValue(NoteContract.Notes.COL_SYNCED, note.getIsSync())
                                    .withValue(NoteContract.Notes.COL_OWNER, note.getOwner())
                                    .build());
                            syncResult.stats.numInserts++;
                        }
                    }

                    if (!localEntries.isEmpty()) {
                        Retrofit remoteRetrofit = RetroInstance.getRetrofitInstance();
                        NotesDataService remoteNotesApi = remoteRetrofit.create(NotesDataService.class);
                        Call<Note[]> callRemote = remoteNotesApi.updateNote(new ArrayList<>(localEntries.values()));
                        callRemote.enqueue(new AppCallback<Note[]>(getContext()) {
                            @Override
                            public void onResponse(Note[] response) {
                                try {
                                    ArrayList<ContentProviderOperation> responseBatch = new ArrayList<>();

                                    for (Note note : response) {
//                                        note.setIsSync(true);
                                        addOperation(responseBatch, ContentProviderOperation.newUpdate(NoteContract.Notes.CONTENT_URI), note);
                                    }

                                    resolver.applyBatch(NoteContract.CONTENT_AUTHORITY, responseBatch);
                                    resolver.notifyChange(NoteContract.Notes.CONTENT_URI, // URI where data was modified
                                            null, // No local observer
                                            ContentResolver.NOTIFY_UPDATE); // IMPORTANT: Do not sync to network

                                } catch (Exception ex) {
                                    Log.e(TAG, "Error updating local data", ex);
                                }
                                Log.i("SERVER", response.length + " Items updated and synced successfully");
                            }

                            @Override
                            public void onFailure(Throwable throwable) {
                                Log.e(TAG, "Error happend");
                            }
                        });
                    }

                    Log.i(TAG, "Merge solution ready, applying batch update...");
                    resolver.applyBatch(NoteContract.CONTENT_AUTHORITY, batch);
                    resolver.notifyChange(NoteContract.Notes.CONTENT_URI, // URI where data was modified
                            null, // No local observer
                            ContentResolver.NOTIFY_INSERT | ContentResolver.NOTIFY_UPDATE | ContentResolver.NOTIFY_DELETE); // IMPORTANT: Do not sync to network

                    Log.i(TAG, "Finished synchronization!");

                } catch (Exception ex) {
                    Log.e(TAG, "Error synchronizing!", ex);
                    syncResult.stats.numAuthExceptions++;
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e(TAG, "Error on sync operation " + throwable.getMessage());
            }
        });
    }

    private void archives(Map<String, Note> localEntries, ArrayList<ContentProviderOperation> batch, Note localNote, Note remoteNote) {
        if (remoteNote != null) {
            if (localNote.getDateArchived() != null && remoteNote.getDateArchived() != null) {
                int comp = localNote.getDateArchived().compareTo(remoteNote.getDateArchived());

                if (comp < 0){
                    addOperation(batch, ContentProviderOperation.newUpdate(NoteContract.Notes.CONTENT_URI), remoteNote);
                }

                if (comp > 0)
                    localEntries.put(localNote.getId() + localNote.getUserId(), localNote);
            }

            if (localNote.getDateArchived() != null && remoteNote.getDateArchived() == null)
                localEntries.put(localNote.getId() + localNote.getUserId(), localNote);


            if (localNote.getDateArchived() == null && remoteNote.getDateArchived() != null) {
                addOperation(batch, ContentProviderOperation.newUpdate(NoteContract.Notes.CONTENT_URI), remoteNote);
            }
        }
    }

    private void pinOrder(Map<String, Note> localEntries, ArrayList<ContentProviderOperation> batch, Note localNote, Note remoteNote) {
        if (remoteNote != null) {
            if (localNote.getPinOrder() != null && remoteNote.getPinOrder() != null) {

                int comp = localNote.getPinOrder().compareTo(remoteNote.getPinOrder());

                if (comp < 0){
                    addOperation(batch, ContentProviderOperation.newUpdate(NoteContract.Notes.CONTENT_URI), remoteNote);
                }

                if (comp > 0)
                    localEntries.put(localNote.getId() + localNote.getUserId(), localNote);
            }

            if (localNote.getPinOrder() != null && remoteNote.getPinOrder() == null)
                localEntries.put(localNote.getId() + localNote.getUserId(), localNote);

            if (localNote.getPinOrder() == null && remoteNote.getPinOrder() != null)
                addOperation(batch, ContentProviderOperation.newUpdate(NoteContract.Notes.CONTENT_URI), remoteNote);
        }
    }

    private void deleted(Map<String, Note> localEntries, ArrayList<ContentProviderOperation> batch, Note localNote, Note remoteNote) {
        if (remoteNote != null) {
            if (localNote.getDateDeleted() != null && remoteNote.getDateDeleted() != null) {
                int comp = localNote.getDateDeleted().compareTo(remoteNote.getDateDeleted());

                if (comp < 0){
                    addOperation(batch, ContentProviderOperation.newUpdate(NoteContract.Notes.CONTENT_URI), remoteNote);
                }

                if (comp > 0)
                    localEntries.put(localNote.getId() + localNote.getUserId(), localNote);
            }

            if (localNote.getDateDeleted() != null && remoteNote.getDateDeleted() == null)
                localEntries.put(localNote.getId() + localNote.getUserId(), localNote);


            if (localNote.getDateDeleted() == null && remoteNote.getDateDeleted() != null) {
                addOperation(batch, ContentProviderOperation.newUpdate(NoteContract.Notes.CONTENT_URI), remoteNote);
            }
        }
    }

    private void addOperation(ArrayList<ContentProviderOperation> batch, ContentProviderOperation.Builder operation, Note note) {
        batch.add(operation
                .withSelection(NoteContract.Notes.COL_ID + "='" + note.getId() + "'", null)
                .withValue(NoteContract.Notes.COL_ID, note.getId())
                .withValue(NoteContract.Notes.COL_HEADER, note.getHeader())
                .withValue(NoteContract.Notes.COL_TEXT, note.getText())
                .withValue(NoteContract.Notes.COL_USER_ID, note.getUserId())
                .withValue(NoteContract.Notes.COL_COLOUR, note.getColour())
                .withValue(NoteContract.Notes.COL_SELECTION, note.getSelection())
                .withValue(NoteContract.Notes.COL_ARCHIVED, note.getArchived())
                .withValue(NoteContract.Notes.COL_PINNED, note.getPinned())
                .withValue(NoteContract.Notes.COL_ACTIVE, note.getActive())
                .withValue(NoteContract.Notes.COL_SPELL_CHECK, note.getSpellCheck())
                .withValue(NoteContract.Notes.COL_PIN_ORDER, note.getPinOrder())
                .withValue(NoteContract.Notes.COL_DATE_CREATED, note.getDateCreated().toString())
                .withValue(NoteContract.Notes.COL_DATE_ARCHIVED, note.getDateArchived().toString())
                .withValue(NoteContract.Notes.COL_DATE_MODIFIED, note.getDateModified().toString())
                .withValue(NoteContract.Notes.COL_DATE_DELETED, note.getDateDeleted().toString())
                .withValue(NoteContract.Notes.COL_LAST_MODIFIED_DATE, note.getLastModifiedDate().toString())
                .withValue(NoteContract.Notes.COL_DATE_SYNC, note.getDateSync())
                .withValue(NoteContract.Notes.COL_SYNCED, note.getIsSync())
                .withValue(NoteContract.Notes.COL_OWNER, note.getOwner())
                .build());
    }
}
