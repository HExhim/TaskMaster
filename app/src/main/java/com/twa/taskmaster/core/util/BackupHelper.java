package com.twa.taskmaster.core.util;

import android.content.Context;
import android.util.Log;

import com.twa.taskmaster.data.local.database.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class BackupHelper {

    private static final String DB_NAME = "app_database";

    public static boolean backupDatabase(Context context) {
        // Ensure all data is written to disk
        Database.checkpoint();

        File dbFile = context.getDatabasePath(DB_NAME);
        File dbWalFile = context.getDatabasePath(DB_NAME + "-wal");
        File dbShmFile = context.getDatabasePath(DB_NAME + "-shm");

        File backupDir = new File(context.getExternalFilesDir(null), "backups");
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                return false;
            }
        }

        try {
            copyFile(dbFile, new File(backupDir, DB_NAME));
            if (dbWalFile.exists()) copyFile(dbWalFile, new File(backupDir, DB_NAME + "-wal"));
            if (dbShmFile.exists()) copyFile(dbShmFile, new File(backupDir, DB_NAME + "-shm"));
            return true;
        } catch (IOException e) {
            Log.e("BackupHelper", "Backup failed", e);
            return false;
        }
    }

    public static boolean restoreDatabase(Context context) {
        File backupDir = new File(context.getExternalFilesDir(null), "backups");
        File backupDb = new File(backupDir, DB_NAME);
        
        if (!backupDb.exists()) {
            return false;
        }

        // Close database if open to release locks
        Database.destroyInstance();

        File dbFile = context.getDatabasePath(DB_NAME);
        File dbWalFile = context.getDatabasePath(DB_NAME + "-wal");
        File dbShmFile = context.getDatabasePath(DB_NAME + "-shm");

        try {
            copyFile(backupDb, dbFile);
            
            File backupWal = new File(backupDir, DB_NAME + "-wal");
            if (backupWal.exists()) copyFile(backupWal, dbWalFile);
            else if (dbWalFile.exists()) dbWalFile.delete();
            
            File backupShm = new File(backupDir, DB_NAME + "-shm");
            if (backupShm.exists()) copyFile(backupShm, dbShmFile);
            else if (dbShmFile.exists()) dbShmFile.delete();
            
            return true;
        } catch (IOException e) {
            Log.e("BackupHelper", "Restore failed", e);
            return false;
        }
    }
    
    public static long getLastBackupTime(Context context) {
        File backupDir = new File(context.getExternalFilesDir(null), "backups");
        File backupDb = new File(backupDir, DB_NAME);
        if (backupDb.exists()) {
            return backupDb.lastModified();
        }
        return -1;
    }

    public static boolean deleteBackup(Context context) {
        File backupDir = new File(context.getExternalFilesDir(null), "backups");
        File backupDb = new File(backupDir, DB_NAME);
        File backupWal = new File(backupDir, DB_NAME + "-wal");
        File backupShm = new File(backupDir, DB_NAME + "-shm");

        boolean deleted = true;
        if (backupDb.exists()) deleted &= backupDb.delete();
        if (backupWal.exists()) deleted &= backupWal.delete();
        if (backupShm.exists()) deleted &= backupShm.delete();

        return deleted;
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (FileChannel inChannel = new FileInputStream(src).getChannel();
             FileChannel outChannel = new FileOutputStream(dst).getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
    }
}
