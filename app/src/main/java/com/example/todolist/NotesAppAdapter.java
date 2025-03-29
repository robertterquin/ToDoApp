package com.example.todolist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class NotesAppAdapter extends BaseAdapter {
    private Context context;
    private List<notes_Item> notesList;

    // Constructor
    public NotesAppAdapter(Context context, List<notes_Item> notesList) {
        this.context = context;
        this.notesList = notesList;
    }

    @Override
    public int getCount() {
        return notesList.size();
    }

    @Override
    public Object getItem(int position) {
        return notesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.custom_notes_item, parent, false);
        }

        // Get views
        TextView title = convertView.findViewById(R.id.tvNoteTitle);
        TextView description = convertView.findViewById(R.id.tvNoteDescription);
        TextView date = convertView.findViewById(R.id.tvNoteDate);
        ImageView favoriteIcon = convertView.findViewById(R.id.imgFavorite);

        // Get the current note
        notes_Item note = notesList.get(position);

        // Bind data to views
        title.setText(note.getTitle());
        description.setText(note.getDescription());
        date.setText(note.getDate());

        // Set favorite icon
        if (note.isFavorite()) {
            favoriteIcon.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            favoriteIcon.setImageResource(android.R.drawable.btn_star_big_off);
        }

        return convertView;
    }
}
