package com.twa.taskmaster.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "subtasks",
        foreignKeys = @ForeignKey(
                entity = TaskEntity.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("taskId")}
)
public class SubtaskEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;

    private String description;

    private boolean isCompleted;

    private int taskId;  // Foreign key to TaskEntity

    public SubtaskEntity(String title, String description, boolean isCompleted, int taskId) {
        this.title = title;
        this.description = description;
        this.isCompleted = isCompleted;
        this.taskId = taskId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }
}
