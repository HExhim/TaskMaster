package com.twa.taskmaster.domain.usecases;

import com.twa.taskmaster.data.repository.ReminderRepository;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.data.repository.TaskRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class SyncAllUseCase {

    private final TaskRepository taskRepo;
    private final ReminderRepository reminderRepo;
    private final TaskLogRepository taskLogRepo;

    public SyncAllUseCase(TaskRepository taskRepo,
                          ReminderRepository reminderRepo,
                          TaskLogRepository taskLogRepo) {

        if (taskRepo == null || reminderRepo == null || taskLogRepo == null) {
            throw new IllegalArgumentException("Repositories cannot be null");
        }

        this.taskRepo = taskRepo;
        this.reminderRepo = reminderRepo;
        this.taskLogRepo = taskLogRepo;
    }

    public void execute(ExecutorService executor, SyncCallback callback) {
        executor.execute(() -> {

            CountDownLatch latch = new CountDownLatch(3);
            Exception[] error = new Exception[1];

            runRepoSync(executor, latch, error, () -> {
                taskRepo.syncLocalToRemote();
                taskRepo.fetchRemoteToLocal();
            });

            runRepoSync(executor, latch, error, () -> {
                reminderRepo.syncLocalToRemote();
                reminderRepo.fetchRemoteToLocal();
            });

            runRepoSync(executor, latch, error, () -> {
                taskLogRepo.syncLocalToRemote();
                taskLogRepo.fetchRemoteToLocal();
            });

            try {
                latch.await();
                if (error[0] == null) {
                    callback.onSuccess();
                } else {
                    callback.onError(error[0]);
                }
            } catch (InterruptedException e) {
                callback.onError(e);
            }
        });
    }

    private void runRepoSync(ExecutorService executor,
                             CountDownLatch latch,
                             Exception[] err,
                             Runnable block) {

        executor.execute(() -> {
            try {
                block.run();
            } catch (Exception e) {
                err[0] = e;
            } finally {
                latch.countDown();
            }
        });
    }

    public interface SyncCallback {
        void onSuccess();

        void onError(Exception e);
    }
}
