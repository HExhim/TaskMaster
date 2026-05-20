package com.twa.taskmaster.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    @NonNull
    private String color; // HEX color code (e.g., "#FF5722")

    public CategoryEntity(){
        //empty constructor
    }
    public CategoryEntity(String name, String colorHex) {
        this.name = name;
        this.color = colorHex;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    @NonNull
    public String getColor() { return color; }
    public void setColor(@NonNull String color) { this.color = color; }
}

