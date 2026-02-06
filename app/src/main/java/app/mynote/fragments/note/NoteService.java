package app.mynote.fragments.note;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import app.mynote.auth.AuthConfig;
import app.mynote.core.db.NoteContract;
import app.mynote.core.utils.AppDate;
import app.mynote.core.utils.AppTimestamp;

public class NoteService {

    public static Note add(Context c, Note note) {
        ContentValues values = new ContentValues();
        values.put(NoteContract.Notes.COL_ID, note.getId());
        values.put(NoteContract.Notes.COL_TEXT, note.getText());
        values.put(NoteContract.Notes.COL_HEADER, note.getHeader());
        values.put(NoteContract.Notes.COL_USER_ID, AuthConfig.USER.getId());
        values.put(NoteContract.Notes.COL_SELECTION, note.getSelection());
        values.put(NoteContract.Notes.COL_ARCHIVED, note.getArchived());
        values.put(NoteContract.Notes.COL_PINNED, note.getPinned());
        values.put(NoteContract.Notes.COL_COLOUR, note.getColour());
        values.put(NoteContract.Notes.COL_ACTIVE, note.isActive());
        values.put(NoteContract.Notes.COL_READONLY, note.getReadonly());
        values.put(NoteContract.Notes.COL_SPELL_CHECK, note.getSpellCheck());
        values.put(NoteContract.Notes.COL_PIN_ORDER, note.getPinOrder());
        values.put(NoteContract.Notes.COL_DATE_CREATED, note.getDateCreated().toString());
        values.put(NoteContract.Notes.COL_DATE_MODIFIED, note.getDateModified().toString());
        values.put(NoteContract.Notes.COL_DATE_ARCHIVED, note.getDateArchived().toString());
        values.put(NoteContract.Notes.COL_DATE_DELETED, note.getDateDeleted().toString());
        values.put(NoteContract.Notes.COL_LAST_MODIFIED_DATE, note.getLastModifiedDate().toString());
        values.put(NoteContract.Notes.COL_DATE_SYNC, note.getDateSync());
        values.put(NoteContract.Notes.COL_SYNCED, note.getIsSync());
        values.put(NoteContract.Notes.COL_OWNER, note.getOwner());
        c.getContentResolver().insert(NoteContract.Notes.CONTENT_URI, values);
        return note;
    }

    public static Note update(Context c, Note note, boolean markModified) {
        ContentValues values = new ContentValues();
        values.put(NoteContract.Notes.COL_TEXT, note.getText());
        values.put(NoteContract.Notes.COL_HEADER, note.getHeader());
        values.put(NoteContract.Notes.COL_USER_ID, AuthConfig.USER.getId());
        values.put(NoteContract.Notes.COL_SELECTION, note.getSelection());
        values.put(NoteContract.Notes.COL_ARCHIVED, note.getArchived());
        values.put(NoteContract.Notes.COL_PINNED, note.getPinned());
        values.put(NoteContract.Notes.COL_COLOUR, note.getColour());
        values.put(NoteContract.Notes.COL_ACTIVE, note.isActive());
        values.put(NoteContract.Notes.COL_READONLY, note.getReadonly());
        values.put(NoteContract.Notes.COL_SPELL_CHECK, note.getSpellCheck());
        values.put(NoteContract.Notes.COL_PIN_ORDER, note.getPinOrder());
        values.put(NoteContract.Notes.COL_DATE_CREATED, note.getDateCreated().toString());
        if(markModified){
            values.put(NoteContract.Notes.COL_DATE_MODIFIED, AppDate.Now());
            values.put(NoteContract.Notes.COL_LAST_MODIFIED_DATE, AppDate.Now().toString());
        }else{
            values.put(NoteContract.Notes.COL_DATE_MODIFIED, note.getDateModified().toString());
            values.put(NoteContract.Notes.COL_LAST_MODIFIED_DATE, note.getLastModifiedDate().toString());
        }
        values.put(NoteContract.Notes.COL_SYNCED, false);
        values.put(NoteContract.Notes.COL_DATE_ARCHIVED, note.getDateArchived().toString());
        values.put(NoteContract.Notes.COL_DATE_DELETED, note.getDateDeleted().toString());
//        values.put(NoteContract.Notes.COL_LAST_MODIFIED_DATE, note.getLastModifiedDate().toString());
        values.put(NoteContract.Notes.COL_DATE_SYNC, note.getDateSync());
        values.put(NoteContract.Notes.COL_OWNER, note.getOwner());
        c.getContentResolver().update(NoteContract.Notes.CONTENT_URI, values, NoteContract.Notes.COL_ID + " = ?",
                new String[]{String.valueOf(note.getId())});
        return note;
    }


    public static ArrayList<Note> getAllNotes(Context ctx) {
        ArrayList<Note> noteArrayList = new ArrayList<Note>();
//        String[] where ={ NoteContract.Notes.COL_USER_ID + " = " + AuthConfig.USER.getId();

        Cursor c = ctx.getContentResolver().query(NoteContract.Notes.CONTENT_URI, null, NoteContract.Notes.COL_USER_ID + " = '" + AuthConfig.USER.getId() + "'", null, null, null);
        assert c != null;
        if (c.moveToFirst()) {
            do {
                Note note = noteMapper(c);
                // adding to list
                noteArrayList.add(note);
            } while (c.moveToNext());
        }
        c.close();
        return noteArrayList;
    }

    public static Note noteMapper(Cursor c) {
        Note note = new Note();
        note.setId(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_ID)));
        note.setHeader(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_HEADER)));
        note.setUserId(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_USER_ID)));
        note.setText(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_TEXT)));
        note.setSelection(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_SELECTION)));
        note.setColour(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_COLOUR)));
        note.setArchived(c.getInt(c.getColumnIndexOrThrow(NoteContract.Notes.COL_ARCHIVED)) > 0);
        note.setPinned(c.getInt(c.getColumnIndexOrThrow(NoteContract.Notes.COL_PINNED)) > 0);
        note.setActive(c.getInt(c.getColumnIndexOrThrow(NoteContract.Notes.COL_ACTIVE)) > 0);
        note.setReadonly(c.getInt(c.getColumnIndexOrThrow(NoteContract.Notes.COL_READONLY)) > 0);
        note.setFavorite(c.getInt(c.getColumnIndexOrThrow(NoteContract.Notes.COL_FAVORITE)) > 0);
        note.setSpellCheck(c.getInt(c.getColumnIndexOrThrow(NoteContract.Notes.COL_SPELL_CHECK)) > 0);
        note.setPinOrder(c.getLong(c.getColumnIndexOrThrow(NoteContract.Notes.COL_PIN_ORDER)));
        note.setDateCreated(AppTimestamp.convertStringToTimestamp(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_DATE_CREATED))));
        note.setDateModified(AppTimestamp.convertStringToTimestamp(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_DATE_MODIFIED))));
        note.setDateArchived(AppTimestamp.convertStringToTimestamp(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_DATE_ARCHIVED))));
        note.setLastModifiedDate(AppTimestamp.convertStringToTimestamp(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_LAST_MODIFIED_DATE))));
        note.setDateDeleted( Optional.ofNullable(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_DATE_DELETED)))
                        .map(AppTimestamp::convertStringToTimestamp)
                        .orElse(null)
        );
        note.setDateSync(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_DATE_SYNC)));
        note.setIsSync(c.getInt(c.getColumnIndexOrThrow(NoteContract.Notes.COL_SYNCED)) > 0);
        note.setOwner(c.getString(c.getColumnIndexOrThrow(NoteContract.Notes.COL_OWNER)));
        return note;
    }

    public static ArrayList<Note> getArchived(Context c) {
        ArrayList<Note> response = getAllNotes(c);
        return new ArrayList<>(response.stream().filter(n -> n.getArchived() && n.getActive()).collect(Collectors.toList()));
    }

    public static ArrayList<Note> getPinned(Context c) {
        ArrayList<Note> response = getAllNotes(c);
        return new ArrayList<>(response.stream().filter(n -> n.isPinned()).collect(Collectors.toList()));
    }

    public ArrayList<Note> Update(Context c, ArrayList<Note> notes) {
        for (Note note : notes) {
            ContentValues values = new ContentValues();
            values.put(NoteContract.Notes.COL_ID, note.getId());
            values.put(NoteContract.Notes.COL_TEXT, note.getText());
            values.put(NoteContract.Notes.COL_HEADER, note.getHeader());
            values.put(NoteContract.Notes.COL_USER_ID, AuthConfig.USER.getId());
            values.put(NoteContract.Notes.COL_SELECTION, note.getSelection());
            values.put(NoteContract.Notes.COL_ARCHIVED, note.getArchived());
            values.put(NoteContract.Notes.COL_PINNED, note.getPinned());
            values.put(NoteContract.Notes.COL_COLOUR, note.getColour());
            values.put(NoteContract.Notes.COL_ACTIVE, note.isActive());
            values.put(NoteContract.Notes.COL_READONLY, note.getReadonly());
            values.put(NoteContract.Notes.COL_SPELL_CHECK, note.getSpellCheck());
            values.put(NoteContract.Notes.COL_PIN_ORDER, note.getPinOrder());
            values.put(NoteContract.Notes.COL_DATE_CREATED, note.getDateCreated().toString());
            values.put(NoteContract.Notes.COL_DATE_MODIFIED, note.getDateModified().toString());
            values.put(NoteContract.Notes.COL_DATE_ARCHIVED, note.getDateArchived().toString());
            values.put(NoteContract.Notes.COL_DATE_DELETED, note.getDateDeleted().toString());
            values.put(NoteContract.Notes.COL_LAST_MODIFIED_DATE, note.getLastModifiedDate().toString());
            values.put(NoteContract.Notes.COL_DATE_SYNC, note.getDateSync());
            values.put(NoteContract.Notes.COL_SYNCED, note.getIsSync());
            values.put(NoteContract.Notes.COL_OWNER, note.getOwner());
            update(c, note, true);
        }
        return notes;
    }
}
