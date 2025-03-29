package com.example.todolist;

import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class notes extends AppCompatActivity {
    private ListView lvNotes;
    private NotesAppAdapter adapter;
    private List<notes_Item> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        // Initialize ListView
        lvNotes = findViewById(R.id.lvNotes);

        // Sample Data
        notesList = new ArrayList<>();
        notesList.add(new notes_Item("Meeting Notes", "Discuss project updates", "March 29, 2025", true));
        notesList.add(new notes_Item("Grocery List", "Buy milk, eggs, and bread", "March 28, 2025", false));
        notesList.add(new notes_Item("Workout Plan", "Cardio + Strength training", "March 27, 2025", false));

        // Set Adapter
        adapter = new NotesAppAdapter(this, notesList);
        lvNotes.setAdapter(adapter);
    }
}
