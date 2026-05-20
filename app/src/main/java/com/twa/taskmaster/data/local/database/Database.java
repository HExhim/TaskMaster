package com.twa.taskmaster.data.local.database;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.twa.taskmaster.data.local.dao.CategoryDao;
import com.twa.taskmaster.data.local.dao.DeletedItemDao;
import com.twa.taskmaster.data.local.dao.ReminderDao;
import com.twa.taskmaster.data.local.entity.CategoryEntity;
import com.twa.taskmaster.data.local.entity.DeletedItemEntity;
import com.twa.taskmaster.data.local.entity.ReminderEntity;
import com.twa.taskmaster.data.local.entity.SubtaskEntity;
import com.twa.taskmaster.data.local.entity.TaskEntity;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.data.local.dao.TaskDao;
import com.twa.taskmaster.data.local.dao.TaskLogDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.room.Database(entities = {TaskEntity.class, CategoryEntity.class, ReminderEntity.class, TaskLogEntity.class, DeletedItemEntity.class}, version = 2, exportSchema = false)
public abstract class Database extends RoomDatabase {

    private static Database instance;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);


    public abstract TaskDao taskDao();
    public abstract TaskLogDao taskLogDao();
    public abstract ReminderDao reminderDao();
    public abstract CategoryDao categoryDao();
    public abstract DeletedItemDao deletedItemDao();

    public static synchronized Database getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            Database.class, "app_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public static void checkpoint() {
        if (instance != null) {
            instance.getOpenHelper().getWritableDatabase().query("PRAGMA wal_checkpoint(FULL)");
        }
    }

    public static synchronized void destroyInstance() {
        if (instance != null) {
            if (instance.isOpen()) {
                instance.close();
            }
            instance = null;
        }
    }
}
