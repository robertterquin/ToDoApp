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

        tvTitle.setText(currentItem.getTitle());
        tvDescription.setText(currentItem.getDescription());

        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(currentItem.isChecked());

        applyStrikeThrough(tvTitle, tvDescription, currentItem.isChecked());

        // ✅ Checkbox updates Firestore
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
            updateTaskInFirestore(currentItem);
        });

        return convertView;
    }

    private void updateTaskInFirestore(Item item) {
        if (item.getId() == null || item.getId().isEmpty()) return;

        // 🔥 Keep timestamp unchanged by only updating checked/favorite fields
        Map<String, Object> updates = new HashMap<>();
        updates.put("checked", item.isChecked());
        updates.put("favorite", item.isFavorite());

        // ✅ Only update necessary fields, keeping timestamp unchanged
        db.collection("tasks").document(item.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Task updated successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating task", e));
    }

    private void fetchTasksFromFirestore() {
        db.collection("tasks")
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
