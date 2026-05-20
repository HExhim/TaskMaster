package com.twa.taskmaster.data.repository;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.twa.taskmaster.data.local.dao.DeletedItemDao;
import com.twa.taskmaster.data.local.dao.ReminderDao;
import com.twa.taskmaster.data.local.dao.TaskDao;
import com.twa.taskmaster.data.local.database.Database;
import com.twa.taskmaster.data.local.entity.DeletedItemEntity;
import com.twa.taskmaster.data.local.entity.ReminderEntity;
import com.twa.taskmaster.data.local.entity.TaskEntity;
import com.twa.taskmaster.domain.mapper.ReminderMapper;
import com.twa.taskmaster.domain.model.Reminder;
import com.twa.taskmaster.domain.model.Task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderRepository {

    private final ReminderDao reminderDao;
    private final TaskDao taskDao;
    private final DeletedItemDao deletedItemDao;
    private final TaskRepository taskRepository; // Added TaskRepository
    private final ExecutorService executorService;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    Context context;

    public ReminderRepository(Application application) {
        Database database = Database.getInstance(application);
        context = application.getApplicationContext();
        reminderDao = database.reminderDao();
        taskDao = database.taskDao();
        deletedItemDao = database.deletedItemDao();
        taskRepository = new TaskRepository(application); // Initialize TaskRepository
        executorService = Executors.newSingleThreadExecutor();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void insert(Reminder reminder) {
        executorService.execute(() -> {
            reminder.setSynced(false);
            ReminderEntity entity = ReminderMapper.toEntity(reminder);
            reminderDao.insert(entity);
        });
    }

    public void update(Reminder reminder) {
        executorService.execute(() -> {
            reminder.setSynced(false);
            ReminderEntity entity = ReminderMapper.toEntity(reminder);
            reminderDao.update(entity);
        });
    }
    public void deleteAllReminders() {
        executorService.execute(reminderDao::deleteAllReminders);
    }

    public void delete(Reminder reminder) {
        executorService.execute(() -> {
            ReminderEntity entity = ReminderMapper.toEntity(reminder);
            reminderDao.delete(entity);
            deleteFromFirebase(reminder.getId());
        });
    }

    public void deleteReminderById(long reminderId) {
        executorService.execute(() -> {
            reminderDao.deleteById(reminderId);
            deleteFromFirebase(reminderId);
            Log.d("ReminderRepository", "Deleted Reminder by ID: " + reminderId);
        });
    }

    private String getUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private void deleteFromFirebase(long reminderId) {
        String uid = getUid();
        if (uid == null) return;
        Log.d("ReminderRepository", "Deleting from Firebase: Reminder ID: " + reminderId);
        db.collection("users").document(uid).collection("reminders")
                .document(String.valueOf(reminderId))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("ReminderRepository", "Deleted from Firebase: Reminder ID: " + reminderId))
                .addOnFailureListener(e -> executorService.execute(() -> 
                    deletedItemDao.insert(new DeletedItemEntity("REMINDER", String.valueOf(reminderId), System.currentTimeMillis()))));
    }

    public void syncLocalToRemote() {
        String uid = getUid();
        if (uid == null) return;

        executorService.execute(() -> {
            List<ReminderEntity> unsynced = reminderDao.getUnsyncedReminders();
            for (ReminderEntity entity : unsynced) {
                Reminder reminder = ReminderMapper.toDomain(entity);
                db.collection("users").document(uid).collection("reminders").document(String.valueOf(reminder.getId()))
                        .set(reminder)
                        .addOnSuccessListener(aVoid -> {
                            entity.setSynced(true);
                            executorService.execute(() -> reminderDao.update(entity));
                        });
            }
        });
    }

    public void fetchRemoteToLocal() {
        String uid = getUid();
        if (uid == null) return;

        // First, fetch tasks to ensure referential integrity
        taskRepository.fetchRemoteToLocal();

        // Then fetch reminders
        db.collection("users").document(uid).collection("reminders").get()
                .addOnSuccessListener(snapshots -> executorService.execute(() -> {
                    List<Reminder> reminders = snapshots.toObjects(Reminder.class);
                    for (Reminder reminder : reminders) {
                        reminder.setSynced(true);
                        ReminderEntity entity = ReminderMapper.toEntity(reminder);

                        // Check if the task exists locally before inserting the reminder
                        // We can retry finding the task if fetchRemoteToLocal for tasks has completed
                        TaskEntity task = taskDao.getTaskByIdStatic(entity.getTaskId());
                        if (task != null) {
                            reminderDao.insert(entity);
                        } else {
                            Log.w("ReminderRepository", "Skipping reminder insertion because task with ID " + entity.getTaskId() + " does not exist.");
                        }
                    }
                }));
    }
}
