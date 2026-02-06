package app.mynote.fragments.note;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import app.mynote.auth.UserManager;
import app.mynote.core.db.NoteContract;
import app.mynote.core.db.NoteSyncAdapter;
import app.mynote.core.utils.AppDate;
import app.mynote.core.utils.AppTimestamp;
import app.mynote.core.utils.GsonParser;
import app.mynote.fragments.SwipeController;
import app.mynote.fragments.note.NotesAdapter.OnNoteItemClickListener;
import mynote.R;
import mynote.databinding.FragmentNotesBinding;

public class NotesFragment extends Fragment implements OnNoteItemClickListener, NotesAdapter.OnNoteItemLongClickListener, SwipeRefreshLayout.OnRefreshListener, ActionMode.Callback {

    public RecyclerView recyclerView;
    public FloatingActionButton fab;
    public NotesAdapter notesAdapter;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    private FragmentNotesBinding binding;
    private NoteObserver noteObserver;
    private ActionMode actionMode;

    public NotesFragment() {
    }

      private void startActionMode() {
        if (actionMode == null) {
            actionMode = getActivity().startActionMode(this);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteObserver = new NoteObserver();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotesBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        this.recyclerView = view.findViewById(R.id.noterecyclerview);
        ArrayList<Note> notes = new ArrayList<>(NoteService.getAllNotes(getContext()));
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        this.notesAdapter = new NotesAdapter(getContext(),notes, this, this);
        this.recyclerView.setAdapter(this.notesAdapter);
        this.notesAdapter.registerAdapterDataObserver(new AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                setAppbarCount();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                setAppbarCount();
            }
        });
        registerForContextMenu(recyclerView);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        SwipeController swipeHelper = new SwipeController(getContext(), recyclerView) {
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons) {
                underlayButtons.add(new SwipeController.UnderlayButton(
                        "Pin",
                        SwipeController.getBitmapFromVectorDrawable(getContext(), R.drawable.ic_pin_white),
                        ContextCompat.getColor(getContext(), R.color.primary_light),
                        true,
                        new SwipeController.UnderlayButtonClickListener() {
                            @Override
                            public void onClick(int position) {
                                Note noteItem = notesAdapter.notesList.get(position);
                                noteItem.setPinned(!noteItem.isPinned());
                                noteItem.setPinOrder(System.currentTimeMillis());
                                NoteService.update(getContext(), noteItem, false);
                                String textMsg = noteItem.isPinned() ? "Pinned" : "Un Pinned";
                                Toast.makeText(getContext(), "Note " + textMsg, Toast.LENGTH_LONG).show();
                                notesAdapter.notifyItemChanged(position);
                            }
                        }
                ));
                underlayButtons.add(new SwipeController.UnderlayButton(
                        "Archive",
                        SwipeController.getBitmapFromVectorDrawable(getContext(), R.drawable.ic_archive),
                        ContextCompat.getColor(getContext(), R.color.orange),
                        new SwipeController.UnderlayButtonClickListener() {
                            @Override
                            public void onClick(int position) {
                                Note noteItem = notesAdapter.notesList.get(position);
                                noteItem.setArchived(true);
                                noteItem.setDateArchived(AppTimestamp.convertStringToTimestamp(AppDate.Now()));
                                NoteService.update(getContext(), noteItem, false);
                                String textMsg = "archived";
                                Toast.makeText(getContext(), "Note " + textMsg, Toast.LENGTH_LONG).show();
                                notesAdapter.notesList.remove(position);
                                notesAdapter.notifyItemRemoved(position);
                            }
                        }
                ));
                underlayButtons.add(new SwipeController.UnderlayButton(
                        "Unlock",
                        SwipeController.getBitmapFromVectorDrawable(getContext(), R.drawable.ic_lock_24),
                        ContextCompat.getColor(getContext(), R.color.primary_light),
                        0,
                        true,
                        new SwipeController.UnderlayButtonClickListener() {
                            @Override
                            public void onClick(int position) {
                                Note noteItem = notesAdapter.notesList.get(position);
                                noteItem.setReadonly(!noteItem.getReadonly());
                                NoteService.update(getContext(), noteItem, false);
                                String textMsg = noteItem.getReadonly() ? "Locked" : "UnLocked";
                                Toast.makeText(getContext(), "Note " + textMsg, Toast.LENGTH_LONG).show();
                                notesAdapter.notifyItemChanged(position);
                            }
                        }
                ));

            }
        };

        this.fab = view.findViewById(R.id.fab);

        this.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Note note = new Note();

                note.setId(UUID.randomUUID().toString());
                note.setOwner(UserManager.getUser(getContext()).getGivenName());
                note.setDateModified(AppTimestamp.convertStringToTimestamp(AppDate.Now()));
                note.setDateCreated(AppTimestamp.convertStringToTimestamp(AppDate.Now()));
                NoteService.add(getContext(), note);

                Bundle bundle = new Bundle();
                String noteJsonString = GsonParser.getGsonParser().toJson(note);
                bundle.putString("note", noteJsonString);
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.action_nav_notes_to_nav_note, bundle);
            }
        });

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.primary_light,
                R.color.primary_light,
                R.color.primary_light,
                R.color.primary_light);
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
//         getActivity().getMenuInflater().inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        if(item.getItemId() == R.id.action_context_pin){
            Toast.makeText(getContext(),"Pin toggle clicked", Toast.LENGTH_LONG).show();
            return true;
        }
        if(item.getItemId() == R.id.action_context_archive){
            Toast.makeText(getContext(),"Archived clicked", Toast.LENGTH_LONG).show();
            return true;
        }
         // Handle the selected item from the context menu
//        switch (item.getItemId()) {
            // Handle different menu item selections
//            case R.id.action:
                // Perform delete action
//                return true;
            // Handle other actions
//        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        setAppbarCount();
        getActivity().getContentResolver().registerContentObserver(
                NoteContract.Notes.CONTENT_URI,
                true,
                noteObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (noteObserver != null) {
            getActivity().getContentResolver().unregisterContentObserver(noteObserver);
        }
    }


    @Override
    public void onNoteItemClick(View view, Note note) {
        Bundle bundle = new Bundle();
        String noteJsonString = GsonParser.getGsonParser().toJson(note);
        bundle.putString("note", noteJsonString);

        NavController navController = Navigation.findNavController(view);
        navController.navigate(R.id.action_nav_notes_to_nav_note, bundle);
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        NoteSyncAdapter.performSync();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void fetchNotes() {
        ArrayList<Note> notes = new ArrayList<>(NoteService.getAllNotes(getContext()));
        NotesFragment.this.dataView(notes);
        setAppbarCount();
        recyclerView.getAdapter().notifyDataSetChanged();
    }


    private void dataView(List<Note> notes) {
        this.notesAdapter = new NotesAdapter(getContext(), notes, this, this);
        recyclerView.setAdapter(this.notesAdapter);
    }

    private void setAppbarCount() {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Notes " + "(" + NotesFragment.this.recyclerView.getAdapter().getItemCount() + ")");
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }

    @Override
    public void onNoteItemLongClick(View view, Note note) {
        PopupMenu popupMenu = new PopupMenu(getContext(),view, Gravity.END);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.action_context_archive) {
                    Toast.makeText(getContext(), note.getHeader(), Toast.LENGTH_LONG).show();
                    return true;
                }
                if(item.getItemId() == R.id.action_context_pin) {
                    Toast.makeText(getContext(), note.getHeader(), Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }
        });
        popupMenu.inflate(R.menu.context_menu);
        popupMenu.show();
    }

    private final class NoteObserver extends ContentObserver {
        private NoteObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            fetchNotes();
        }
    }
}
