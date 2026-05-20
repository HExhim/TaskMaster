package com.twa.taskmaster.domain.model;

public class DailyProductivity {
    private String day;
    private int tasksCompleted;
    private int tasksCreated;  // Renamed from totalTasks for clarity
    private int overdueTasks;

    public DailyProductivity(String day, int tasksCreated, int tasksCompleted, int overdueTasks) {
        this.day = day;
        this.tasksCreated = Math.max(tasksCreated, 0);
        this.tasksCompleted = Math.max(tasksCompleted, 0);
        this.overdueTasks = Math.max(overdueTasks, 0);
    }

    // Getters
    public String getDay() { return day; }
    public int getTasksCompleted() { return tasksCompleted; }
    public int getTasksCreated() { return tasksCreated; }  // Added this
    public int getOverdueTasks() { return overdueTasks; }

    // Setters
    public void setTasksCompleted(int completed) {
        this.tasksCompleted = Math.max(completed, 0);
    }

    public void setTasksCreated(int created) {  // Renamed from setTotal
        this.tasksCreated = Math.max(created, 0);
    }

    public void setOverdueTasks(int tasksOverdue) {
        this.overdueTasks = Math.max(tasksOverdue, 0);
    }

    // Derived properties
    public float getCompletionRate() {
        return tasksCreated > 0 ? (tasksCompleted / (float) tasksCreated) * 100f : 0f;
    }

    public float getOverdueRate() {
        return tasksCreated > 0 ? (overdueTasks / (float) tasksCreated) * 100f : 0f;
    }

    // Helper method for ViewModel updates
    public void incrementTasksCreated() {
        this.tasksCreated++;
    }

    public void incrementTasksCompleted() {
        this.tasksCompleted++;
    }

    public void incrementOverdueTasks() {
        this.overdueTasks++;
    }
}