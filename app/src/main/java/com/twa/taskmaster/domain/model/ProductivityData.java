package com.twa.taskmaster.domain.model;

public class ProductivityData {
    private String date;
    private int tasksCompleted;
    private int tasksCreated;
    private int overdueTasks;
    private float completionRate;
    private float overdueRate;
    private float averageCompletionTimeMinutes;
    private int currentStreak;

    // Constructor matching ViewModel usage
    public ProductivityData(String date, int tasksCompleted, int tasksCreated,
                            int overdueTasks, float completionRate) {
        this.date = date;
        this.tasksCompleted = tasksCompleted;
        this.tasksCreated = tasksCreated;
        this.overdueTasks = overdueTasks;
        this.completionRate = completionRate;
        this.overdueRate = tasksCreated > 0 ? (overdueTasks * 100f / tasksCreated) : 0f;
    }

    // Additional constructor for all fields
    public ProductivityData(String date, int tasksCompleted, int tasksCreated,
                            int overdueTasks, float completionRate,
                            float averageCompletionTimeMinutes, int currentStreak) {
        this(date, tasksCompleted, tasksCreated, overdueTasks, completionRate);
        this.averageCompletionTimeMinutes = averageCompletionTimeMinutes;
        this.currentStreak = currentStreak;
    }

    // Getters
    public String getDate() { return date; }
    public int getTasksCompleted() { return tasksCompleted; }
    public int getTasksCreated() { return tasksCreated; }
    public float getCompletionRate() { return completionRate; }
    public int getOverdueTasks() { return overdueTasks; }
    public float getOverdueRate() { return overdueRate; }
    public float getAverageCompletionTimeMinutes() { return averageCompletionTimeMinutes; }
    public int getCurrentStreak() { return currentStreak; }

    // Setters
    public void setDate(String date) { this.date = date; }
    public void setTasksCompleted(int tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
        recalculateRates();
    }

    public void setTasksCreated(int tasksCreated) {
        this.tasksCreated = tasksCreated;
        recalculateRates();
    }

    public void setOverdueTasks(int overdueTasks) {
        this.overdueTasks = overdueTasks;
        recalculateRates();
    }

    public void setAverageCompletionTimeMinutes(float minutes) {
        this.averageCompletionTimeMinutes = minutes;
    }

    public void setCurrentStreak(int streak) {
        this.currentStreak = streak;
    }

    private void recalculateRates() {
        this.completionRate = tasksCreated > 0 ? (tasksCompleted * 100f / tasksCreated) : 0f;
        this.overdueRate = tasksCreated > 0 ? (overdueTasks * 100f / tasksCreated) : 0f;
    }
}