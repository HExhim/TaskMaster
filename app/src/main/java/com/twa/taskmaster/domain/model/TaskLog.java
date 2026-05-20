package com.twa.taskmaster.domain.model;

public class TaskLog {
    private int id;
    private long timestamp;
    private long duration;
    private String source;

    private int taskId;

    public TaskLog(int id, long timestamp, long duration, String source, int taskId) {
        this.id = id;
        this.timestamp = timestamp;
        this.duration = duration;
        this.source = source;
        this.taskId = taskId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }
}
