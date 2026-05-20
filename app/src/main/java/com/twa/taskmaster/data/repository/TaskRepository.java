package com.twa.taskmaster.data.repository;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.twa.taskmaster.core.reminder.ReminderScheduler;
import com.twa.taskmaster.core.util.CSVExporter;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.data.local.dao.ReminderDao;
import com.twa.taskmaster.data.local.dao.TaskDao;
import com.twa.taskmaster.data.local.dao.TaskLogDao;
import com.twa.taskmaster.data.local.database.Database;
import com.twa.taskmaster.data.local.entity.ReminderEntity;
import com.twa.taskmaster.data.local.entity.TaskEntity;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.data.local.entity.TaskWithReminders;
import com.twa.taskmaster.domain.mapper.ReminderMapper;
import com.twa.taskmaster.domain.mapper.TaskMapper;
import com.twa.taskmaster.domain.model.Reminder;
import com.twa.taskmaster.domain.model.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TaskRepository {

    private final TaskDao taskDao;
    private final ReminderDao reminderDao;
    private final TaskLogDao taskLogDao;
    private final ExecutorService executor;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final Gson gson = new Gson();


    public TaskRepository(Application application) {
        Database database = Database.getInstance(application);
        taskDao = database.taskDao();
        reminderDao = database.reminderDao();
        taskLogDao = database.taskLogDao();
        executor = Executors.newSingleThreadExecutor();
        this.context = application.getApplicationContext();
    }

    public LiveData<Task> getTask(long taskId) {
        return Transformations.map(taskDao.getTaskWithReminders(taskId), taskWithReminders -> {
            if (taskWithReminders != null) {
                Task task = TaskMapper.toDomain(taskWithReminders.task);
                if (taskWithReminders.reminders != null) {
                    task.setReminders(ReminderMapper.toDomainList(taskWithReminders.reminders));
                }
                return task;
            }
            return null;
        });
    }
    public LiveData<List<Task>> getAllTasks() {
        return Transformations.map(taskDao.getAllTasks(), TaskMapper::toDomainList);
    }
    public LiveData<Task> getTaskById(int taskId) {
        return Transformations.map(taskDao.getTaskById(taskId), TaskMapper::toDomain);
    }
    public LiveData<List<Task>> getTasksForPeriod(long start, long end) {
        return Transformations.map(taskDao.getTasksWithinPeriodLive(start, end), TaskMapper::toDomainList);
    }
    //Only for Dummy Data Generator
    public long insertTask(Task task) {
        Future<Long> future = executor.submit(() -> {
            TaskEntity entity = TaskMapper.toEntity(task);
            long id = taskDao.insert(entity);
            task.setId((int) id); // Update the task with the new ID
            
            syncLocalToRemote(); // Trigger sync immediately after insert

            if (task.getReminders() != null && !task.getReminders().isEmpty()) {
                for (Reminder reminder : task.getReminders()) {
                    reminder.setTaskId(id);
                    reminder.setSynced(false);
                    reminderDao.insert(ReminderMapper.toEntity(reminder));
                }
                ReminderScheduler.scheduleReminders(context, task);
            }

            return id;
        });

        try {
            return future.get(); // Wait for the result
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return error value
        }
    }
    public void deleteAlltask(){
        executor.execute(() -> {
            taskDao.deleteAllTasks();

        });
    }

    public void update(Task task) {
        executor.execute(() -> {
            task.setUpdatedAt(System.currentTimeMillis());
            task.setSynced(false);
            taskDao.update(TaskMapper.toEntity(task));
            
            syncLocalToRemote(); // Trigger sync immediately after update

            String uid = getUid();
            List<ReminderEntity> existingReminders = reminderDao.getRemindersForTask(task.getId());
            List<Reminder> newReminders = task.getReminders();

            // Identify and delete removed reminders from Firebase
            if (existingReminders != null) {
                for (ReminderEntity old : existingReminders) {
                    boolean stillExists = false;
                    if (newReminders != null) {
                        for (Reminder newR : newReminders) {
                            if (newR.getId() == old.getId()) {
                                stillExists = true;
                                break;
                            }
                        }
                    }
                    if (!stillExists) {
                         deleteFromFirebase("reminders", String.valueOf(old.getId()));
                    }
                }
            }

            reminderDao.deleteRemindersByTaskId(task.getId());

            if (newReminders != null && !newReminders.isEmpty()) {
                for (Reminder reminder : newReminders) {
                    reminder.setTaskId(task.getId());
                    reminder.setSynced(false);
                    long reminderId = reminderDao.insert(ReminderMapper.toEntity(reminder));
                    reminder.setId((int)reminderId);
                }
            }

            ReminderScheduler.cancelReminders(context, task);
            ReminderScheduler.scheduleReminders(context, task);
        });
    }
    public void delete(Task task) {
        executor.execute(() -> {
            String uid = getUid();
            long taskId = task.getId();

            // 1. Delete Reminders (Firebase & Local)
            List<ReminderEntity> reminders = reminderDao.getRemindersForTask(taskId);
            if (reminders != null) {
                for (ReminderEntity r : reminders) {
                     deleteFromFirebase("reminders", String.valueOf(r.getId()));
                }
            }
            reminderDao.deleteRemindersByTaskId(taskId);

            // 2. Delete Logs (Firebase & Local)
            List<TaskLogEntity> logs = taskLogDao.getLogsForTaskExport(taskId);
            if (logs != null) {
                for (TaskLogEntity log : logs) {
                     deleteFromFirebase("task_logs", String.valueOf(log.getId()));
                }
            }
            taskLogDao.deleteLogsForTask((int) taskId);

            // 3. Delete Task (Firebase & Local)
            TaskEntity taskEntity = TaskMapper.toEntity(task);
            deleteFromFirebase("tasks", String.valueOf(taskId));
            
            taskDao.delete(taskEntity);
            ReminderScheduler.cancelReminders(context, task);
        });
    }

    public void deleteTasksById(List<Integer> taskIds) {
        executor.execute(() -> {
            String uid = getUid();
            for (Integer id : taskIds) {
                long taskId = id;
                deleteFromFirebase("tasks", String.valueOf(taskId));

                // 1. Delete Reminders
                List<ReminderEntity> reminders = reminderDao.getRemindersForTask(taskId);
                if (reminders != null) {
                    for (ReminderEntity r : reminders) {
                         deleteFromFirebase("reminders", String.valueOf(r.getId()));
                    }
                    // Create temp task for scheduler cancellation
                    Task tempTask = new Task();
                    tempTask.setId(id);
                    tempTask.setReminders(ReminderMapper.toDomainList(reminders));
                    ReminderScheduler.cancelReminders(context, tempTask);
                }
                reminderDao.deleteRemindersByTaskId(taskId);

                // 2. Delete Logs
                List<TaskLogEntity> logs = taskLogDao.getLogsForTaskExport(taskId);
                if (logs != null) {
                    for (TaskLogEntity log : logs) {
                        deleteFromFirebase("task_logs", String.valueOf(log.getId()));
                    }
                }
                taskLogDao.deleteLogsForTask((int) taskId);
            }
            // Bulk delete tasks locally
            taskDao.deleteTasksByIds(taskIds);
        });
    }
    
    private void deleteFromFirebase(String collection, String documentId) {
        String uid = getUid();
        if (uid != null) {
            db.collection("users").document(uid).collection(collection).document(documentId)
                    .delete()
                    .addOnFailureListener(e -> Log.e("TaskRepository", "Failed to delete from Firebase: " + collection + "/" + documentId, e));
        }
    }
    
    private String getUid() {
        FirebaseUser user = auth.getCurrentUser();
        // Check if user is anonymous or null. 
        // If we want offline first with guest mode not syncing to unique paths unless we want per-device guest data in firestore (unlikely for guest).
        // Usually guest data is local only.
        if (user != null && user.isAnonymous()) {
            // But typically "Guest" implies local-only. If we return user.getUid(), it will sync to a temporary anonymous account.
            // Let's assume we WANT to sync to anonymous account so data isn't lost if they upgrade later?
            // Actually, `linkWithCredential` can merge anonymous data. So syncing anonymous is fine.
            // However, if offline first implies "no internet", then syncing will just fail or pend.
            // Let's return user.getUid() even for anonymous.
             return user.getUid();
        }
        return user != null ? user.getUid() : null;
    }

    public void syncLocalToRemote() {
        String uid = getUid();
        if (uid == null) return;
        
        executor.execute(() -> {
            List<Task> unsyncedTasks = TaskMapper.toDomainList(taskDao.getUnsyncedTasks());
            for (Task task : unsyncedTasks) {
                db.collection("users").document(uid).collection("tasks").document(String.valueOf(task.getId())).set(task)
                    .addOnSuccessListener(aVoid -> {
                        task.setSynced(true);
                        executor.execute(() -> taskDao.update(TaskMapper.toEntity(task)));
                    });
            }
        });
    }

    public void fetchRemoteToLocal() {
        String uid = getUid();
        if (uid == null) return;

        // Changed from get() to addSnapshotListener for real-time updates
        db.collection("users").document(uid).collection("tasks")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Log.w("TaskRepository", "Listen failed.", e);
                    return;
                }

                if (snapshots != null) {
                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                                Task remoteTask = dc.getDocument().toObject(Task.class);
                                TaskEntity localTaskEntity = taskDao.getTaskByIdStatic(remoteTask.getId());

                                remoteTask.setSynced(true);
                                TaskEntity remoteEntity = TaskMapper.toEntity(remoteTask);

                                if (localTaskEntity == null) {
                                    taskDao.bulkInsertOrUpdate(Collections.singletonList(remoteEntity));
                                } else {
                                    if (remoteEntity.getUpdatedAt() > localTaskEntity.getUpdatedAt()) {
                                        taskDao.bulkInsertOrUpdate(Collections.singletonList(remoteEntity));
                                    }
                                }
                            } else if (dc.getType() == DocumentChange.Type.REMOVED) {
                                // Handle remote deletions if needed, or ignore if only syncing additions/updates
                                // For full sync, we might want to delete locally too if deleted remotely
                                // But usually we need to check timestamps or have a tombstone
                            }
                        }
                    });
                }
            });
    }
    
    // Kept export method...
    public Uri exportTasksToCSV() {
        Future<Uri> future = executor.submit(() -> {
            List<TaskEntity> tasks = taskDao.getAllTasksSync();
            List<String> headers = Arrays.asList("ID", "Title", "Description", "Category", "Priority", "Deadline", "Completed", "Reminders", "Logs");
            List<List<String>> rows = new ArrayList<>();

            for (TaskEntity task : tasks) {
                List<ReminderEntity> reminders = reminderDao.getRemindersForTask(task.getId());
                StringBuilder remindersStr = new StringBuilder();
                if (reminders != null) {
                    for (ReminderEntity r : reminders) {
                        remindersStr.append(DateTimeUtils.formatDateTime(r.getReminderTime())).append("; ");
                    }
                }

                List<TaskLogEntity> logs = taskLogDao.getLogsForTaskExport(task.getId());
                StringBuilder logsStr = new StringBuilder();
                if (logs != null) {
                    for (int i = 0; i < logs.size(); i++) {
                        TaskLogEntity l = logs.get(i);
                        logsStr.append("[#").append(i + 1).append("] ")
                                .append("Duration: ").append(l.getDurationMinutes()).append("m, ")
                                .append("Source: ").append(l.getSource() != null ? l.getSource() : "N/A").append(", ")
                                .append("Time: ").append(DateTimeUtils.formatDateTime(l.getTimestamp())).append(", ")
                                .append("Note: ").append(l.getNote() != null ? l.getNote() : "None")
                                .append("; ");
                    }
                }

                rows.add(Arrays.asList(
                        String.valueOf(task.getId()),
                        task.getTitle(),
                        task.getDescription(),
                        task.getCategory(),
                        task.getPriority(),
                        DateTimeUtils.formatDateTime(task.getDeadline()),
                        String.valueOf(task.isCompleted()),
                        remindersStr.toString(),
                        logsStr.toString()
                ));
            }

            return CSVExporter.exportToCSV(context, "tasks_export", headers, rows);
        });

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e("TaskRepository", "Export failed", e);
            return null;
        }
    }
}
