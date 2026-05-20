package com.twa.taskmaster.ui.calenderview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.twa.taskmaster.R;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.domain.model.Task;

import java.util.List;

public class TaskCalendarAdapter extends RecyclerView.Adapter<TaskCalendarAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final Context context;
    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public TaskCalendarAdapter(Context context, List<Task> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textTime, textCategory, textPriority, textStatus;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textTime = itemView.findViewById(R.id.textTime);
            textCategory = itemView.findViewById(R.id.textCategory);
            textPriority = itemView.findViewById(R.id.textPriority);

        }

        public void bind(final Task task, final OnTaskClickListener listener) {
            textTitle.setText(task.getTitle());

            // Format time (e.g., "3:00 PM - 4:30 PM")
            String duration = DateTimeUtils.formatDateTime(task.getCreatedAt()) + " - " + DateTimeUtils.formatDateTime(task.getDeadline());

            textTime.setText(duration);

            textCategory.setText(task.getCategory());

            // Priority badge styling
            String priority = task.getPriority(); // "LOW", "MEDIUM", "HIGH"
            textPriority.setText(priority.toUpperCase());

            switch (priority.toLowerCase()) {
                case "high":
                    textPriority.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.red_800));
                    break;
                case "medium":
                    textPriority.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.orange_800));
                    break;
                case "low":
                default:
                    textPriority.setBackgroundTintList(ContextCompat.getColorStateList(itemView.getContext(), R.color.green_800));
                    break;
            }

            // Click listener
            itemView.setOnClickListener(v -> listener.onTaskClick(task));
        }
    }
}

