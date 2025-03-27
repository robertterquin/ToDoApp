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

        // ✅ Checkbox updates Firestore and triggers sorting
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentItem.setChecked(isChecked);
            applyStrikeThrough(tvTitle, tvDescription, isChecked);
            updateTaskInFirestore(currentItem);
        });

        // Set favorite icon
        favButton.setImageResource(currentItem.isFavorite() ? R.drawable.star : R.drawable.star_outline);

        // Handle favorite button click
        favButton.setOnClickListener(v -> {
            boolean isFavorite = !currentItem.isFavorite();
            currentItem.setFavorite(isFavorite);
            currentItem.setPriority(isFavorite ? 1 : 0);
            updateTaskInFirestore(currentItem);
        });

        return convertView;
    }



    // ✅ Add a new task (priority items will be placed correctly after sorting)

    // ✅ Updates Firestore (does not save position anymore)
    private void updateTaskInFirestore(Item item) {
        if (item.getId() == null || item.getId().isEmpty()) return;

        // Preserve order by keeping timestamps
        Map<String, Object> updates = new HashMap<>();
        updates.put("checked", item.isChecked());
        updates.put("favorite", item.isFavorite());
        updates.put("priority", item.getPriority());
        updates.put("title", item.getTitle());
        updates.put("description", item.getDescription());

        // 🔥 If task is completed, keep timestamp but push to bottom
        if (item.isChecked()) {
            updates.put("timestamp", System.currentTimeMillis() + 9999999);
        } else {
            updates.put("timestamp", System.currentTimeMillis());
        }

        // 🔥 Update Firestore and refresh the sorted list
        db.collection("tasks").document(item.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Task updated successfully");
                    fetchTasksFromFirestore();  // 🔥 Re-fetch sorted data
                    sortTasks();  // 🔥 Ensure tasks move dynamically in UI
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating task", e));
    }

    private void fetchTasksFromFirestore() {
        db.collection("tasks")
                .orderBy("priority", Query.Direction.DESCENDING)  // 🔥 Priority tasks on top
                .orderBy("checked", Query.Direction.ASCENDING)   // 🔥 Completed tasks at the bottom
                .orderBy("timestamp", Query.Direction.ASCENDING) // 🔥 Keep order persistent
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Item task = doc.toObject(Item.class);
                        taskList.add(task);
                    }
                    notifyDataSetChanged(); // ✅ Refresh UI instantly
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching tasks", e));
    }

    // ✅ Sorts tasks based on priority & completion status
    // ✅ Ensures sorted order (priority → normal → completed)
    // ✅ Ensures sorted order (priority → normal → completed)
    private void sortTasks() {
        ArrayList<Item> priorityTasks = new ArrayList<>();
        ArrayList<Item> normalTasks = new ArrayList<>();
        ArrayList<Item> completedTasks = new ArrayList<>();

        for (Item task : taskList) {
            if (task.isChecked()) {
                completedTasks.add(task);  // ✅ Move completed tasks to the bottom
            } else if (task.getPriority() == 1) {
                priorityTasks.add(task);   // ✅ Move priority tasks to the top
            } else {
                normalTasks.add(task);     // ✅ Keep normal tasks in the middle
            }
        }

        // 🔥 Update task list dynamically
        taskList.clear();
        taskList.addAll(priorityTasks);
        taskList.addAll(normalTasks);
        taskList.addAll(completedTasks);

        notifyDataSetChanged(); // ✅ Refresh UI immediately
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