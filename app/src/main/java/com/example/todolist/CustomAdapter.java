package com.example.todolist;

import android.app.Activity;
import android.graphics.Paint;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class CustomAdapter extends ArrayAdapter<Item> {

    private Activity context;
    private ArrayList<Item> taskList;
    private FirebaseFirestore db;

    public CustomAdapter(Activity context, ArrayList<Item> taskList) {
        super(context, R.layout.custom_item, taskList);
        this.context = context;
        this.taskList = taskList;
        this.db = FirebaseFirestore.getInstance(); // Initialize Firestore
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

        // Ensure Firestore ID is set
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

        // Handle checkbox change (no sorting)
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentItem.setChecked(isChecked);
            applyStrikeThrough(tvTitle, tvDescription, isChecked);
            updateTaskInFirestore(currentItem, false); // Do not sort when just checking/unchecking
        });

        // Set favorite icon
        favButton.setImageResource(currentItem.isFavorite() ? R.drawable.star : R.drawable.star_outline);

        // Handle favorite button click
        favButton.setOnClickListener(v -> {
            boolean isFavorite = !currentItem.isFavorite();
            currentItem.setFavorite(isFavorite);
            currentItem.setPriority(isFavorite ? 1 : 0); // Update priority

            // Update Firestore and refresh list (sorting only when favoriting)
            updateTaskInFirestore(currentItem, true);
        });

        return convertView;
    }

    // Save task updates to Firestore
    private void updateTaskInFirestore(Item item, boolean shouldSort) {
        if (item.getId() == null || item.getId().isEmpty()) {
            Log.e("CustomAdapter", "Error: Cannot update Firestore, Task ID is missing.");
            return;
        }

        db.collection("tasks").document(item.getId())
                .update("checked", item.isChecked(),
                        "favorite", item.isFavorite(),
                        "priority", item.getPriority()) // Store priority
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Task updated successfully");

                    // Only sort if explicitly required (e.g., when favoriting)
                    if (shouldSort) {
                        sortPriorityTasks();
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating task", e));
    }
    // Sort priority tasks while keeping others in place
    private void sortPriorityTasks() {
        ArrayList<Item> priorityTasks = new ArrayList<>();
        ArrayList<Item> normalTasks = new ArrayList<>();

        // Separate priority and normal tasks
        for (Item task : taskList) {
            if (task.getPriority() == 1) {
                priorityTasks.add(task);
            } else {
                normalTasks.add(task);
            }
        }

        // Maintain order: priority tasks first, then normal tasks
        taskList.clear();
        taskList.addAll(priorityTasks);
        taskList.addAll(normalTasks);

        notifyDataSetChanged(); // Refresh UI
    }

    // Add a new task at the bottom without affecting priority tasks
    public void addTask(Item newTask) {
        taskList.add(newTask); // Add new task at the end
        notifyDataSetChanged(); // Refresh UI without sorting
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
