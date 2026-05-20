package com.twa.taskmaster.ui.task;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.slider.Slider;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.TaskExecutionType;
import com.google.android.material.textfield.TextInputEditText;

public class TaskTypeHandler {

    private final TaskActivity activity;
    private final RadioGroup taskExecutionGroup;
    private final View dateTimeContainer;
    private final View recurrenceContainer;

    private final TextView taskTypeHelperText;

    private TaskExecutionType executionType;
    private LinearLayout reminderLayout;

    public TaskTypeHandler(TaskActivity activity) {
        this.activity = activity;

        taskExecutionGroup = activity.findViewById(R.id.taskExecutionGroup);
        dateTimeContainer = activity.findViewById(R.id.datetimeLayout);
        recurrenceContainer = activity.findViewById(R.id.recurrenceLayout);
        taskTypeHelperText = activity.findViewById(R.id.taskTypeHelper);
        reminderLayout = activity.findViewById(R.id.reminderLayout);


        initExecutionType();
        setupTaskTypeListener();
    }

    private void initExecutionType() {
        int checkedId = taskExecutionGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.rbInstant) {
            setExecutionType(TaskExecutionType.INSTANT);
        } else if (checkedId == R.id.rbScheduled) {
            setExecutionType(TaskExecutionType.SCHEDULED);
        } else if (checkedId == R.id.rbRecurring) {
            setExecutionType(TaskExecutionType.RECURRING);
        } else {
            taskExecutionGroup.check(R.id.rbInstant);
            setExecutionType(TaskExecutionType.INSTANT);
        }
    }

    private void setupTaskTypeListener() {
        taskExecutionGroup.setOnCheckedChangeListener((group, id) -> {
            if (id == R.id.rbInstant) {
                setExecutionType(TaskExecutionType.INSTANT);
            } else if (id == R.id.rbScheduled) {
                setExecutionType(TaskExecutionType.SCHEDULED);
            } else if (id == R.id.rbRecurring) {
                setExecutionType(TaskExecutionType.RECURRING);
            }
        });
    }

    void setExecutionType(TaskExecutionType type) {
        this.executionType = type;

        switch (type) {
            case INSTANT:
                dateTimeContainer.setVisibility(View.GONE);
                recurrenceContainer.setVisibility(View.GONE);
                reminderLayout.setVisibility(View.GONE);
                taskTypeHelperText.setText("*Instant tasks are created and completed immediately.");
                break;

            case SCHEDULED:
                dateTimeContainer.setVisibility(View.VISIBLE);
                recurrenceContainer.setVisibility(View.GONE);
                reminderLayout.setVisibility(View.VISIBLE);
                taskTypeHelperText.setText("*Scheduled tasks occur once at a specific time.");
                break;

            case RECURRING:
                dateTimeContainer.setVisibility(View.GONE);
                recurrenceContainer.setVisibility(View.VISIBLE);
                reminderLayout.setVisibility(View.VISIBLE);
                taskTypeHelperText.setText("*Recurring tasks repeat periodically based on selected rules.");
                break;
        }
    }

    public TaskExecutionType getExecutionType() {
        return executionType;
    }
}
