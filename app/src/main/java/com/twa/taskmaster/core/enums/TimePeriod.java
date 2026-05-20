package com.twa.taskmaster.core.enums;

import java.util.Calendar;
import java.util.Date;

public enum TimePeriod {
    ALL(-1),       // Special case for all time
    TODAY(1),      // Last 1 day
    WEEK(7),       // Last 7 days
    MONTH(30),   // Last 30 days (approximate)
    YEAR(365);

    private final int days;

    TimePeriod(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    // Helper method to get date range
    public DateRange getDateRange() {
        if (this == ALL) {
            return new DateRange(null, null); // No date limits
        }

        Calendar cal = Calendar.getInstance();
        Date endDate = cal.getTime();

        cal.add(Calendar.DATE, -days);
        Date startDate = cal.getTime();

        return new DateRange(startDate, endDate);
    }

    public static class DateRange {
        public final Date startDate;
        public final Date endDate;

        public DateRange(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}