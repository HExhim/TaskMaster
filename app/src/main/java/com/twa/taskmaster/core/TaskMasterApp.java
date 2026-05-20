package com.twa.taskmaster.core;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.twa.taskmaster.data.repository.ReminderRepository;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.data.sync.SyncManager;
import com.twa.taskmaster.domain.usecases.SyncAllUseCase;

public class TaskMasterApp extends Application {
    private TaskRepository taskRepository;
    private ReminderRepository reminderRepository;
    private TaskLogRepository taskLogRepository;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize repositories first
        taskRepository = new TaskRepository(this);
        reminderRepository = new ReminderRepository(this);
        taskLogRepository = new TaskLogRepository(this);

        // Initialize SyncManager
        SyncAllUseCase syncAllUseCase = new SyncAllUseCase(taskRepository, reminderRepository, taskLogRepository);
        SyncManager.initialize(syncAllUseCase);

    }
}
