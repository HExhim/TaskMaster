package com.twa.taskmaster.core.reminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.twa.taskmaster.R;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.ui.task.details.TaskDetailActivity;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "task_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ReminderReceiver", "onReceive called with intent: " + intent);
        Task task = (Task) intent.getSerializableExtra("task_data");
        long reminderId = intent.getLongExtra("reminder_id", -1L);

        if (task == null) {
            Log.e("ReminderReceiver", "Task not found in intent");
            return;
        }

        long taskId = task.getId();
        String title = task.getTitle();
        String desc = task.getDescription();

        createChannel(context);

        Intent fullScreenIntent = new Intent(context, TaskDetailActivity.class);
        fullScreenIntent.putExtra("task_id", taskId);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                (int) reminderId,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Reminder: " + title)
                .setContentText(desc != null && !desc.isEmpty() ? desc : "Task is due soon")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify((int) reminderId, builder.build());
    }

    private void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for upcoming task deadlines");
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            channel.setSound(alarmSound, null);
            manager.createNotificationChannel(channel);
        }
    }
}
