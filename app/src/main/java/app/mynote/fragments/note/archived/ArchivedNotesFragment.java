package app.mynote.fragments.note.archived;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import app.mynote.core.db.NoteContract;
import app.mynote.core.db.NoteSyncAdapter;
import app.mynote.core.utils.AppDate;
import app.mynote.core.utils.AppTimestamp;
import app.mynote.fragments.SwipeController;
import app.mynote.fragments.note.Note;
import app.mynote.fragments.note.NoteService;
import app.mynote.fragments.note.NotesFragment;
import mynote.R;
import mynote.databinding.FragmentArchivedNotesBinding;

public class ArchivedNotesFragment extends Fragment implements ArchivedNotesAdapter.OnNoteItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    public RecyclerView recyclerView;
    public ArchivedNotesAdapter notesAdapter;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    private FragmentArchivedNotesBinding binding;
    private NoteObserver noteObserver;

    public ArchivedNotesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteObserver = new NoteObserver();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentArchivedNotesBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        recyclerView = view.findViewById(R.id.archivednoterecyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        ArrayList<Note> notes = new ArrayList<>(NoteService.getArchived(getContext()));
        this.notesAdapter = new ArchivedNotesAdapter(getContext(), notes, this);
        this.recyclerView.setAdapter(notesAdapter);
        this.notesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        SwipeController swipeController = new SwipeController(getContext(), recyclerView) {
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons) {
                underlayButtons.add(new UnderlayButton(
                        "Delete",
                        SwipeController.getBitmapFromVectorDrawable(getContext(), R.drawable.ic_delete),
                        ContextCompat.getColor(getContext(), R.color.red),
                        new UnderlayButtonClickListener() {
                            @Override
                            public void onClick(int position) {
                                Note noteItem = notesAdapter.notesList.get(position);
                                noteItem.setActive(false);
                                noteItem.setDateDeleted(AppTimestamp.convertStringToTimestamp(AppDate.Now()));
                                NoteService.update(getContext(), noteItem, true);
                                Toast.makeText(getContext(), "Note deleted permanently", Toast.LENGTH_LONG).show();
                                notesAdapter.notesList.remove(position);
                                notesAdapter.notifyItemRemoved(position);
                            }
                        }
                ));

                underlayButtons.add(new UnderlayButton(
                        "Restore",
                        SwipeController.getBitmapFromVectorDrawable(getContext(), R.drawable.ic_restore_white),
                        ContextCompat.getColor(getContext(), R.color.green),
                        new UnderlayButtonClickListener() {
                            @Override
                            public void onClick(int position) {
                                Note noteItem = notesAdapter.notesList.get(position);
                                noteItem.setArchived(false);
                                noteItem.setDateArchived(AppTimestamp.convertStringToTimestamp(AppDate.Now()));
                                NoteService.update(getContext(), noteItem, false);
                                String textMsg = "restored";
                                Toast.makeText(getContext(), "Note " + textMsg, Toast.LENGTH_LONG).show();
                                notesAdapter.notesList.remove(position);
                                notesAdapter.notifyItemRemoved(position);
                            }
                        }
                ));

            }
        };

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.primary_light,
                R.color.primary_light,
                R.color.primary_light,
                R.color.primary_light);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onNoteItemClick(View view, Note note) {
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
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        NoteSyncAdapter.performSync();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void fetchNotes() {
        ArrayList<Note> notes = new ArrayList<>(NoteService.getArchived(getContext()));
        ArchivedNotesFragment.this.dataView(notes);
        setAppbarCount();
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private void dataView(List<Note> notes) {
//        List<Note> list = notes.stream().filter(n -> !n.isArchived()).toList();
        this.notesAdapter = new ArchivedNotesAdapter(getContext(), notes, this);
        recyclerView.setAdapter(this.notesAdapter);
    }

    private void setAppbarCount() {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Archived " + "(" + ArchivedNotesFragment.this.notesAdapter.notesList.size() + ") ");
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
