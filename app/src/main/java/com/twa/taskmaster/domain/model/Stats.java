package com.twa.taskmaster.domain.model;

import androidx.annotation.NonNull;

import java.util.Locale;

public class Stats {
    private final int completedTasks;
    private final long totalTimeTracked; // in milliseconds
    private final int productivityScore; // percentage

    public Stats(int completedTasks, long totalTimeTracked, int productivityScore) {
        this.completedTasks = completedTasks;
        this.totalTimeTracked = totalTimeTracked;
        this.productivityScore = productivityScore;
    }

    // Getters
    public int getCompletedTasks() {
        return completedTasks;
    }

    public long getTotalTimeTracked() {
        return totalTimeTracked;
    }

    public int getProductivityScore() {
        return productivityScore;
    }

    // Formatted values for UI
    public String getFormattedTimeTracked() {
        long totalSeconds = totalTimeTracked / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %02dm", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%dm", minutes);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Stats{" +
                "completedTasks=" + completedTasks +
                ", totalTimeTracked=" + totalTimeTracked +
                ", productivityScore=" + productivityScore +
                '}';
    }
}
