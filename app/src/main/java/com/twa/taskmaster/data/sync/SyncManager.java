package com.twa.taskmaster.data.sync;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.twa.taskmaster.domain.usecases.SyncAllUseCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncManager {

    private static SyncManager instance;

    // Use a cached thread pool – optimal for mixed Firestore I/O + local DB I/O
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final MutableLiveData<SyncState> syncState =
            new MutableLiveData<>(SyncState.SYNCED);

    private final SyncAllUseCase syncAllUseCase;
    private final AtomicBoolean isSyncRunning = new AtomicBoolean(false);

    // Prevents sync spam — minimum delay between syncs (e.g., 15 seconds)
    private static final long SYNC_DEBOUNCE_MS = 15000;
    private long lastSyncTimestamp = 0;

    private SyncManager(SyncAllUseCase syncAllUseCase) {
        this.syncAllUseCase = syncAllUseCase;
    }

    public static synchronized void initialize(SyncAllUseCase syncAllUseCase) {
        if (instance == null) {
            instance = new SyncManager(syncAllUseCase);
        }
    }

    public static SyncManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SyncManager not initialized.");
        }
        return instance;
    }

    public void syncAll() {
        long now = System.currentTimeMillis();

        // Debounce protection
        if (now - lastSyncTimestamp < SYNC_DEBOUNCE_MS) return;
        lastSyncTimestamp = now;

        // Prevent concurrent or overlapping sync runs
        if (!isSyncRunning.compareAndSet(false, true)) {
            Log.d("SyncManager", "Sync already in progress. Skipping.");
            return;
        }

        syncState.postValue(SyncState.PENDING);

        syncAllUseCase.execute(executor, new SyncAllUseCase.SyncCallback() {

            @Override
            public void onSuccess() {
                syncState.postValue(SyncState.SYNCED);
                isSyncRunning.set(false);
                Log.d("SyncManager", "Sync completed successfully");
            }

            @Override
            public void onError(Exception e) {
                syncState.postValue(SyncState.NO_CONNECTION);
                isSyncRunning.set(false);
                Log.e("SyncManager", "Sync failed", e);
            }
        });
    }

    public LiveData<SyncState> getSyncState() {
        return syncState;
    }

    private String getUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public enum SyncState { SYNCED, PENDING, NO_CONNECTION }
}
