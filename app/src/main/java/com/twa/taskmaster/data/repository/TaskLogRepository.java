package com.twa.taskmaster.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.twa.taskmaster.core.enums.TimePeriod;
import com.twa.taskmaster.data.local.dao.DeletedItemDao;
import com.twa.taskmaster.data.local.dao.TaskDao;
import com.twa.taskmaster.data.local.dao.TaskLogDao;
import com.twa.taskmaster.data.local.database.Database;
import com.twa.taskmaster.data.local.entity.DeletedItemEntity;
import com.twa.taskmaster.data.local.entity.TaskEntity;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.domain.mapper.TaskLogMapper;
import com.twa.taskmaster.domain.model.TaskLog;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskLogRepository {
    private final TaskLogDao logDao;
    private final TaskDao taskDao;
    private final DeletedItemDao deletedItemDao;
    private final ExecutorService executorService;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public TaskLogRepository(Application application) {
        Database db = Database.getInstance(application);
        this.logDao = db.taskLogDao();
        this.taskDao = db.taskDao();
        this.deletedItemDao = db.deletedItemDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public LiveData<List<TaskLogEntity>> getLogsForTask(long taskId) {
        return logDao.getLogsForTask(taskId);
    }

    public LiveData<List<TaskLogEntity>> getLogsForTaskToday(long taskId) {
        long startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return logDao.getLogsForTaskToday(taskId, startOfDay);
    }

    public LiveData<List<TaskLogEntity>> getLogsForTaskWeek(long taskId) {
        long startOfWeek = LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return logDao.getLogsForTaskWeek(taskId, startOfWeek);
    }

    public LiveData<List<TaskLogEntity>> getLogsForTaskMonth(long taskId) {
        long startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return logDao.getLogsForTaskMonth(taskId, startOfMonth);
    }

    public void insertLog(TaskLogEntity log) {
        executorService.execute(() -> {
            log.setSynced(false);
            logDao.insertLog(log);

            TaskEntity task = taskDao.getTaskByIdStatic(log.getTaskId());
            if (task != null) {
                long durationMillis = log.getDurationMinutes() * 60 * 1000L;
                task.setTimeSpent(durationMillis);
                taskDao.update(task);
            }
        });
    }
    // For Dummy Data Generator
    public void deleteAllLogs() {
        executorService.execute(logDao::deleteAlllogs);
    }

    public void deleteLogsforTask(int taskId) {
        executorService.execute(() -> {
            List<TaskLogEntity> logs = logDao.getLogsForTaskExport(taskId);
            if (logs != null && !logs.isEmpty()) {
                long totalDuration = 0;
                for (TaskLogEntity log : logs) {
                    totalDuration += log.getDurationMinutes() * 60 * 1000L;
                }
                if (totalDuration > 0) {
                    TaskEntity task = taskDao.getTaskByIdStatic(taskId);
                    if (task != null) {
                        task.setTimeSpent(-totalDuration);
                        taskDao.update(task);
                    }
                }
                String uid = getUid();
                for (TaskLogEntity log : logs) {
                    if (uid != null) {
                        db.collection("users").document(uid).collection("task_logs")
                                .document(String.valueOf(log.getId())).delete()
                                .addOnFailureListener(e -> executorService.execute(() -> 
                                    deletedItemDao.insert(new DeletedItemEntity("LOG", String.valueOf(log.getId()), System.currentTimeMillis()))));
                    } else {
                        deletedItemDao.insert(new DeletedItemEntity("LOG", String.valueOf(log.getId()), System.currentTimeMillis()));
                    }
                }
            }
            logDao.deleteLogsForTask(taskId);
        });
    }
    
    public void deleteLogsByIds(List<Long> logIds) {
        executorService.execute(() -> {
            List<TaskLogEntity> logs = logDao.getLogsByIds(logIds);
            String uid = getUid();
            
            if (logs != null && !logs.isEmpty()) {
                for (TaskLogEntity log : logs) {
                    TaskEntity task = taskDao.getTaskByIdStatic(log.getTaskId());
                    if (task != null) {
                        long durationMillis = log.getDurationMinutes() * 60 * 1000L;
                        task.setTimeSpent(-durationMillis);
                        taskDao.update(task);
                    }
                    
                    if (uid != null) {
                        db.collection("users").document(uid).collection("task_logs")
                                .document(String.valueOf(log.getId())).delete()
                                .addOnFailureListener(e -> executorService.execute(() -> 
                                    deletedItemDao.insert(new DeletedItemEntity("LOG", String.valueOf(log.getId()), System.currentTimeMillis()))));
                    } else {
                        deletedItemDao.insert(new DeletedItemEntity("LOG", String.valueOf(log.getId()), System.currentTimeMillis()));
                    }
                }
            }
            
            logDao.deleteLogsByIds(logIds);
        });
    }

    public LiveData<List<TaskLogEntity>> getLogsForPeriod(TimePeriod period) {
        Calendar cal = Calendar.getInstance();
        long endDate = cal.getTimeInMillis();

        if (period == TimePeriod.ALL) {
            return logDao.getLogsBetweenDates(0, endDate);
        }

        cal.add(Calendar.DATE, -period.getDays());
        long startDate = cal.getTimeInMillis();

        return logDao.getLogsBetweenDates(startDate, endDate);
    }

    public void deleteLogsforTasks(ArrayList<Integer> tasks) {
        // This is usually handled by cascading or task deletion, but if called directly
        executorService.execute(() -> {
            String uid = getUid();
            // This method in DAO deletes based on TaskID list, so we might miss capturing individual log IDs for remote deletion if we just call DAO
            // Better to iterate if precise sync is needed, but for now let's assume Task deletion handles it or we implement similar logic
            // Since the argument is taskIds, we should probably query logs first
            // However, the original method was just a simple DAO call. If we want full offline sync for this:
             for (Integer taskId : tasks) {
                 deleteLogsforTask(taskId); // Reuse the method that handles tracking
             }
        });
    }

    private String getUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void syncLocalToRemote() {
        String uid = getUid();
        if (uid == null) return;

        executorService.execute(() -> {
            List<TaskLogEntity> unsyncedLogs = logDao.getUnsyncedLogs();
            for (TaskLogEntity log : unsyncedLogs) {
                db.collection("users").document(uid).collection("task_logs").document(String.valueOf(log.getId())).set(log)
                        .addOnSuccessListener(aVoid -> {
                            log.setSynced(true);
                            executorService.execute(() -> logDao.insertLog(log));
                        });
            }
        });
    }

    public void fetchRemoteToLocal() {
        String uid = getUid();
        if (uid == null) return;

        db.collection("users").document(uid).collection("task_logs").get()
                .addOnSuccessListener(snapshots -> {
                    List<TaskLogEntity> logs = snapshots.toObjects(TaskLogEntity.class);
                    for (TaskLogEntity log : logs) {
                        log.setSynced(true);
                    }
                    executorService.execute(() -> logDao.bulkInsert(logs));
                });
    }

    public List<TaskLog> getLogsForTaskExport(long id) {
        return TaskLogMapper.toModelList(logDao.getLogsForTaskExport(id));
    }
}
