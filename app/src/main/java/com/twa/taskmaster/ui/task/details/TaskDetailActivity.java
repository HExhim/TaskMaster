package com.twa.taskmaster.ui.task.details;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.util.AppExecutors;
import com.twa.taskmaster.core.util.CSVExporter;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.data.local.database.Database;
import com.twa.taskmaster.data.local.entity.TaskWithReminders;
import com.twa.taskmaster.domain.mapper.ReminderMapper;
import com.twa.taskmaster.domain.mapper.TaskMapper;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.domain.model.TaskLog;
import com.twa.taskmaster.ui.main_activities.BaseActivity;
import com.twa.taskmaster.ui.task.TaskActivity;
import com.twa.taskmaster.ui.task.analytics.TaskAnalyticsFragment;
import com.twa.taskmaster.ui.task.logs.TaskLogFragment;
import com.twa.taskmaster.viewmodel.ExportViewModel;
import com.twa.taskmaster.viewmodel.SharedTaskViewModel;
import com.twa.taskmaster.viewmodel.TaskViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskDetailActivity extends BaseActivity {

    private Task task;
    private SharedTaskViewModel sharedViewModel;
    private TaskViewModel viewModel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        sharedViewModel = new ViewModelProvider(this).get(SharedTaskViewModel.class);
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        if (getIntent() != null && getIntent().hasExtra("task_data")) {
            task = (Task) getIntent().getSerializableExtra("task_data");
            sharedViewModel.setTaskId(task.getId());
            setupUi();
        } else if (getIntent() != null && getIntent().hasExtra("task_id")) {
            long taskId = getIntent().getLongExtra("task_id", -1L);
            if (taskId != -1L) {
                loadTaskFromDb(taskId);
            } else {
                Toast.makeText(this, "Invalid Task ID", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No Task data provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadTaskFromDb(long taskId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            TaskWithReminders taskWithReminders = Database.getInstance(getApplicationContext()).taskDao().getTaskWithRemindersSync(taskId);
            runOnUiThread(() -> {
                if (taskWithReminders != null) {
                    this.task = TaskMapper.toDomain(taskWithReminders.task);
                    if (taskWithReminders.reminders != null) {
                        this.task.setReminders(ReminderMapper.toDomainList(taskWithReminders.reminders));
                    }
                    sharedViewModel.setTaskId(this.task.getId());
                    setupUi();
                } else {
                    Toast.makeText(TaskDetailActivity.this, "Task not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void setupUi() {
        setupToolbar();
        setupBottomNavigation();
        // Load first fragment by default
        if (getSupportFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            loadFragment(TaskDetailsFragment.newInstance());
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            if (item.getItemId() == R.id.nav_details)
                selected = TaskDetailsFragment.newInstance();
            else if (item.getItemId() == R.id.nav_analytics)
                selected = TaskAnalyticsFragment.newInstance();
            else if (item.getItemId() == R.id.nav_logs)
                selected = TaskLogFragment.newInstance();

            if (selected != null) {
                loadFragment(selected);
                return true;
            }
            return false;
        });
    }


    private void loadFragment(Fragment fragment) {
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }


    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(task != null ? task.getTitle() : "Task Details");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.edit) {
            navigateToEditTask();
            return true;
        } else if (id == R.id.share) {
            ExportCSV();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ExportCSV() {
        ExportViewModel viewModel = new ViewModelProvider(this).get(ExportViewModel.class);
        viewModel.setTask(task);

        new Thread(() -> {
            Task task = viewModel.getTask();
            List<TaskLog> logs = viewModel.getLogsForTask();

            List<String> headers = Arrays.asList("Field", "Value");
            List<List<String>> rows = new ArrayList<>();

            // SECTION: TASK INFO
            rows.add(Arrays.asList("Task Details", ""));   // Section title
            rows.add(Arrays.asList("Title", task.getTitle()));
            rows.add(Arrays.asList("Description", task.getDescription()));
            rows.add(Arrays.asList("Status", task.getStatus()));
            rows.add(Arrays.asList("Priority", task.getPriority()));
            rows.add(Arrays.asList("Due Date", task.getFormattedDeadline()));
            rows.add(Arrays.asList("Time Spent", task.getFormattedTimeSpent()));
            rows.add(Arrays.asList("")); // Empty line

            // SECTION: LOGS
            rows.add(Arrays.asList("Logs", ""));   // Section title
            if (logs.isEmpty()) {
                rows.add(Arrays.asList("No Logs Available", ""));
            } else {
                for (int i = 0; i < logs.size(); i++) {
                    TaskLog log = logs.get(i);
                    rows.add(Arrays.asList("Log #" + (i + 1), ""));
                    rows.add(Arrays.asList("Start Time", DateTimeUtils.formatDateTime(log.getTimestamp())));
                    rows.add(Arrays.asList("Duration", log.getDuration() + " minutes"));
                    rows.add(Arrays.asList("")); // spacing between logs
                }
            }

            // SECTION: ANALYTICS
            rows.add(Arrays.asList("Analytics", ""));
            rows.add(Arrays.asList("Total Logs", String.valueOf(logs.size())));
            rows.add(Arrays.asList("Total Time Spent", task.getFormattedTimeSpent()));
            rows.add(Arrays.asList(""));

            // SECTION: REMINDERS
            rows.add(Arrays.asList("Reminders", ""));
            if (task.getReminders() != null) {
                for (int i = 0; i < task.getReminders().size(); i++) {
                    rows.add(Arrays.asList(
                            "Reminder " + (i + 1),
                            DateTimeUtils.formatDateTime(task.getReminders().get(i).getReminderTime())
                    ));
                } 
            } else {
                rows.add(Arrays.asList("No Reminders", ""));
            }

            Uri uri = CSVExporter.exportToCSV(
                    getApplicationContext(),
                    "Task_Export_" + task.getId(),
                    headers,
                    rows
            );

            if (uri != null) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/csv");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Share Task CSV"));
            }
        }).start();
    }


    private void navigateToEditTask() {
        if (task == null) {
            Toast.makeText(this, "Task data is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, TaskActivity.class);
        intent.putExtra("task_data", task);
        intent.putExtra("is_edit", true);
        startActivity(intent);
    }


    private void fadeOutAndDelete() {
        View rootView = findViewById(R.id.TaskDetailsRoot);
        rootView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> deleteTask(task))
                .start();
    }

    private void deleteTask(Task task) {
        if (task == null) {
            Toast.makeText(this, "Task data is invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.delete(task);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from edit
        if (getIntent() != null && getIntent().hasExtra("task_id")) {
            long taskId = getIntent().getLongExtra("task_id", -1L);
            if (taskId != -1L) {
                loadTaskFromDb(taskId);
            }
        }
    }
}
