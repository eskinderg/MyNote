package com.example.drawer.ui.notes.pinned;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.example.drawer.core.utils.Time2Ago;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.example.drawer.R;
import com.example.drawer.ui.notes.Note;

public class PinNotesAdapter extends  RecyclerView.Adapter<PinNotesAdapter.PinNoteRecyclerViewHolder> {

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
        return new PinNoteRecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PinNoteRecyclerViewHolder holder, int position) {

        Note noteItem = notesList.get(position);

        if (noteItem.getHeader() == null || noteItem.getHeader().isEmpty()) {
            holder.header.setText("");
        } else {
            holder.header.setText(noteItem.getHeader());
        }

        holder.description.setText("Modified " + Time2Ago.covertTimeToText(noteItem.getDateModified()));

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
        List<Note> pinnedNotes = list.stream().filter(n -> n.getPinned()).collect(Collectors.toList());
        return new ArrayList<Note>(pinnedNotes);
    }

    public interface OnPinNoteItemClickListener {
        void onPinNoteItemClick(View view, Note note);
    }

    public static class PinNoteRecyclerViewHolder extends RecyclerView.ViewHolder {

        TextView header;
        TextView description;
        CardView card;

        public PinNoteRecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
            description = itemView.findViewById(R.id.description);
            card = itemView.findViewById(R.id.card);
        }
    }
}
