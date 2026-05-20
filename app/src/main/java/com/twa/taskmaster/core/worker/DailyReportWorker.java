package com.twa.taskmaster.core.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.twa.taskmaster.core.util.NotificationHelper;

import java.util.Calendar;

public class DailyReportWorker extends Worker {

    public DailyReportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        Context context = getApplicationContext();
        
        String title = "Daily Report Ready 📊";
        String message = "Check out your productivity stats for today! You've been doing great.";
        
        NotificationHelper.showDailyReportNotification(context, title, message);

        return Result.success();
    }
}