package com.twa.taskmaster.domain.model;

public class CompletionStats {
    public int total;
    public int completed;
    public int pending;
    public double percentage;  // percentage (0-100)

    public int getTotal() { return total; }
    public int getCompleted() { return completed; }
    public int getPending() { return pending; }
    public double getPercentage() { return percentage; }
}
