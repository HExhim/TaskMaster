package com.twa.taskmaster.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "tasks")
public class TaskEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String category;
    private String title;
    private String description;
    private String priority;
    private long deadline;
    private long startDateTime;
    private long reminderDateTime;
    private String status;
    private long createdAt;
    private long updatedAt;
    private long timeSpent;
    private boolean isCompleted;
    private int total;
    private int completed;
    private String executionType;

    @ColumnInfo(name = "progressPercentage")
    private int progressPercentage;

    @ColumnInfo(name = "isSynced")
    private boolean isSynced;

    @ColumnInfo(name = "isOverdue")
    private boolean isOverdue;

    private String recurrenceRule;   // e.g. DAILY, WEEKLY, MONTHLY
    private int recurrenceCount;     // ends after X occurrences
    private long recurrenceEndDate;  // millis timestamp
    
    // Stores comma separated days like "2,3,4" (Mon, Tue, Wed)
    private String recurrenceDays; 

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public long getDeadline() {
        return deadline;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public long getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(long startDateTime) {
        this.startDateTime = startDateTime;
    }

    public long getReminderDateTime() { return reminderDateTime; }
    public void setReminderDateTime(Long reminderDateTime) { this.reminderDateTime = reminderDateTime; }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        this.isSynced = synced;
    }


    public void setTimeSpent(long millis) {
        this.timeSpent += millis;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public void setTotal(int total) { this.total = total; }
    public void setCompleted(int completed) { this.completed = completed; }

    public int getTotal() { return total; }
    public int getCompleted() { return completed; }

    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
    }

    public String getRecurrenceRule() {
        return recurrenceRule;
    }

    public void setRecurrenceRule(String recurrenceRule) {
        this.recurrenceRule = recurrenceRule;
    }

    public int getRecurrenceCount() {
        return recurrenceCount;
    }

    public void setRecurrenceCount(int recurrenceCount) {
        this.recurrenceCount = recurrenceCount;
    }

    public long getRecurrenceEndDate() {
        return recurrenceEndDate;
    }

    public void setRecurrenceEndDate(long recurrenceEndDate) {
        this.recurrenceEndDate = recurrenceEndDate;
    }
    
    public String getRecurrenceDays() {
        return recurrenceDays;
    }

    public void setRecurrenceDays(String recurrenceDays) {
        this.recurrenceDays = recurrenceDays;
    }

    public boolean isOverdue() {
        return isOverdue;
    }

    public void setOverdue(boolean overdue) {
        isOverdue = overdue;
    }
}
