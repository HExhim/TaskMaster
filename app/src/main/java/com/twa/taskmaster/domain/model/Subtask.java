package com.twa.taskmaster.domain.model;

import java.io.Serializable;

public class Subtask implements Serializable {

    private String title;
    private boolean isCompleted;

    public Subtask() {
        // Default constructor for Firestore
    }

    public Subtask(String title) {
        this.title = title;
        this.isCompleted = false;
    }

    // ----------- Getters and Setters -----------

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
