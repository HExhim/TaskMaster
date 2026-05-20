package com.twa.taskmaster.data.local.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "deleted_items")
public class DeletedItemEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String itemType; // "TASK", "REMINDER", "LOG"
    private String itemId;   // Original ID (usually String for Firebase compatibility)
    private long deletedAt;
    private String data;     // JSON representation of the deleted object for restoration

    public DeletedItemEntity(String itemType, String itemId, long deletedAt, String data) {
        this.itemType = itemType;
        this.itemId = itemId;
        this.deletedAt = deletedAt;
        this.data = data;
    }
    
    // Helper constructor for existing code that might call it without data (though we should update those calls)
    @Ignore
    public DeletedItemEntity(String itemType, String itemId, long deletedAt) {
        this(itemType, itemId, deletedAt, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(long deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    public String getData() {
        return data;
    }
    
    public void setData(String data) {
        this.data = data;
    }
}
