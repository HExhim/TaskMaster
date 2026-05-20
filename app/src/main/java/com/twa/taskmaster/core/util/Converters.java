package com.twa.taskmaster.core.util;

import androidx.room.TypeConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class Converters {
    @TypeConverter
    public static Long dateToTimestamp(LocalDate date) {
        return date == null ? null : date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    @TypeConverter
    public static String fromReminderList(List<Long> reminders) {
        if (reminders == null || reminders.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Long r : reminders) {
            sb.append(r).append(",");
        }
        return sb.toString();
    }

    @TypeConverter
    public static List<Long> toReminderList(String data) {
        List<Long> reminders = new ArrayList<>();
        if (data == null || data.isEmpty()) return reminders;
        for (String s : data.split(",")) {
            try {
                reminders.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {}
        }
        return reminders;
    }
}
