package com.twa.taskmaster.ui.task.details;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.TaskExecutionType;
import com.twa.taskmaster.core.reminder.ReminderScheduler;
import com.twa.taskmaster.data.local.database.Database;
import com.twa.taskmaster.data.repository.ReminderRepository;
import com.twa.taskmaster.domain.mapper.ReminderMapper;
import com.twa.taskmaster.domain.model.Reminder;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.core.util.AppExecutors;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.domain.mapper.TaskMapper;
import com.twa.taskmaster.viewmodel.SharedTaskViewModel;
import com.twa.taskmaster.viewmodel.TaskViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class TaskDetailsFragment extends Fragment implements ReminderAdapter.OnReminderListener {

    private TextView  textDescription,noReminderText,textCreatedAt, textStartedAt, textDeadline;
    private Chip chipPriority, chipTag, chipCategory;
    private MaterialButton buttonMarkComplete, buttonDeleteTask;
    private FloatingActionButton fabAddReminder;
    private RecyclerView recyclerReminders;
    private ReminderAdapter reminderAdapter;
    private ReminderRepository reminderRepository;

    private Task task;
    private SharedTaskViewModel sharedViewModel;
    private TaskViewModel taskViewModel;

    public TaskDetailsFragment() {
        // Required empty public constructor
    }

    public static TaskDetailsFragment newInstance() {
        return new TaskDetailsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reminderRepository = new ReminderRepository(requireActivity().getApplication());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_details, container, false);
        initializeViews(view);
        setupViewModel();
        setupButtonActions();
        return view;
    }

    private void initializeViews(View view) {

        textDescription = view.findViewById(R.id.textDescription);

        textCreatedAt = view.findViewById(R.id.textCreatedAt);
        textDeadline = view.findViewById(R.id.textDeadline);
        textStartedAt = view.findViewById(R.id.textStartedAt);
        TextView textEditedDate = view.findViewById(R.id.textEditedDate);

        chipPriority = view.findViewById(R.id.chipPriority);
        chipTag = view.findViewById(R.id.chipTag);
        chipCategory = view.findViewById(R.id.chipCategory);



        fabAddReminder = view.findViewById(R.id.fabAddReminder);
        recyclerReminders = view.findViewById(R.id.recyclerReminders);
        recyclerReminders.setLayoutManager(new LinearLayoutManager(getContext()));
        noReminderText = view.findViewById(R.id.no_Reminder_Text);



        buttonMarkComplete = view.findViewById(R.id.buttonMarkComplete);
        buttonDeleteTask = view.findViewById(R.id.buttonDeleteTask);
    }

    private void setupViewModel() {
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedTaskViewModel.class);
        sharedViewModel.getTask().observe(getViewLifecycleOwner(), this::updateUI);
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

    }

    @SuppressLint("SetTextI18n")
    private void updateUI(Task task) {
        if (task == null) {
            showToast("No task data available");
            return;
        }

        this.task = task;

        // Title and basic info
        textDescription.setText(task.getDescription());

        // Priority and Tag chips
        chipPriority.setText("Priority : " + task.getPriority());
        chipCategory.setText("Category : " + task.getCategory());

        chipTag.setText("Status : " + task.getStatus());
        chipTag.setChipIconResource(R.drawable.ic_tag);
         // Dates section
        if (task.getExecutionType() == TaskExecutionType.SCHEDULED || task.getExecutionType() == TaskExecutionType.RECURRING) {
            textStartedAt.setText(DateTimeUtils.formatDateTime(task.getStartDateTime()));
            textDeadline.setText(DateTimeUtils.formatDateTime(task.getDeadline()));
        }else {
            textStartedAt.setVisibility(GONE);
            textDeadline.setVisibility(GONE);
        }
        textCreatedAt.setText(DateTimeUtils.formatDateTime(task.getCreatedAt()));
        ((TextView) requireView().findViewById(R.id.textEditedDate)).setText(DateTimeUtils.formatDateTime(task.getUpdatedAt()));


        // Reminder section
        if (task.getReminders() != null && !task.getReminders().isEmpty()) {
            ReminderScheduler.scheduleReminders(requireContext(), task);
            Collections.sort(task.getReminders(), (r1, r2) -> Long.compare(r1.getReminderTime(), r2.getReminderTime()));
            reminderAdapter = new ReminderAdapter(task.getReminders(), this);
            recyclerReminders.setAdapter(reminderAdapter);
            noReminderText.setVisibility(GONE);
            recyclerReminders.setVisibility(VISIBLE);
        } else {
            noReminderText.setVisibility(VISIBLE);
            recyclerReminders.setVisibility(GONE);
        }

        // Update complete button state
        updateCompleteButtonState(task.isCompleted());
    }

    private void updateCompleteButtonState(boolean isCompleted) {
        if (isCompleted) {
            buttonMarkComplete.setText(getString(R.string.mark_as_incomplete));
            buttonMarkComplete.setIconResource(R.drawable.ic_undo);
        } else {
            buttonMarkComplete.setText(getString(R.string.mark_as_complete));
            buttonMarkComplete.setIconResource(R.drawable.ic_check);
        }
    }

    private void setupButtonActions() {
        buttonMarkComplete.setOnClickListener(v -> toggleTaskCompletion());
        buttonDeleteTask.setOnClickListener(v -> showDeleteConfirmationDialog());
        fabAddReminder.setOnClickListener(v -> showDateTimePicker());
    }

    private void showDateTimePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Reminder Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build();

        datePicker.addOnPositiveButtonClickListener(dateSelection -> {
            // This selection is in UTC. We need to combine it with a time.
            Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCal.setTimeInMillis(dateSelection);

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTitleText("Select Reminder Time")
                .setHour(12)
                .setMinute(0)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .build();

            timePicker.addOnPositiveButtonClickListener(timeView -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();

                // Create a calendar in the local timezone for the final time
                Calendar localCal = Calendar.getInstance();
                localCal.set(
                    utcCal.get(Calendar.YEAR),
                    utcCal.get(Calendar.MONTH),
                    utcCal.get(Calendar.DAY_OF_MONTH),
                    hour,
                    minute,
                    0
                );
                localCal.set(Calendar.MILLISECOND, 0);

                long reminderTime = localCal.getTimeInMillis();

                if (reminderTime > System.currentTimeMillis()) {
                    Reminder newReminder = new Reminder();
                    newReminder.setTaskId(task.getId());
                    newReminder.setReminderTime(reminderTime);

                    AppExecutors.getInstance().diskIO().execute(() -> {
                        long newId = Database.getInstance(requireContext()).reminderDao().insert(ReminderMapper.toEntity(newReminder));
                        newReminder.setId((int) newId);

                        List<Reminder> currentReminders = task.getReminders();
                        List<Reminder> mutableReminders;
                        if (currentReminders == null) {
                            mutableReminders = new ArrayList<>();
                        } else {
                            mutableReminders = new ArrayList<>(currentReminders);
                        }
                        mutableReminders.add(newReminder);
                        task.setReminders(mutableReminders);

                        requireActivity().runOnUiThread(() -> {
                            updateUI(task);
                            showToast("Reminder set for " + DateTimeUtils.formatDateTime(reminderTime));
                        });
                    });

                } else {
                    showToast("Please select a future time for the reminder.");
                }
            });
            timePicker.show(getParentFragmentManager(), "timePicker");
        });
        datePicker.show(getParentFragmentManager(), "datePicker");
    }

    private void toggleTaskCompletion() {
        if (task == null) return;

        boolean newCompletedState = !task.isCompleted();
        task.setCompleted(newCompletedState);
        taskViewModel.update(task);
        updateCompleteButtonState(newCompletedState);
        showToast(newCompletedState ? "Task completed!" : "Task marked incomplete");


    }

    private void showDeleteConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete")
                .setMessage("Are you sure?, You want to delete this task.")
                .setIcon(R.drawable.ic_delete)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteTask())
                .show();
    }

    private void deleteTask() {
        if (task == null) return;

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                Database.getInstance(requireContext())
                        .taskDao()
                        .delete(TaskMapper.toEntity(task));
                requireActivity().runOnUiThread(() -> {
                    showToast(getString(R.string.task_deleted_successfully));
                    requireActivity().finish();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        showToast(getString(R.string.error_deleting_task) + e.getMessage()));
            }
        });
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteReminder(Reminder reminder) {
        AppExecutors.getInstance().diskIO().execute(() -> {
           reminderRepository.delete(reminder);
            ReminderScheduler.cancelReminder(requireContext(), reminder);
            task.getReminders().remove(reminder);
            requireActivity().runOnUiThread(() -> {
                updateUI(task);
                showToast("Reminder deleted");
            });
        });
    }
}
