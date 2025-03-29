package com.example.todolist;

public class notes_Item {
    private String title;
    private String description;
    private String date;
    private boolean isFavorite;

    // Constructor
    public notes_Item(String title, String description, String date, boolean isFavorite) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.isFavorite = isFavorite;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }
}
