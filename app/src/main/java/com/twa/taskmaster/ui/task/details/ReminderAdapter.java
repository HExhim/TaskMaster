package com.twa.taskmaster.ui.task.details;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.twa.taskmaster.R;
import com.twa.taskmaster.domain.model.Reminder;
import com.twa.taskmaster.core.util.DateTimeUtils;

import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private final List<Reminder> reminders;
    private final OnReminderListener onReminderListener;

    public interface OnReminderListener {
        void onDeleteReminder(Reminder reminder);
    }

    public ReminderAdapter(List<Reminder> reminders, OnReminderListener onReminderListener) {
        this.reminders = reminders;
        this.onReminderListener = onReminderListener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view, onReminderListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        holder.bind(reminder);
    }

    @Override
    public int getItemCount() {
        return reminders != null ? reminders.size() : 0;
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        TextView textReminderTime;
        ImageButton buttonDeleteReminder;

        private Reminder reminder;

        public ReminderViewHolder(@NonNull View itemView, OnReminderListener onReminderListener) {
            super(itemView);

            textReminderTime = itemView.findViewById(R.id.textReminderTime);
            buttonDeleteReminder = itemView.findViewById(R.id.buttonDeleteReminder);

            buttonDeleteReminder.setOnClickListener(v -> {
                if (reminder != null) {
                    new AlertDialog.Builder(itemView.getContext())
                        .setTitle("Delete Reminder")
                        .setMessage("Are you sure you want to delete this reminder?")
                        .setPositiveButton("Delete", (dialog, which) -> onReminderListener.onDeleteReminder(reminder))
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            });
        }

        void bind(Reminder reminder) {
            this.reminder = reminder;
            textReminderTime.setText(DateTimeUtils.formatDateTime(reminder.getReminderTime()));
            boolean isPassed = reminder.getReminderTime() <= System.currentTimeMillis();
            if (isPassed) {
                textReminderTime.setTextColor(itemView.getContext().getResources().getColor(R.color.red_800));
            }
        }
    }
}
