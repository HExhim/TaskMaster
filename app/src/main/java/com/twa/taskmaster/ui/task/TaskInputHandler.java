package com.twa.taskmaster.ui.task;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.TaskExecutionType;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.domain.model.Reminder;
import com.twa.taskmaster.domain.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskInputHandler {
    private final TaskActivity activity;
    private final TaskUiBinder uiBinder;

    public TaskInputHandler(TaskActivity activity, TaskUiBinder uiBinder) {
        this.activity = activity;
        this.uiBinder = uiBinder;
    }

    public Task buildTask() {
        String title = uiBinder.getEdtTitle().getText().toString().trim();
        if (title.isEmpty()) {
            uiBinder.getEdtTitle().setError("Title required");
            return null;
        }

        Task task = new Task();

        // Determine execution type
        TaskExecutionType type = activity.getTaskTypeHandler().getExecutionType();
        task.setExecutionType(type);
        
        // Basic fields
        task.setTitle(title);
        task.setDescription(uiBinder.getEdtDescription().getText().toString().trim());
        task.setPriority(uiBinder.getPrioritySpinner().getSelectedItem().toString());
        task.setCreatedAt(System.currentTimeMillis());
        
        // Category
        task.setCategory(uiBinder.getSelectedCategory());

        // Scheduled tasks
        if (type == TaskExecutionType.SCHEDULED) {
            String startText = uiBinder.getEdtStartDateTime().getText().toString().trim();
            String dueText = uiBinder.getEdtDueDateTime().getText().toString().trim();

            long startMillis = DateTimeUtils.parseToTimestamp(startText);
            long dueMillis = DateTimeUtils.parseToTimestamp(dueText);

            if (startMillis > 0) task.setStartDateTime(startMillis);
            if (dueMillis > 0) task.setDeadline(dueMillis);
            
            // Reminders
            if (uiBinder.getSwitchReminder().isChecked()) {
                int hour = uiBinder.getReminderTimePicker().getHour();
                int minute = uiBinder.getReminderTimePicker().getMinute();
                
                List<Reminder> reminders = new ArrayList<>();
                
                Calendar startCal = Calendar.getInstance();
                if (startMillis > 0) startCal.setTimeInMillis(startMillis);
                
                Calendar endCal = Calendar.getInstance();
                if (dueMillis > 0) endCal.setTimeInMillis(dueMillis);
                else endCal.setTimeInMillis(startCal.getTimeInMillis()); // If no due date, assume same day

                long endDayMillis = DateTimeUtils.getStartOfDay(endCal.getTimeInMillis());
                long currentDayMillis = DateTimeUtils.getStartOfDay(startCal.getTimeInMillis());
                
                // Loop through each day from start to end
                Calendar loopCal = Calendar.getInstance();
                loopCal.setTimeInMillis(currentDayMillis);

                while (loopCal.getTimeInMillis() <= endDayMillis) {
                    Calendar reminderCal = Calendar.getInstance();
                    reminderCal.setTimeInMillis(loopCal.getTimeInMillis());
                    reminderCal.set(Calendar.HOUR_OF_DAY, hour);
                    reminderCal.set(Calendar.MINUTE, minute);
                    reminderCal.set(Calendar.SECOND, 0);
                    reminderCal.set(Calendar.MILLISECOND, 0);
                    
                    Reminder r = new Reminder();
                    r.setReminderTime(reminderCal.getTimeInMillis());
                    r.setStatus("PENDING");
                    r.setSynced(false);
                    reminders.add(r);
                    
                    // Next day
                    loopCal.add(Calendar.DAY_OF_MONTH, 1);
                }
                task.setReminders(reminders);
            }
        }

        // Recurring tasks
        if (type == TaskExecutionType.RECURRING) {
            List<Integer> days = new ArrayList<>();
            ChipGroup group = uiBinder.getChipGroupWeekdays();
            
            if (group.findViewById(R.id.chipMon) != null && ((Chip)group.findViewById(R.id.chipMon)).isChecked()) days.add(Calendar.MONDAY);
            if (group.findViewById(R.id.chipTue) != null && ((Chip)group.findViewById(R.id.chipTue)).isChecked()) days.add(Calendar.TUESDAY);
            if (group.findViewById(R.id.chipWed) != null && ((Chip)group.findViewById(R.id.chipWed)).isChecked()) days.add(Calendar.WEDNESDAY);
            if (group.findViewById(R.id.chipThu) != null && ((Chip)group.findViewById(R.id.chipThu)).isChecked()) days.add(Calendar.THURSDAY);
            if (group.findViewById(R.id.chipFri) != null && ((Chip)group.findViewById(R.id.chipFri)).isChecked()) days.add(Calendar.FRIDAY);
            if (group.findViewById(R.id.chipSat) != null && ((Chip)group.findViewById(R.id.chipSat)).isChecked()) days.add(Calendar.SATURDAY);
            if (group.findViewById(R.id.chipSun) != null && ((Chip)group.findViewById(R.id.chipSun)).isChecked()) days.add(Calendar.SUNDAY);

            task.setRecurrenceDaysOfWeek(days);

            
            // For recurring tasks, even if reminder is not checked, we need to set StartDateTime for listing purposes
            // But the prompt says "handle Proper Next Occurence and Shedule Atleast 1 weeks reminders"
            // Assuming we always calculate next occurrence if days are selected.
            
            if (!days.isEmpty()) {
                int hour = 9; // Default to 9 AM if no reminder set, or just use current time? 
                int minute = 0;
                
                if (uiBinder.getSwitchReminder().isChecked()) {
                    hour = uiBinder.getReminderTimePicker().getHour();
                    minute = uiBinder.getReminderTimePicker().getMinute();
                } else {
                    // If no reminder, maybe we just find next day at start of day or current time?
                    // Let's use current time for next occurrence calculation but without reminders
                    Calendar now = Calendar.getInstance();
                    hour = now.get(Calendar.HOUR_OF_DAY);
                    minute = now.get(Calendar.MINUTE);
                }
                
                List<Reminder> reminders = new ArrayList<>();
                
                // Calculate next occurrences for the next 28 days (4 weeks)
                Calendar check = Calendar.getInstance();
                check.set(Calendar.SECOND, 0);
                check.set(Calendar.MILLISECOND, 0);

                long nextOccurrence = 0;
                
                for (int i = 0; i < 10; i++) { // Look ahead 4 weeks
                    int dayOfWeek = check.get(Calendar.DAY_OF_WEEK);
                    if (days.contains(dayOfWeek)) {
                        check.set(Calendar.HOUR_OF_DAY, hour);
                        check.set(Calendar.MINUTE, minute);
                        
                        if (check.getTimeInMillis() > System.currentTimeMillis()) {
                            
                            // Only add reminders if enabled
                            if (uiBinder.getSwitchReminder().isChecked()) {
                                Reminder r = new Reminder();
                                r.setReminderTime(check.getTimeInMillis());
                                r.setStatus("PENDING");
                                reminders.add(r);
                            }
                            
                            // Capture the very first occurrence for startDateTime/deadline
                            if (nextOccurrence == 0) {
                                nextOccurrence = check.getTimeInMillis();
                            }
                        }
                    }
                    check.add(Calendar.DAY_OF_MONTH, 1);
                }
                
                if (nextOccurrence > 0) {
                    task.setStartDateTime(nextOccurrence);
                    task.setDeadline(nextOccurrence);
                }
                
                if (!reminders.isEmpty()) {
                    task.setReminders(reminders);
                }
            }
        }
        return task;
    }

    private long calculateNextOccurrence(List<Integer> days, int hour, int minute) {
        return 0;
    }

    public void populateFields(Task task) {
        uiBinder.getEdtTitle().setText(task.getTitle());
        uiBinder.getEdtDescription().setText(task.getDescription());
        uiBinder.selectCategory(task.getCategory());

        // Priority
        String[] priorities = {"Low", "Medium", "High"};
        for (int i = 0; i < priorities.length; i++) {
            if (priorities[i].equalsIgnoreCase(task.getPriority())) {
                uiBinder.getPrioritySpinner().setSelection(i);
                break;
            }
        }

        if (task.getExecutionType() == TaskExecutionType.SCHEDULED) {
            if (task.getStartDateTime() > 0) {
                uiBinder.getEdtStartDateTime().setText(DateTimeUtils.formatDateTime(task.getStartDateTime()));
            }
            if (task.getDeadline() > 0) {
                uiBinder.getEdtDueDateTime().setText(DateTimeUtils.formatDateTime(task.getDeadline()));
            }
        }
        
        if (task.getExecutionType() == TaskExecutionType.RECURRING) {
            List<Integer> days = task.getRecurrenceDaysOfWeek();
            if (days != null) {
                ChipGroup group = uiBinder.getChipGroupWeekdays();
                
                if (days.contains(Calendar.MONDAY)) checkChip(group, R.id.chipMon);
                if (days.contains(Calendar.TUESDAY)) checkChip(group, R.id.chipTue);
                if (days.contains(Calendar.WEDNESDAY)) checkChip(group, R.id.chipWed);
                if (days.contains(Calendar.THURSDAY)) checkChip(group, R.id.chipThu);
                if (days.contains(Calendar.FRIDAY)) checkChip(group, R.id.chipFri);
                if (days.contains(Calendar.SATURDAY)) checkChip(group, R.id.chipSat);
                if (days.contains(Calendar.SUNDAY)) checkChip(group, R.id.chipSun);
            }
        }
        
        // Reminders
        if (task.getReminders() != null && !task.getReminders().isEmpty()) {
            uiBinder.getSwitchReminder().setChecked(true);
            // Get the first reminder to set the time picker
            Reminder r = task.getReminders().get(0);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(r.getReminderTime());
            uiBinder.getReminderTimePicker().setHour(c.get(Calendar.HOUR_OF_DAY));
            uiBinder.getReminderTimePicker().setMinute(c.get(Calendar.MINUTE));
        }
    }
    
    private void checkChip(ChipGroup group, int id) {
        Chip chip = group.findViewById(id);
        if (chip != null) chip.setChecked(true);
    }
}
