package com.twa.taskmaster.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.twa.taskmaster.data.local.entity.TaskEntity;
import com.twa.taskmaster.data.local.entity.TaskWithReminders;
import com.twa.taskmaster.domain.model.CompletionStats;

import java.time.LocalDate;
import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insert(TaskEntity task);

    @Update
    void update(TaskEntity task);

    @Delete
    void delete(TaskEntity task);

    @Query("DELETE FROM tasks WHERE id IN (:taskIds)")
    void deleteTasksByIds(List<Integer> taskIds);

    @Query("DELETE FROM tasks")
    void deleteAllTasks();

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<TaskWithReminders> getTaskWithReminders(long taskId);

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    TaskWithReminders getTaskWithRemindersSync(long taskId);

    @Transaction
    @Query("SELECT * FROM tasks")
    List<TaskWithReminders> getAllTasksWithRemindersSync();

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void bulkInsertOrUpdate(List<TaskEntity> tasks);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<TaskEntity> getTaskById(int taskId);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    TaskEntity getTaskByIdStatic(long taskId);

    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    List<TaskEntity> getUnsyncedTasks();

    @Query("SELECT * FROM tasks")
    LiveData<List<TaskEntity>> getAllTasks();

    @Query("SELECT * FROM tasks")
    List<TaskEntity> getAllTasksSync();

    @Query("SELECT title FROM tasks")
    List<String> getAllTaskNames();

    @Query("SELECT * FROM tasks WHERE createdAt BETWEEN :start AND :end OR deadline IS NOT NULL AND deadline BETWEEN :start AND :end")
    LiveData<List<TaskEntity>> getTasksWithinPeriodLive(long start, long end);

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    void deleteCompletedTasks();

}
