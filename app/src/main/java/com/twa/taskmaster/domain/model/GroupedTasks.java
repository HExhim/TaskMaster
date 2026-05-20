package com.twa.taskmaster.domain.model;

import java.util.List;

public class GroupedTasks {
    private final String groupTitle;
    private final List<Task> tasks;

    public GroupedTasks(String groupTitle, List<Task> tasks) {
        this.groupTitle = groupTitle;
        this.tasks = tasks;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public List<Task> getTasks() {
        return tasks;
    }
}

