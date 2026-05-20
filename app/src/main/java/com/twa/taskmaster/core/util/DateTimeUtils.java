package com.twa.taskmaster.core.util;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SuppressLint("ConstantLocale")
public class DateTimeUtils {

    // Formatters
    private static final SimpleDateFormat DISPLAY_FORMATTER =
            new SimpleDateFormat("hh:mm a, d MMM yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIMER_FORMATTER =
            new SimpleDateFormat("mm:ss", Locale.getDefault());

    static {
        TIMER_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Conversion Methods
    public static LocalDate toLocalDate(long timestamp) {
        if (timestamp == 0) return null;
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    public static LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }


    public static long toTimestamp(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static String formatDateTime(long timestamp) {
        return DISPLAY_FORMATTER.format(new Date(timestamp));
    }

    public static String formatDate(long timestamp) {
        return DATE_FORMATTER.format(new Date(timestamp));
    }

    public static long parseToTimestamp(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) return 0;
        try {
            Date date = DISPLAY_FORMATTER.parse(dateTime);
            return (date != null) ? date.getTime() : 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }


    /**
     * Format timestamp (millis) to string for EditText fields.
     */

    public static String formatDuration(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %02dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dm %02ds", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%ds", seconds);
        }
    }

    public static String formatTimer(long milliseconds) {
        return TIMER_FORMATTER.format(new Date(milliseconds));
    }

    public static long convertToTimestamp(String dateTime) throws ParseException {
        Date date = DISPLAY_FORMATTER.parse(dateTime);
        return date != null ? date.getTime() : System.currentTimeMillis();
    }
    public static int getHour(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    public static int daysBetween(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date d1 = sdf.parse(start);
            Date d2 = sdf.parse(end);
            long diff = d2.getTime() - d1.getTime();
            return (int) TimeUnit.MILLISECONDS.toDays(diff);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static long getStartOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
