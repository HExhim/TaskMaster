package com.twa.taskmaster.data.local.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

public class TaskWithReminders {

    @Embedded
    public TaskEntity task;

    @Relation(
            parentColumn = "id",
            entityColumn = "taskId"
    )
    public List<ReminderEntity> reminders;
}
