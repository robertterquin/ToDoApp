package com.example.todolist;

public class Item {
    private String id;  // Firestore document ID
    private String title;
    private String description;
    private boolean isChecked;
    private boolean isFavorite;
    private boolean isPriority; // New field for priority

    // Required empty constructor for Firestore
    public Item() {
    }

    public Item(String title, String description, boolean isChecked, boolean isFavorite) {
        this.title = title;
        this.description = description;
        this.isChecked = isChecked;
        this.isFavorite = isFavorite;
        this.isPriority = isFavorite; // Priority is true if favorite
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public int getPriority() {
        return isPriority ? 1 : 0; // Ensure priority returns an integer
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }

    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
    }

    public void setPriority(int priority) {
        this.isPriority = (priority == 1);  // Convert int to boolean
    }
}
