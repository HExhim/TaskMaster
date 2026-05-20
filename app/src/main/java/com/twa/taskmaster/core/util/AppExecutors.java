package com.twa.taskmaster.core.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {
    private static final Object LOCK = new Object();
    private static AppExecutors instance;
    private final Executor diskIO = Executors.newSingleThreadExecutor();

    private AppExecutors() {}

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                instance = new AppExecutors();
            }
        }
        return instance;
    }

    public Executor diskIO() {
        return diskIO;
    }
}

