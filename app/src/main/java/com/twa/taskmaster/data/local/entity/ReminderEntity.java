package com.twa.taskmaster.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.annotation.NonNull;

@Entity(
        tableName = "reminders",
        foreignKeys = @ForeignKey(
                entity = TaskEntity.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@androidx.room.Index(value = "taskId")}
)
public class ReminderEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private long taskId;

    private long reminderTime;  // Epoch milliseconds
    private boolean isSynced;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
}
