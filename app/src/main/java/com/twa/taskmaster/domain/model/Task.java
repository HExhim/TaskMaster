package com.twa.taskmaster.domain.model;

import android.text.format.DateFormat;

import com.twa.taskmaster.core.enums.TaskExecutionType;
import com.twa.taskmaster.core.util.DateTimeUtils;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Task implements Serializable {
    private int id;
    private String title;
    private String description;
    private String category;
    private String priority;
    private long deadline;
    private long startDateTime;
    private List<Reminder> reminders;
    private long createdAt;
    private long updatedAt;
    private long timeSpent = 0;
    private TaskExecutionType executionType;
   private List<Integer> recurrenceDaysOfWeek;
    private boolean isSynced;
    private boolean isCompleted;
    private long completedAt;


    public List<Reminder> getReminders() {
        return reminders;
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
    }


    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }



    public String getCategory() {
        return category != null ? category : "Uncategorized";
    }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() {
        return title != null ? title : "";
    }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }



    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public long getDeadline() { return deadline; }
    public void setDeadline(long deadline) { this.deadline = deadline; }

    public long getStartDateTime() { return startDateTime; }
    public void setStartDateTime(long startDateTime) { this.startDateTime = startDateTime; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getTimeSpent() { return timeSpent; }
    public void setTimeSpent(long timeSpent) { this.timeSpent = timeSpent; }
    public void addTimeSpent(long millis) { this.timeSpent += millis; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getStatus() {
        if (isCompleted()) {
            return "Complete";
        }

        long now = System.currentTimeMillis();
        if (getDeadline() > 0 && now > getDeadline()) {
            return "Overdue";
        }

        if (getTimeSpent() > 0) {
            return "In Progress";
        }

        return "Pending";
    }

    // Utilities

    public String getFormattedDeadline() {
        return DateTimeUtils.formatDateTime(deadline);
    }

    public String getFormattedTimeSpent() {
        return formatDuration(timeSpent);
    }

    private String formatDuration(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long hours = minutes / 60;
        minutes %= 60;
        return hours + "h " + minutes + "m";
    }

    public TaskExecutionType getExecutionType() {
        return executionType;
    }

    public void setExecutionType(TaskExecutionType executionType) {
        this.executionType = executionType;
    }

    public List<Integer> getRecurrenceDaysOfWeek() {
        return recurrenceDaysOfWeek;
    }

    public void setRecurrenceDaysOfWeek(List<Integer> recurrenceDaysOfWeek) {
        this.recurrenceDaysOfWeek = recurrenceDaysOfWeek;
    }

    public long getCompletedAt() {
        return completedAt;
    }
}
