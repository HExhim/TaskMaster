package com.twa.taskmaster.domain.mapper;

import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.domain.model.TaskLog;

import java.util.ArrayList;
import java.util.List;

public class TaskLogMapper {

    public static TaskLog toModel(TaskLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TaskLog(
                entity.getId(),
                entity.getTimestamp(),
                entity.getDurationMinutes(),
                entity.getSource(),
                entity.getTaskId()
        );
    }

    public static TaskLogEntity toEntity(TaskLog model) {
        if (model == null) {
            return null;
        }
        TaskLogEntity entity = new TaskLogEntity();
        entity.setId(model.getId());
        entity.setTimestamp(model.getTimestamp());
        entity.setDurationMinutes((int) model.getDuration());
        entity.setSource(model.getSource());
        entity.setTaskId(model.getTaskId());
        return entity;
    }

    public static List<TaskLog> toModelList(List<TaskLogEntity> entities) {
        if (entities == null) {
            return null;
        }
        List<TaskLog> models = new ArrayList<>();
        for (TaskLogEntity entity : entities) {
            models.add(toModel(entity));
        }
        return models;
    }
}
