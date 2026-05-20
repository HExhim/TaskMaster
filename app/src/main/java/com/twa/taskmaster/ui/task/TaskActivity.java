package com.twa.taskmaster.ui.task;

import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.TaskExecutionType;
import com.twa.taskmaster.core.reminder.ReminderScheduler;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.ui.main_activities.BaseActivity;
import com.twa.taskmaster.viewmodel.TaskViewModel;

public class TaskActivity extends BaseActivity {
    private TaskUiBinder uiBinder;
    private TaskInputHandler inputHandler;
    private TaskTypeHandler taskTypeHandler;
    private TaskViewModel taskViewModel;
    private boolean isEditMode = false;
    private int taskId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);

        uiBinder = new TaskUiBinder(this);
        inputHandler = new TaskInputHandler(this, uiBinder);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskTypeHandler = new TaskTypeHandler(this);

        setupTaskMode();
        setupButtons();
    }

    private void setupButtons() {
        uiBinder.getBtnSaveTask().setOnClickListener(v -> {
            Task task = inputHandler.buildTask();
            if (task == null) return;

            task.setExecutionType(taskTypeHandler.getExecutionType());
            taskTypeHandler.setExecutionType(task.getExecutionType());

            if (isEditMode) {
                task.setId(taskId);
                taskViewModel.update(task);
            } else {
                taskViewModel.insert(task);
            }
            finish();
        });
    }

    private void setupTaskMode() {
        Task task = (Task) getIntent().getSerializableExtra("task_data");
        if (task != null) {
            isEditMode = true;
            taskId = task.getId();
            inputHandler.populateFields(task);
        }
    }
    public TaskTypeHandler getTaskTypeHandler() {
        return taskTypeHandler;
    }

}
