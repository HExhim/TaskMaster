package com.twa.taskmaster.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.twa.taskmaster.data.local.entity.ReminderEntity;

import java.util.List;

@Dao
public interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ReminderEntity reminder);

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    List<ReminderEntity> getRemindersForTask(long taskId);

    @Update
    void update(ReminderEntity entity);

    @Delete
    void delete(ReminderEntity entity);

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    void deleteById(long reminderId);

    @Query("DELETE FROM reminders")
    void deleteAllReminders();
    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    void deleteRemindersByTaskId(long taskId);

    @Query("SELECT * FROM reminders WHERE isSynced = 0")
    List<ReminderEntity> getUnsyncedReminders();

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    ReminderEntity getReminderById(long reminderId);

}
