package com.twa.taskmaster.domain.model;

public class LogFilter {
    public String dateRange;
    public boolean showCompleted;
    public boolean showIncomplete;

    public LogFilter() {
        this("ALL", true, true); // default: all logs
    }

    public LogFilter(String range, boolean completed, boolean incomplete) {
        this.dateRange = range;
        this.showCompleted = completed;
        this.showIncomplete = incomplete;
    }
}

