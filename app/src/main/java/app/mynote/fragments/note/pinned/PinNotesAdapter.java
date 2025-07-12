package app.mynote.fragments.note.pinned;

import static app.mynote.core.MyNote.getContext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import app.mynote.core.utils.Time2Ago;
import app.mynote.fragments.note.Note;
import mynote.R;

public class PinNotesAdapter extends RecyclerView.Adapter<PinNotesAdapter.PinNoteRecyclerViewHolder> {

    private final OnPinNoteItemClickListener mListener;
    public ArrayList<Note> notesList;
    Context context;

    public PinNotesAdapter(Context context, List<Note> notesList, OnPinNoteItemClickListener listener) {
        this.notesList = getPinnedNotes(notesList);
        this.context = context;
        this.mListener = listener;
    }

    @NonNull
    @Override
    public PinNoteRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        view = layoutInflater.inflate(R.layout.note_list, parent, false);
        PinNoteRecyclerViewHolder holder = new PinNoteRecyclerViewHolder(view);
        return holder;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull PinNoteRecyclerViewHolder holder, int position) {

        Note noteItem = notesList.get(position);

        holder.header.setTypeface(Typeface.DEFAULT_BOLD);

        holder.imgPinned.setVisibility(View.VISIBLE);

        if (noteItem.getHeader() == null || noteItem.getHeader().isEmpty()) {
            holder.header.setText("Untitled");
            holder.header.setTextColor(Color.GRAY);
        } else {
            holder.header.setText(noteItem.getHeader());
        }

        // Mark note as unsynced
        if(!noteItem.getIsSync()) {
            holder.sync.setImageResource(R.drawable.ic_sync);
            holder.sync.setColorFilter(ContextCompat.getColor(getContext(), R.color.primary_light));
        }

        holder.description.setText("Modified " + Time2Ago.covertTimeToText(noteItem.getDateModified().toString()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onPinNoteItemClick(v, noteItem);
            }
        });

    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    private ArrayList<Note> getPinnedNotes(List<Note> list) {
        List<Note> pinnedNotes = list.stream().filter(n -> n.isPinned() && !n.getArchived()).collect(Collectors.toList());
        pinnedNotes.sort(new Comparator<Note>() {
            @Override
            public int compare(Note o1, Note o2) {
                return o1.getPinOrder().compareTo(o2.getPinOrder());
            }
        });
        return new ArrayList<Note>(pinnedNotes);
    }

    public interface OnPinNoteItemClickListener {
        void onPinNoteItemClick(View view, Note note);
    }

    public static class PinNoteRecyclerViewHolder extends RecyclerView.ViewHolder {

        TextView header;
        TextView description;
        CardView card;
        ImageView sync;
        ImageView imgPinned;

        public PinNoteRecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
            description = itemView.findViewById(R.id.description);
            card = itemView.findViewById(R.id.card);
            imgPinned = itemView.findViewById(R.id.imgPinned);
            sync = itemView.findViewById(R.id.sync);
        }
    }
}
