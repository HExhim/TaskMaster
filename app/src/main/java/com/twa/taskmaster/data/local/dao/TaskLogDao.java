package com.twa.taskmaster.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.twa.taskmaster.data.local.entity.TaskLogEntity;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface TaskLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLog(TaskLogEntity log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void bulkInsert(List<TaskLogEntity> logs);

    @Query("DELETE FROM task_logs")
    void deleteAlllogs();

    @Query("DELETE FROM task_logs WHERE taskId = :taskId")
    void deleteLogsForTask(int taskId);

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId ORDER BY timestamp ASC")
    LiveData<List<TaskLogEntity>> getLogsForTask(long taskId);

    @Query("SELECT * FROM task_logs WHERE isSynced = 0")
    List<TaskLogEntity> getUnsyncedLogs();

    @Query("SELECT * FROM task_logs WHERE " +
            "timestamp >= :startDate AND timestamp <= :endDate")
    LiveData<List<TaskLogEntity>> getLogsBetweenDates(long startDate, long endDate);


    @Query("DELETE FROM task_logs WHERE taskId IN (:tasks)")
    void deleteLogsForTasks(ArrayList<Integer> tasks);

    @Query("DELETE FROM task_logs WHERE id IN (:logIds)")
    void deleteLogsByIds(List<Long> logIds);

    @Query("SELECT * FROM task_logs WHERE id IN (:logIds)")
    List<TaskLogEntity> getLogsByIds(List<Long> logIds);

    @Query("SELECT * FROM task_logs WHERE taskId = :id")
    List<TaskLogEntity> getLogsForTaskExport(long id);

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId AND timestamp >= :startOfDay")
    LiveData<List<TaskLogEntity>> getLogsForTaskToday(long taskId, long startOfDay);

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId AND timestamp >= :startOfWeek")
    LiveData<List<TaskLogEntity>> getLogsForTaskWeek(long taskId, long startOfWeek);

    @Query("SELECT * FROM task_logs WHERE taskId = :taskId AND timestamp >= :startOfMonth")
    LiveData<List<TaskLogEntity>> getLogsForTaskMonth(long taskId, long startOfMonth);

}
