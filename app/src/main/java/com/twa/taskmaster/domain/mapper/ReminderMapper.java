package com.twa.taskmaster.domain.mapper;

import com.twa.taskmaster.data.local.entity.ReminderEntity;
import com.twa.taskmaster.domain.model.Reminder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReminderMapper {

    public static Reminder toDomain(ReminderEntity entity) {
        Reminder reminder = new Reminder();
        reminder.setId(entity.getId());
        reminder.setTaskId(entity.getTaskId());
        reminder.setReminderTime(entity.getReminderTime());
        return reminder;
    }

    public static ReminderEntity toEntity(Reminder model) {
        ReminderEntity entity = new ReminderEntity();
        entity.setId(model.getId());
        entity.setTaskId(model.getTaskId());
        entity.setReminderTime(model.getReminderTime());

        return entity;
    }
    public static List<Reminder> toDomainList(List<ReminderEntity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return Collections.emptyList();
        }
        return entityList.stream()
                .map(ReminderMapper::toDomain) // Converts each ReminderEntity to a Reminder
                .collect(Collectors.toList());
    }

    // You might also need the reverse mapping for consistency
    public static List<ReminderEntity> toEntityList(List<Reminder> domainList) {
        if (domainList == null || domainList.isEmpty()) {
            return Collections.emptyList();
        }
        return domainList.stream()
                .map(ReminderMapper::toEntity)
                .collect(Collectors.toList());
    }
}
