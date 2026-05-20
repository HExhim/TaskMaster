package com.twa.taskmaster.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.twa.taskmaster.data.local.entity.DeletedItemEntity;

import java.util.List;

@Dao
public interface DeletedItemDao {
    @Insert
    void insert(DeletedItemEntity deletedItem);

    @Query("SELECT * FROM deleted_items")
    List<DeletedItemEntity> getAllDeletedItems();
    
    @Query("SELECT * FROM deleted_items")
    LiveData<List<DeletedItemEntity>> getAllDeletedItemsLive();

    @Query("DELETE FROM deleted_items WHERE id = :id")
    void delete(int id);
    
    @Query("DELETE FROM deleted_items WHERE id IN (:ids)")
    void deleteByIds(List<Integer> ids);

    @Query("DELETE FROM deleted_items WHERE deletedAt < :threshold")
    void deleteOlderThan(long threshold);
}
