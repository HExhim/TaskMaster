package com.twa.taskmaster.ui.task.logs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.twa.taskmaster.R;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LogsAdapter extends ListAdapter<TaskLogEntity, LogsAdapter.LogViewHolder> {

    private final Set<Integer> selectedItems = new HashSet<>();
    private final OnLogActionListener listener;
    private final Context context;
    private boolean isMultiSelectMode = false;
    
    public interface OnLogActionListener {
        void onLogLongClick(TaskLogEntity tasklog);
        void onLogClick(TaskLogEntity taskLog);
    }
    

    public LogsAdapter(Context context, OnLogActionListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void setMultiSelectMode(boolean enabled) {
        this.isMultiSelectMode = enabled;
        notifyDataSetChanged();
    }
    public boolean isMultiSelectModeEnabled(){
        return isMultiSelectMode;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        TaskLogEntity log = getItem(position);
        holder.bind(log, selectedItems.contains(log.getId()), isMultiSelectMode, listener);
    }

    public void toggleSelection(int logId) {
        if (selectedItems.contains(logId)) {
            selectedItems.remove(logId);
        } else {
            selectedItems.add(logId);
        }
        notifyDataSetChanged();
    }
    

    public void clearSelection() {
        isMultiSelectMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public List<Long> getSelectedItems() {
        List<Long> result = new ArrayList<>();
        for (Integer id : selectedItems) {
            result.add((long) id);
        }
        return result;
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView logDuration, logDate, logTimeRange, logNotes, logSource;
        private final LinearLayout card;


        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            logDuration = itemView.findViewById(R.id.logDuration);
            logDate = itemView.findViewById(R.id.logDate);
            logTimeRange = itemView.findViewById(R.id.logTimeRange);
            logNotes = itemView.findViewById(R.id.logNotes);
            logSource = itemView.findViewById(R.id.logSource);
            card = itemView.findViewById(R.id.cardRoot);
        }

        void bind(TaskLogEntity log, boolean isSelected, boolean isMultiSelectMode, OnLogActionListener listener) {
            logDuration.setText(formatDuration(log.getDurationMinutes()));

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            logDate.setText(dateFormat.format(new Date(log.getTimestamp())));

            if (log.getEndTimeMillis() > 0) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                logTimeRange.setText(timeFormat.format(new Date(log.getTimestamp())) +
                        " - " +
                        timeFormat.format(new Date(log.getEndTimeMillis())));
            } else {
                logTimeRange.setText("");
            }


            if (log.getNote() != null) {
                logNotes.setText("Note: " +log.getNote());
            } else {
                logNotes.setText("Note: N/A");
            }
            if (log.getSource() != null) {
                logSource.setText("Source: " + log.getSource());
            }
            else logSource.setText("Source: N/A");
            
            itemView.setOnClickListener(v -> {
                if (isMultiSelectMode) {
                    if (listener != null) {
                        listener.onLogLongClick(log);
                    }
                } else {
                    if (listener != null) {
                        listener.onLogClick(log);
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onLogLongClick(log);
                }
                return true; // Consume the long click
            });

            if (isSelected) {
                card.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.selection_highlight));
            } else {
                card.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }
        }

        private String formatDuration(int minutes) {
            if (minutes < 60) return minutes + "m";
            return (minutes / 60) + "h " + (minutes % 60) + "m";
        }
    }
    private static final DiffUtil.ItemCallback<TaskLogEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskLogEntity o, @NonNull TaskLogEntity n) {
            return o.getId() == n.getId();
        }
        @Override
        public boolean areContentsTheSame(@NonNull TaskLogEntity o, @NonNull TaskLogEntity n) {
            return o.equals(n);
        }
    };
}
