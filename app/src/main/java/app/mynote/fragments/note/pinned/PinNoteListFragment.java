package app.mynote.fragments.note.pinned;

import android.database.ContentObserver;
import android.graphics.Color;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import app.mynote.core.db.NoteContract;
import app.mynote.core.db.NoteSyncAdapter;
import app.mynote.core.utils.AppDate;
import app.mynote.core.utils.AppTimestamp;
import app.mynote.core.utils.GsonParser;
import app.mynote.fragments.SwipeController;
import app.mynote.fragments.note.Note;
import app.mynote.fragments.note.NoteService;
import mynote.R;
import mynote.databinding.FragmentPinBinding;

public class PinNoteListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, PinNotesAdapter.OnPinNoteItemClickListener {

    public RecyclerView recyclerView;
    public PinNotesAdapter pinAdapter;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    private FragmentPinBinding binding;
    private FloatingActionButton fab;
    private NoteObserver noteObserver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteObserver = new NoteObserver();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentPinBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        recyclerView = view.findViewById(R.id.noterecyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ArrayList<Note> notes = new ArrayList<>(NoteService.getAllNotes(getContext()));
        this.pinAdapter = new PinNotesAdapter(getContext(), notes, this);
        recyclerView.setAdapter(pinAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        this.pinAdapter.registerAdapterDataObserver(new AdapterDataObserver() {
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


        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.primary_light,
                R.color.primary_light,
                R.color.primary_light,
                R.color.primary_light);

        this.fab = view.findViewById(R.id.fab);
        this.fab.setVisibility(View.INVISIBLE);

        SwipeController swipeController = new SwipeController(getContext(), recyclerView) {
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons) {
                underlayButtons.add(new UnderlayButton(
                        "Un Pin",
                        SwipeController.getBitmapFromVectorDrawable(getContext(), R.drawable.ic_pin_white),
                        ContextCompat.getColor(getContext(),R.color.primary),
                        new UnderlayButtonClickListener() {
                            @Override
                            public void onClick(int position) {
                                Note noteItem = pinAdapter.notesList.get(position);
                                noteItem.setPinned(false);
                                noteItem.setPinOrder(System.currentTimeMillis());
                                NoteService.update(getContext(), noteItem, false);
                                Toast.makeText(getContext(), "Updated", Toast.LENGTH_LONG).show();
                                pinAdapter.notesList.remove(position);
                                pinAdapter.notifyItemRemoved(position);
                            }
                        }
                ));

            }
        };
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        NoteSyncAdapter.performSync();
        mSwipeRefreshLayout.setRefreshing(false);
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

    private void fetchNotes() {
        ArrayList<Note> notes = new ArrayList<>(NoteService.getPinned(getContext()));
        PinNoteListFragment.this.dataView(notes);
        setAppbarCount();
        recyclerView.getAdapter().notifyDataSetChanged();
    }


    private void dataView(List<Note> notes) {
        this.pinAdapter = new PinNotesAdapter(getContext(), notes, this);
        recyclerView.setAdapter(this.pinAdapter);
    }

    private void setAppbarCount() {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Pinned " + "(" + PinNoteListFragment.this.recyclerView.getAdapter().getItemCount() + ")");
    }

    @Override
    public void onPinNoteItemClick(View view, Note note) {
        Bundle bundle = new Bundle();
        String noteJsonString = GsonParser.getGsonParser().toJson(note);
        bundle.putString("note", noteJsonString);

        NavController navController = Navigation.findNavController(view);
        navController.navigate(R.id.action_nav_pin_to_nav_pin_edit, bundle);
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
