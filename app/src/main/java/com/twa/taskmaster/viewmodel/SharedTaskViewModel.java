package com.twa.taskmaster.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.twa.taskmaster.core.enums.SortLogs;
import com.twa.taskmaster.core.enums.TimePeriod;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.domain.model.Task;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SharedTaskViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final MutableLiveData<Long> taskId = new MutableLiveData<>();
    private final MutableLiveData<SortLogs> sortType = new MutableLiveData<>(SortLogs.DATE_NEWEST);
    private final MutableLiveData<String> sourceFilter = new MutableLiveData<>("All");
    private final MutableLiveData<TimePeriod> currentFilter = new MutableLiveData<>(TimePeriod.ALL);
    private final LiveData<Task> task;

    public SharedTaskViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        taskLogRepository = new TaskLogRepository(application);
        task = Transformations.switchMap(taskId, taskRepository::getTask);
    }

    // Returns logs for a specific period (used by LogListFragment)
    public LiveData<List<TaskLogEntity>> getLogs(TimePeriod period) {
        LiveData<List<TaskLogEntity>> logsSource = Transformations.switchMap(taskId, id -> {
            if (id == null) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            switch (period) {
                case TODAY:
                    return taskLogRepository.getLogsForTaskToday(id);
                case WEEK:
                    return taskLogRepository.getLogsForTaskWeek(id);
                case MONTH:
                    return taskLogRepository.getLogsForTaskMonth(id);
                case ALL:
                default:
                    return taskLogRepository.getLogsForTask(id);
            }
        });

        MediatorLiveData<List<TaskLogEntity>> mediator = new MediatorLiveData<>();

        mediator.addSource(logsSource, logs -> mediator.setValue(applyFiltersAndSort(logs)));
        mediator.addSource(sortType, sort -> mediator.setValue(applyFiltersAndSort(logsSource.getValue())));
        mediator.addSource(sourceFilter, source -> mediator.setValue(applyFiltersAndSort(logsSource.getValue())));

        return mediator;
    }

    // Returns logs for the currently selected filter (used by TaskLogFragment for empty state)
    public LiveData<List<TaskLogEntity>> getFilteredLogs() {
        return Transformations.switchMap(currentFilter, this::getLogs);
    }

    private List<TaskLogEntity> applyFiltersAndSort(List<TaskLogEntity> logs) {
        if (logs == null) {
            return Collections.emptyList();
        }

        Stream<TaskLogEntity> logStream = logs.stream();

        // Apply source filter
        String sourceValue = sourceFilter.getValue();
        if (sourceValue != null && !"All".equals(sourceValue)) {
            logStream = logStream.filter(log -> log.getSource().equalsIgnoreCase(sourceValue));
        }

        // Apply sort
        SortLogs sort = sortType.getValue();
        if (sort != null) {
            Comparator<TaskLogEntity> comparator;
            switch (sort) {
                case DATE_OLDEST:
                    comparator = Comparator.comparingLong(TaskLogEntity::getTimestamp);
                    break;
                case DURATION_LONGEST:
                    comparator = Comparator.comparingLong(TaskLogEntity::getDurationMinutes).reversed();
                    break;
                case DURATION_SHORTEST:
                    comparator = Comparator.comparingLong(TaskLogEntity::getDurationMinutes);
                    break;
                case DATE_NEWEST:
                default:
                    comparator = Comparator.comparingLong(TaskLogEntity::getTimestamp).reversed();
                    break;
            }
            logStream = logStream.sorted(comparator);
        }

        return logStream.collect(Collectors.toList());
    }

    public void setTaskId(long taskId) {
        this.taskId.setValue(taskId);
    }

    public void setSort(SortLogs sort) {
        this.sortType.setValue(sort);
    }

    public void setSourceFilter(String source) {
        this.sourceFilter.setValue(source);
    }

    public void setCurrentFilter(TimePeriod filter) {
        this.currentFilter.setValue(filter);
    }

    public void deleteLogs(List<Long> logIds) {
        taskLogRepository.deleteLogsByIds(logIds);
    }

    public LiveData<Task> getTask() {
        return task;
    }

    public String calculateTotalTime(List<TaskLogEntity> logs) {
        long totalMinutes = 0;
        if (logs != null) {
            for (TaskLogEntity log : logs) {
                totalMinutes += log.getDurationMinutes();
            }
        }
        long totalMilliseconds = totalMinutes * 60 * 1000;
        return DateTimeUtils.formatDuration(totalMilliseconds);
    }
}
