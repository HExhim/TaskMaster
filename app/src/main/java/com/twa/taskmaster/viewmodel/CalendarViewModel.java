package com.twa.taskmaster.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.domain.model.Task;

import java.util.List;

public class CalendarViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final LiveData<List<Task>> allTasks;

    public CalendarViewModel(Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        allTasks = taskRepository.getAllTasks(); // Must return LiveData from repo
    }

    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }
}

