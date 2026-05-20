package com.twa.taskmaster.core.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.twa.taskmaster.domain.model.Reminder;
import com.twa.taskmaster.domain.model.Task;

public class ReminderScheduler {

    public static void scheduleReminders(Context context, Task task) {
        if (task.getReminders() == null || task.getReminders().isEmpty()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (Reminder reminder : task.getReminders()) {
            if (reminder.getReminderTime() < System.currentTimeMillis()) {
                continue; // Skip past reminders
            }

            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("task_data", task);
            intent.putExtra("reminder_id", reminder.getId());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (int) reminder.getId(), // Unique request code for each reminder
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(reminder.getReminderTime(), pendingIntent);
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
        }
    }

    public static void cancelReminder(Context context, Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) reminder.getId(),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static void cancelReminders(Context context, Task task) {
        if (task.getReminders() == null || task.getReminders().isEmpty()) {
            return;
        }

        for (Reminder reminder : task.getReminders()) {
            cancelReminder(context, reminder);
        }
    }
}
