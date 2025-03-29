package com.example.todolist;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.animation.ObjectAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.*;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Item> todoList;
    private ListView lvTasks;
    private CustomAdapter myAdapter;
    private ImageView imgAddTask;
    private FirebaseFirestore db;
    private CollectionReference tasksRef;
    private GestureDetectorCompat gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        tasksRef = db.collection("tasks");

        // Initialize UI components
        lvTasks = findViewById(R.id.lvTasks);
        imgAddTask = findViewById(R.id.add_btn);
        todoList = new ArrayList<>();
        myAdapter = new CustomAdapter(MainActivity.this, todoList);
        lvTasks.setAdapter(myAdapter);

        // Load tasks from Firestore
        loadTasksFromFirestore();

        // Gesture detector for swipe actions
        gestureDetector = new GestureDetectorCompat(this, new GestureListener());
        lvTasks.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        // Click listener for add button
        imgAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        EditText etTitle = dialogView.findViewById(R.id.etTitle);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnAdd.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (!title.isEmpty() && !description.isEmpty()) {
                Item newTask = new Item(title, description, false, false);

                tasksRef.add(newTask).addOnSuccessListener(documentReference -> {
                    newTask.setId(documentReference.getId());
                    tasksRef.document(newTask.getId()).set(newTask); // Save with Firestore ID

                    todoList.add(newTask);
                    myAdapter.notifyDataSetChanged();
                    dialog.dismiss();
                    Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add task", Toast.LENGTH_SHORT).show()
                );
            } else {
                Toast.makeText(this, "Please enter both title and description", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTasksFromFirestore() {
        tasksRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error loading tasks", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                todoList.clear();
                for (QueryDocumentSnapshot document : value) {
                    Item item = document.toObject(Item.class);
                    item.setId(document.getId());
                    todoList.add(item);
                }
                myAdapter.notifyDataSetChanged();
            }
        });
    }

    // Gesture Listener for Swipe Action
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;

            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX < 0) { // Left Swipe Detected
                    int position = lvTasks.pointToPosition((int) e1.getX(), (int) e1.getY());

                    if (position != ListView.INVALID_POSITION && position < todoList.size()) {
                        animateAndDeleteTask(position);
                    }
                    return true;
                }
            }
            return false;
        }
    }

    // Animate and delete task
    private void animateAndDeleteTask(int position) {
        if (position < 0 || position >= todoList.size()) return;

        View taskView = lvTasks.getChildAt(position - lvTasks.getFirstVisiblePosition());
        if (taskView != null) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(taskView, "translationX", 0, -taskView.getWidth());
            animator.setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    deleteTask(position);
                }
            });
            animator.start();
        } else {
            deleteTask(position);
        }
    }

    // Delete task from Firestore and update UI
    private void deleteTask(int position) {
        if (position < 0 || position >= todoList.size()) return;

        Item itemToDelete = todoList.get(position);
        String taskId = itemToDelete.getId();

        // Remove from Firestore
        tasksRef.document(taskId).delete()
                .addOnSuccessListener(aVoid -> {
                    if (position < todoList.size()) {
                        todoList.remove(position);
                        myAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Failed to delete task", Toast.LENGTH_SHORT).show()
                );
    }
}
