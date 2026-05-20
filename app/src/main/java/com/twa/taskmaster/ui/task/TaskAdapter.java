package com.twa.taskmaster.ui.task;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CheckBox;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.TaskExecutionType;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.ui.task.details.TaskDetailActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TASK_INSTANT = 1; // Existing Task type
    private static final int TYPE_TASK_SCHEDULED = 2; // New type
    private static final int TYPE_TASK_RECURRING = 3; // New type
    private final List<Object> items = new ArrayList<>();
    private final Context context;
    private final OnTaskActionListener listener;
    private boolean isMultiSelectMode = false;
    private final Set<Integer> selectedTaskIds = new HashSet<>();
    public interface OnTaskActionListener {
        void onPomodoroClick(Task task);
        void onStopwatchClick(Task task);
        // Add these new methods for selection
        void onTaskLongClick(Task task);
        void onTaskClick(Task task);
    }
    public void toggleSelection(int taskId) {
        if (selectedTaskIds.contains(taskId)) {
            selectedTaskIds.remove(taskId);
        } else {
            selectedTaskIds.add(taskId);
        }
        notifyDataSetChanged(); // Simple way to update all views
    }

    public void clearSelections() {
        isMultiSelectMode = false;
        selectedTaskIds.clear();
        notifyDataSetChanged();
    }

    public Set<Integer> getSelectedTaskIds() {
        return selectedTaskIds;
    }

    public void setMultiSelectMode(boolean enabled) {
        this.isMultiSelectMode = enabled;
    }
    public TaskAdapter(Context context, OnTaskActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    // Call this for grouped lists (header + task flattened list)
    public void setGroupedTasks(List<Object> groupedItems) {
        items.clear();
        if (groupedItems != null) {
            items.addAll(groupedItems);
        }
        notifyDataSetChanged();
    }

    // Call this for plain list of tasks (ungrouped)
    public void setTasks(List<Task> taskList) {
        items.clear();
        if (taskList != null) {
            items.addAll(taskList);
        }
        notifyDataSetChanged();
    }

    public Object getItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }


    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof String) {
            return TYPE_HEADER;
        } else if (item instanceof Task) {
            Task task = (Task) item;
            if (task.getExecutionType() == TaskExecutionType.INSTANT) {
                return TYPE_TASK_INSTANT;
            } else if (task.getExecutionType() == TaskExecutionType.SCHEDULED) {
                return TYPE_TASK_SCHEDULED;
            } else if (task.getExecutionType() == TaskExecutionType.RECURRING) {
                return TYPE_TASK_RECURRING;
            }
            // Fallback for any unknown type
            return TYPE_TASK_INSTANT;
        }
        return TYPE_TASK_INSTANT; // Default for non-header items
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_task_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == TYPE_TASK_INSTANT) {
            // Assuming item_task2 is your INSTANT task card
            View view = LayoutInflater.from(context).inflate(R.layout.item_task_instant, parent, false);
            return new TaskViewHolder(view);
        } else if (viewType == TYPE_TASK_SCHEDULED) {
            // **Requires R.layout.item_task_scheduled.xml**
            View view = LayoutInflater.from(context).inflate(R.layout.item_task_scheduled, parent, false);
            return new TaskViewHolder(view); // Use the same TaskViewHolder for common binding
        } else if (viewType == TYPE_TASK_RECURRING) {
            // **Requires R.layout.item_task_recurring.xml**
            View view = LayoutInflater.from(context).inflate(R.layout.item_task_recurring, parent, false);
            return new TaskViewHolder(view); // Use the same TaskViewHolder for common binding
        } else {
            // Fallback
            View view = LayoutInflater.from(context).inflate(R.layout.item_task_instant, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder && item instanceof String) {
            ((HeaderViewHolder) holder).bind((String) item);
        } else if (holder instanceof TaskViewHolder && item instanceof Task) {
            ((TaskViewHolder) holder).bind((Task) item);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.header_title);
        }

        void bind(String title) {
            headerTitle.setText(title);
        }
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, dueDate, timeSpent,deadlineDate;
        View priorityIndicator;
        Chip statusChip,typeChip;
        MaterialButton btnPomodoro, btnStopwatch;
        Button detailsBtn;
        TextView recurrencePattern;
        ChipGroup taskDaysChipGroup;
        CheckBox taskCheckbox;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.task_title);
            description = itemView.findViewById(R.id.task_description);
            dueDate = itemView.findViewById(R.id.task_due_date);
            deadlineDate = itemView.findViewById(R.id.task_deadline_date);
            timeSpent = itemView.findViewById(R.id.task_time_spent);
            priorityIndicator = itemView.findViewById(R.id.task_priority_indicator);
            statusChip = itemView.findViewById(R.id.task_status_chip);
            typeChip = itemView.findViewById(R.id.task_type_chip);
            btnPomodoro = itemView.findViewById(R.id.btn_pomodoro);
            btnStopwatch = itemView.findViewById(R.id.btn_stopwatch);
            detailsBtn = itemView.findViewById(R.id.btn_view_details);
            taskDaysChipGroup = itemView.findViewById(R.id.chip_group_days);
        }


        void bind(Task task) {
            title.setText(task.getTitle());
            description.setText(task.getDescription());

            statusChip.setText(task.getStatus());

            typeChip.setText((task.getExecutionType().name()));

            // Set Priority Indicator color (UNCHANGED)
            int colorRes = R.color.priority_low;
            switch (task.getPriority()) {
                case "Medium": colorRes = R.color.priority_medium; break;
                case "High": colorRes = R.color.priority_high; break;
            }
            priorityIndicator.setBackgroundColor(ContextCompat.getColor(context, colorRes));

            // --- CRITICAL VISIBILITY MANAGEMENT ---
            // Handle views that exist in some layouts but not others (using null checks)
            // The following properties must be hidden or shown appropriately for ALL 3 types.
            if (deadlineDate != null) deadlineDate.setVisibility(View.GONE); // Only used in item_task_scheduled

            if (taskDaysChipGroup != null) {
                taskDaysChipGroup.setVisibility(View.GONE);
            }
            // --- TIME / DATE LOGIC based on Execution Type ---

            if (task.getExecutionType() == TaskExecutionType.INSTANT) {
                // INSTANT Logic
                if(task.getDeadline() > 0){
                    dueDate.setText("Due: " + DateTimeUtils.formatDateTime(task.getDeadline()));
                } else {
                    dueDate.setText("No Deadline");
                }

            } else if (task.getExecutionType() == TaskExecutionType.SCHEDULED) {
                // SCHEDULED Logic
                // 1. Start Time (using existing dueDate TextView)
                if (task.getStartDateTime() > 0) {
                    dueDate.setText("Start: " + DateTimeUtils.formatDateTime(task.getStartDateTime()));
                } else {
                    dueDate.setText("Start: N/A");
                }

                // 2. End Time/Deadline (using the dedicated deadlineDate TextView)
                if (deadlineDate != null) {
                    if (task.getDeadline() > 0) {
                        deadlineDate.setText("End: " + DateTimeUtils.formatDateTime(task.getDeadline()));
                        deadlineDate.setVisibility(View.VISIBLE);
                    } else {
                        deadlineDate.setVisibility(View.GONE);
                    }
                }

            } else if (task.getExecutionType() == TaskExecutionType.RECURRING) {
                // 1. Set Next Due Date (Placeholder)
                dueDate.setText("Next: " + (task.getStartDateTime() > 0 ?
                        DateTimeUtils.formatDateTime(task.getStartDateTime()) : "N/A"));

                // 2. Handle ChipGroup Visibility and Highlighting
                List<Integer> daysOfWeek = task.getRecurrenceDaysOfWeek();

                if (taskDaysChipGroup != null && daysOfWeek != null && !daysOfWeek.isEmpty()) {
                    taskDaysChipGroup.removeAllViews();
                    taskDaysChipGroup.setVisibility(View.VISIBLE);
                    
                    for (Integer day : daysOfWeek) {
                        Chip chip = new Chip(context);
                        chip.setText(getDayName(day));
                        chip.setCheckable(false);
                        chip.setClickable(false);
                        // Make it small
                        chip.ensureAccessibleTouchTarget(24); 
                        taskDaysChipGroup.addView(chip);
                    }
                }
            }

            // Common Logic
            timeSpent.setText("Time Spent: " + task.getFormattedTimeSpent());

            btnPomodoro.setOnClickListener(v -> listener.onPomodoroClick(task));
            btnStopwatch.setOnClickListener(v -> listener.onStopwatchClick(task));
            detailsBtn.setOnClickListener(v -> {
                Intent intent = new Intent(context, TaskDetailActivity.class);
                intent.putExtra("task_data", task);
                context.startActivity(intent);
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onTaskLongClick(task);
                }
                return true; // Consume the long click
            });

            if (taskCheckbox != null) {
                if (isMultiSelectMode) {
                    taskCheckbox.setVisibility(View.VISIBLE);
                    taskCheckbox.setChecked(selectedTaskIds.contains(task.getId()));
                } else {
                    taskCheckbox.setVisibility(View.GONE);
                }
            }
            if (selectedTaskIds.contains(task.getId())) {
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.selection_highlight));
            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            }
        }
        
        private String getDayName(int day) {
            switch(day) {
                case Calendar.MONDAY: return "Mon";
                case Calendar.TUESDAY: return "Tue";
                case Calendar.WEDNESDAY: return "Wed";
                case Calendar.THURSDAY: return "Thu";
                case Calendar.FRIDAY: return "Fri";
                case Calendar.SATURDAY: return "Sat";
                case Calendar.SUNDAY: return "Sun";
                default: return "";
            }
        }
    }
}
