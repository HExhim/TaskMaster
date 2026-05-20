package com.twa.taskmaster.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;

import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.domain.model.TaskLog;

import java.util.List;

public class ExportViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final TaskLogRepository logRepository;
    private Task task;

    public ExportViewModel(Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        logRepository = new TaskLogRepository(application);
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    public List<TaskLog> getLogsForTask() { // <-- Assuming the entity is what you need
        // This can return null if LiveData hasn't been populated yet.
        return logRepository.getLogsForTaskExport(task.getId());
    }

}
