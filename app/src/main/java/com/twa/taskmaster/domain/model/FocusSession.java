package com.twa.taskmaster.domain.model;

import java.io.Serializable;

public class FocusSession implements Serializable {

    private String id;
    private String taskId;
    private long startTime;
    private long endTime;
    private long duration; // in milliseconds

    public FocusSession() {
        // Default constructor
    }

    public FocusSession(String id, String taskId, long startTime, long endTime) {
        this.id = id;
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = endTime - startTime;
    }

    // ----------- Getters and Setters -----------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
        this.duration = endTime - startTime;
    }

    public long getDuration() {
        return duration;
    }
}
