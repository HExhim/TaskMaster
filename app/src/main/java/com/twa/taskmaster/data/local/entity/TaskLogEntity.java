package com.twa.taskmaster.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_logs")
public class TaskLogEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "taskId")
    private int taskId;
    private String taskName;

    @ColumnInfo(name = "timestamp")
    private long timestamp; // in millis (created time)

    @ColumnInfo(name = "endTimeMillis")
    private long endTimeMillis; // in millis (completed time)

    @ColumnInfo(name = "durationMinutes")
    private int durationMinutes; // Time spent

    @ColumnInfo(name = "note")
    private String note; // Optional annotation or user comment

    @ColumnInfo(name = "source")
    private String source; // Work, Personal, etc.

    @ColumnInfo(name = "isSynced")
    private boolean isSynced;

    public TaskLogEntity() {}

    public TaskLogEntity(int id, int taskId, String taskName, long timestamp, long endTimeMillis, int durationMinutes, String note, String source, boolean isCompleted, boolean isSynced) {
        this.id = id;
        this.taskId = taskId;
        this.taskName = taskName;
        this.timestamp = timestamp;
        this.endTimeMillis = endTimeMillis;
        this.durationMinutes = durationMinutes;
        this.note = note;
        this.source = source;
        this.isSynced = isSynced;
    }

    // --- Getters and Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getTaskName() {
        return taskName;
    }
    public void setTaskName(String name){
        this.taskName = name;
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getEndTimeMillis() { return endTimeMillis; }
    public void setEndTimeMillis(long endTimeMillis) { this.endTimeMillis = endTimeMillis; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }



    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    // --- Utility Methods ---

    public java.util.Date getCreatedAt() {
        return new java.util.Date(timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TaskLogEntity that = (TaskLogEntity) obj;
        return id == that.id &&
                taskId == that.taskId &&
                timestamp == that.timestamp &&
                endTimeMillis == that.endTimeMillis &&
                durationMinutes == that.durationMinutes &&
                java.util.Objects.equals(note, that.note) &&
                java.util.Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, taskId, timestamp, endTimeMillis, durationMinutes, note, source);
    }
}
