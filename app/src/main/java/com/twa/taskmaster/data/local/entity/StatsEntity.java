package com.twa.taskmaster.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.twa.taskmaster.domain.model.Stats;

@Entity(tableName = "stats")
public class StatsEntity {
    @PrimaryKey
    private int id = 1; // Singleton row
    private int completedTasks;
    private long totalTimeTracked;
    private int productivityScore;

    // Getters and setters...

    public Stats toModel() {
        return new Stats(completedTasks, totalTimeTracked, productivityScore);
    }
}
