package com.twa.taskmaster.core.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.twa.taskmaster.R;
import com.twa.taskmaster.ui.analytics.DailyReportActivity;

public class NotificationHelper {

    public static final String CHANNEL_ID_DAILY_REPORT = "daily_report_channel";
    public static final String CHANNEL_NAME_DAILY_REPORT = "Daily Report";
    public static final String CHANNEL_DESC_DAILY_REPORT = "Notifications for daily productivity reports";

    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_DAILY_REPORT,
                CHANNEL_NAME_DAILY_REPORT,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(CHANNEL_DESC_DAILY_REPORT);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void showDailyReportNotification(Context context, String title, String message) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, DailyReportActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_DAILY_REPORT)
                .setSmallIcon(R.drawable.ic_notification) // Ensure this drawable exists
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, builder.build());
        }
    }
}