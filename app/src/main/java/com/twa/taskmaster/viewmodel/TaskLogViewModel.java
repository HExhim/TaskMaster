package com.twa.taskmaster.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.twa.taskmaster.core.enums.SortLogs;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.domain.mapper.TaskLogMapper;
import com.twa.taskmaster.domain.model.TaskLog;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TaskLogViewModel extends AndroidViewModel {

    private static final LiveData<List<TaskLog>> EMPTY_LOGS = new MutableLiveData<>(Collections.emptyList());

    private final TaskLogRepository repository;
    private final MediatorLiveData<List<TaskLog>> taskLogs = new MediatorLiveData<>();
    private final MutableLiveData<SortLogs> sortType = new MutableLiveData<>(SortLogs.DATE_NEWEST);
    private final MutableLiveData<String> sourceFilter = new MutableLiveData<>("All");

    private final MutableLiveData<Long> taskIdLiveData = new MutableLiveData<>();
    private final LiveData<List<TaskLog>> logsSource;

    public TaskLogViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskLogRepository(application);

        logsSource = Transformations.switchMap(taskIdLiveData, id -> {
            if (id == null) {
                return EMPTY_LOGS;
            }
            return Transformations.map(repository.getLogsForTask(id), TaskLogMapper::toModelList);
        });

        taskLogs.addSource(logsSource, logs -> applyFilters());
        taskLogs.addSource(sortType, type -> applyFilters());
        taskLogs.addSource(sourceFilter, source -> applyFilters());
    }

    public LiveData<List<TaskLog>> getTaskLogs() {
        return taskLogs;
    }

    public void loadTaskLogs(long taskId) {
        if (Long.valueOf(taskId).equals(taskIdLiveData.getValue())) {
            return;
        }
        this.taskIdLiveData.setValue(taskId);
    }

    public void sortLogs(SortLogs sortType) {
        this.sortType.setValue(sortType);
    }

    public void filterLogs(String source) {
        this.sourceFilter.setValue(source);
    }

    private void applyFilters() {
        List<TaskLog> logs = logsSource.getValue();
        if (logs == null) {
            taskLogs.setValue(Collections.emptyList());
            return;
        }

        Stream<TaskLog> logStream = logs.stream();

        // Apply source filter
        String sourceValue = sourceFilter.getValue();
        if (sourceValue != null && !"All".equals(sourceValue)) {
            logStream = logStream.filter(log -> log.getSource().equalsIgnoreCase(sourceValue));
        }

        // Apply sort
        SortLogs sort = sortType.getValue();
        if (sort != null) {
            Comparator<TaskLog> comparator;
            switch (sort) {
                case DATE_OLDEST:
                    comparator = Comparator.comparingLong(TaskLog::getTimestamp);
                    break;
                case DURATION_LONGEST:
                    comparator = Comparator.comparingLong(TaskLog::getDuration).reversed();
                    break;
                case DURATION_SHORTEST:
                    comparator = Comparator.comparingLong(TaskLog::getDuration);
                    break;
                case DATE_NEWEST:
                default:
                    comparator = Comparator.comparingLong(TaskLog::getTimestamp).reversed();
                    break;
            }
            logStream = logStream.sorted(comparator);
        }

        taskLogs.setValue(logStream.collect(Collectors.toList()));
    }

    public void insertLog(TaskLogEntity log) {
        repository.insertLog(log);
    }


}
