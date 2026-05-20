package com.twa.taskmaster.domain.mapper;

import android.text.TextUtils;

import com.twa.taskmaster.core.enums.TaskExecutionType;
import com.twa.taskmaster.data.local.entity.TaskEntity;
import com.twa.taskmaster.domain.model.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TaskMapper {

    public static Task toDomain(TaskEntity entity) {
        Task task = new Task();
        task.setId(entity.getId());
        task.setTitle(entity.getTitle());
        task.setDescription(entity.getDescription());
        task.setCategory(entity.getCategory());
        task.setPriority(entity.getPriority());
        task.setDeadline(entity.getDeadline());
        task.setStartDateTime(entity.getStartDateTime());
        task.setCreatedAt(entity.getCreatedAt());
        task.setUpdatedAt(entity.getUpdatedAt());
        task.setTimeSpent(entity.getTimeSpent());
        if (entity.getExecutionType() != null) {
            task.setExecutionType(TaskExecutionType.valueOf(entity.getExecutionType()));
        }
        task.setSynced(entity.isSynced());
        task.setCompleted(entity.isCompleted());
        
        if (entity.getRecurrenceDays() != null && !entity.getRecurrenceDays().isEmpty()) {
            List<Integer> days = Arrays.stream(entity.getRecurrenceDays().split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            task.setRecurrenceDaysOfWeek(days);
        }
        
        return task;
    }

    public static TaskEntity toEntity(Task task) {
        TaskEntity entity = new TaskEntity();
        entity.setId(task.getId());
        entity.setTitle(task.getTitle());
        entity.setDescription(task.getDescription());
        entity.setCategory(task.getCategory());
        entity.setPriority(task.getPriority());
        entity.setDeadline(task.getDeadline());
        entity.setStartDateTime(task.getStartDateTime());
        entity.setCreatedAt(task.getCreatedAt());
        entity.setUpdatedAt(task.getUpdatedAt());
        entity.setTimeSpent(task.getTimeSpent());
        if (task.getExecutionType() != null) {
            entity.setExecutionType(task.getExecutionType().name());
        }
        entity.setSynced(task.isSynced());
        entity.setCompleted(task.isCompleted());
        
        if (task.getRecurrenceDaysOfWeek() != null && !task.getRecurrenceDaysOfWeek().isEmpty()) {
            String days = task.getRecurrenceDaysOfWeek().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            entity.setRecurrenceDays(days);
        }
        
        return entity;
    }
    public static List<Task> toDomainList(List<TaskEntity> entities) {
        List<Task> tasks = new ArrayList<>();
        if (entities != null) {
            for (TaskEntity entity : entities) {
                tasks.add(toDomain(entity));
            }
        }
        return tasks;
    }

    public static List<TaskEntity> toEntityList(List<Task> tasks) {
        List<TaskEntity> entities = new ArrayList<>();
        if (tasks != null) {
            for (Task task : tasks) {
                entities.add(toEntity(task));
            }
        }
        return entities;
    }
}
