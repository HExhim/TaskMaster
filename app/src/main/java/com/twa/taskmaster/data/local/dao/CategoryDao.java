package com.twa.taskmaster.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.twa.taskmaster.data.local.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategoryEntity category);

    @Query("SELECT name FROM categories")
    static List<String> getAllCategoriesName() {
        return null;
    }

    @Query("SELECT * FROM categories")
    List<CategoryEntity> getAllCategories();
    @Delete
    void delete(CategoryEntity category);
}

