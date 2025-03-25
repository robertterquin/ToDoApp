package com.example.todolist;

import android.app.Activity;
import android.graphics.Paint;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.google.firebase.firestore.*;

import java.util.*;

public class CustomAdapter extends ArrayAdapter<Item> {

    private Activity context;
    private ArrayList<Item> taskList;
    private FirebaseFirestore db;

    public CustomAdapter(Activity context, ArrayList<Item> taskList) {
        super(context, R.layout.custom_item, taskList);
        this.context = context;
        this.taskList = taskList;
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            convertView = inflater.inflate(R.layout.custom_item, parent, false);
        }

        // Initialize views
        CheckBox checkBox = convertView.findViewById(R.id.checkBox);
        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvDescription = convertView.findViewById(R.id.tvDescription);
        ImageView favButton = convertView.findViewById(R.id.favButton);

        // Get current item
        Item currentItem = taskList.get(position);

        if (currentItem.getId() == null || currentItem.getId().isEmpty()) {
            Log.e("CustomAdapter", "Error: Task ID is missing!");
            return convertView;
        }

        // Set task data
        tvTitle.setText(currentItem.getTitle());
        tvDescription.setText(currentItem.getDescription());

        // Remove previous listener before updating check state
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(currentItem.isChecked());

        // Apply strikethrough if checked
        applyStrikeThrough(tvTitle, tvDescription, currentItem.isChecked());

        // ✅ Checkbox only updates Firestore, no UI refresh
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentItem.setChecked(isChecked);
            applyStrikeThrough(tvTitle, tvDescription, isChecked);
            updateCheckboxInFirestore(currentItem);
        });

        // Set favorite icon
        favButton.setImageResource(currentItem.isFavorite() ? R.drawable.star : R.drawable.star_outline);

        // Handle favorite button click
        favButton.setOnClickListener(v -> {
            boolean isFavorite = !currentItem.isFavorite();
            currentItem.setFavorite(isFavorite);
            currentItem.setPriority(isFavorite ? 1 : 0);

            updatePriorityInFirestore(currentItem); // 🔥 Refresh only on priority change
        });

        return convertView;
    }

    // ✅ Updates only checkbox in Firestore, no UI refresh
    private void updateCheckboxInFirestore(Item item) {
        if (item.getId() == null || item.getId().isEmpty()) return;

        db.collection("tasks").document(item.getId())
                .update("checked", item.isChecked())
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Task checkbox updated");

                    // 🔥 Ensure priority tasks remain at the top, but don't refresh non-priority tasks
                    if (item.getPriority() == 1) {
                        sortPriorityTasks(); // Keep priority tasks on top
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating checkbox", e));
    }


    // ✅ Refreshes UI only when priority changes
    private void updatePriorityInFirestore(Item item) {
        if (item.getId() == null || item.getId().isEmpty()) return;

        db.collection("tasks").document(item.getId())
                .update("favorite", item.isFavorite(), "priority", item.getPriority())
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Task priority updated");

                    // 🔥 Only sort when a task is newly prioritized or de-prioritized
                    if (item.getPriority() == 1) {
                        sortPriorityTasks();
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating priority", e));
    }

    // ✅ Loads tasks from Firestore without changing their position

    // ✅ Maintain original order, only move priority tasks
    private void sortPriorityTasks() {
        ArrayList<Item> priorityTasks = new ArrayList<>();
        ArrayList<Item> normalTasks = new ArrayList<>();

        for (Item task : taskList) {
            if (task.getPriority() == 1) {
                priorityTasks.add(task);
            } else {
                normalTasks.add(task);
            }
        }

        // 🔥 Only re-sort if priority tasks actually changed
        if (!priorityTasks.isEmpty()) {
            taskList.clear();
            taskList.addAll(priorityTasks);
            taskList.addAll(normalTasks);
            notifyDataSetChanged(); // 🔥 Only refresh when needed
        }
    }

    // Apply or remove strikethrough
    private void applyStrikeThrough(TextView title, TextView description, boolean isChecked) {
        if (isChecked) {
            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            description.setPaintFlags(description.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            description.setPaintFlags(description.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }
}
