package app.mynote.fragments.note;

import static app.mynote.core.MyNote.getContext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import mynote.R;


public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteRecyclerViewHolder> {

    private final OnNoteItemClickListener mListener;
    private final OnNoteItemLongClickListener onNoteItemLongClickListener;
    public ArrayList<Note> notesList;
    Context context;

    public NotesAdapter(Context context, List<Note> notesList, OnNoteItemClickListener listener, OnNoteItemLongClickListener onNoteItemLongClickListener) {
        this.context = context;
        this.notesList = getActiveNotes(notesList);
        this.mListener = listener;
        this.onNoteItemLongClickListener = onNoteItemLongClickListener;
    }

    @NonNull
    @Override
    public NoteRecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        view = layoutInflater.inflate(R.layout.note_list, parent, false);
        NoteRecyclerViewHolder holder = new NoteRecyclerViewHolder(view);
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
    public void onBindViewHolder(@NonNull NoteRecyclerViewHolder holder, int position) {

        Note noteItem = notesList.get(position);

        if (noteItem.getPinned()) {
            holder.header.setTypeface(Typeface.DEFAULT_BOLD);
//            holder.imgPinned.setColorFilter(ContextCompat.getColor(context, R.color.primary), PorterDuff.Mode.MULTIPLY);
            holder.imgPinned.setVisibility(View.VISIBLE);
        }

        if (noteItem.getReadonly()) {
            holder.imgReadonly.setVisibility(View.VISIBLE);
        }

        if (noteItem.getHeader() == null || noteItem.getHeader().isEmpty()) {
            holder.header.setText("Untitled");
            holder.header.setTextColor(Color.GRAY);
        } else {
            holder.header.setText(noteItem.getHeader());
        }

        // Mark note as unsynced
        if(!noteItem.getIsSync()) {
            holder.sync.setImageResource(R.drawable.ic_sync);
//            holder.sync.setColorFilter(ContextCompat.getColor(getContext(), R.color.green));
        }

        holder.description.setText("Modified " + Time2Ago.covertTimeToText(noteItem.getDateModified().toString()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onNoteItemClick(v, noteItem);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onNoteItemLongClickListener.onNoteItemLongClick(v, noteItem);
                return false;
            }
        });

//        holder.itemView.setOnLongClickListener(view -> {
//            holder.itemView.showContextMenu();
//            return true;
//        });

    }

    @Override
    public int getItemCount() {
        return notesList.size();
    }

    private ArrayList<Note> getActiveNotes(List<Note> list) {
        List<Note> activeNotes = list.stream().filter(n -> !n.getArchived()).collect(Collectors.toList());

        activeNotes.sort(new Comparator<Note>() {
            @Override
            public int compare(Note o1, Note o2) {
                return o2.getDateModified().compareTo(o1.getDateModified());
            }
        });

        activeNotes.sort(new Comparator<Note>() {
            @Override
            public int compare(Note o1, Note o2) {
                if(o2.isPinned() || o1.isPinned())
                    return Boolean.compare(o2.isPinned(), o1.isPinned());
                return 0;
            }
        });

        activeNotes.sort(new Comparator<Note>() {
            @Override
            public int compare(Note o1, Note o2) {
                if(o1.isPinned() && o2.isPinned())
                    return o1.getPinOrder().compareTo(o2.getPinOrder());
                return 0;
            }
        });

        return new ArrayList<Note>(activeNotes);
    }

    public interface OnNoteItemClickListener {
        void onNoteItemClick(View view, Note note);
    }

    public interface OnNoteItemLongClickListener {
        void onNoteItemLongClick(View view, Note note);
    }

    public static class NoteRecyclerViewHolder extends RecyclerView.ViewHolder {

        TextView header;
        TextView description;
        ImageView imgPinned;
        ImageView imgReadonly;
        ImageView sync;
        CardView card;

        public NoteRecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.header);
            description = itemView.findViewById(R.id.description);
            card = itemView.findViewById(R.id.card);
            imgPinned = itemView.findViewById(R.id.imgPinned);
            imgReadonly = itemView.findViewById(R.id.imgReadonly);
            sync = itemView.findViewById(R.id.sync);
        }
    }
}