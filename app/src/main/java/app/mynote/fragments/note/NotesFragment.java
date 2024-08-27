package app.mynote.fragments.note;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import app.mynote.core.db.NoteContract;
import app.mynote.core.db.NoteSyncAdapter;
import app.mynote.core.utils.GsonParser;
import app.mynote.fragments.SwipeController;
import app.mynote.fragments.note.NotesAdapter.OnNoteItemClickListener;
import mynote.R;
import mynote.databinding.FragmentNotesBinding;

public class NotesFragment extends Fragment implements OnNoteItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    public RecyclerView recyclerView;
    public FloatingActionButton fab;
    public NotesAdapter notesAdapter;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    private FragmentNotesBinding binding;
    private NoteObserver noteObserver;

    public NotesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteObserver = new NoteObserver();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        recyclerView = view.findViewById(R.id.noterecyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

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
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                                Date date = new Date();
                                noteItem.setPinOrder(dateFormat.format(date));
                                NoteService noteService = new NoteService(getContext());
                                noteService.update(noteItem, false);
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
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                                Date date = new Date();
                                noteItem.setDateArchived(dateFormat.format(date));
                                NoteService noteService = new NoteService(getContext());
                                noteService.update(noteItem, false);
                                String textMsg = "archived";
                                Toast.makeText(getContext(), "Note " + textMsg, Toast.LENGTH_LONG).show();
                                notesAdapter.notesList.remove(position);
                                notesAdapter.notifyItemRemoved(position);
                            }
                        }
                ));

            }
        };

        this.fab = view.findViewById(R.id.fab);

        this.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NoteService noteService = new NoteService(getContext());
                Note note = new Note();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                Date date = new Date();
                note.setId(UUID.randomUUID().toString());
                note.setDateModified(dateFormat.format(date));
                noteService.add(note);

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

        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                fetchNotes();
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
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

//        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
//        fragmentManager.beginTransaction()
//                .add(R.id.nav_host_fragment_content_main, NoteFragment.class, bundle)
//                .addToBackStack("notes")
//                .commit();

//        FragmentTransaction fragmentTransaction =fragmentManager.beginTransaction();
//        fragmentTransaction.replace(R.id.nav_host_fragment_content_main,frag);
//        fragmentTransaction.commit();

        //Put the value
//        NoteFragment ldf = new NoteFragment();
//        Bundle args = new Bundle();
//        args.putString("YourKey", "YourValue");
//        ldf.setArguments(args);
//
//        getFragmentManager().beginTransaction().add(R.id.nav_host_fragment_content_main, ldf).commit();
    }

    @Override
    public void onRefresh() {
        NoteSyncAdapter.cancelSync();
        NoteSyncAdapter.performSync();
        recyclerView.setVisibility(View.INVISIBLE);
        mSwipeRefreshLayout.setRefreshing(true);
        fetchNotes();
    }

    private void fetchNotes() {
        NoteService noteService = new NoteService(getContext());
        ArrayList<Note> notes = new ArrayList<>(noteService.getAllNotes());
        NotesFragment.this.dataView(notes);
        setAppbarCount();
        mSwipeRefreshLayout.setRefreshing(false);
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.setVisibility(View.VISIBLE);
    }


    private void dataView(List<Note> notes) {
        this.notesAdapter = new NotesAdapter(getContext(), notes, this);
        recyclerView.setAdapter(this.notesAdapter);
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
    }

    private void setAppbarCount() {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Notes " + "(" + NotesFragment.this.recyclerView.getAdapter().getItemCount() + ")");
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
