package com.twa.taskmaster.ui.timer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.twa.taskmaster.R;

public class LapsAdapter extends ListAdapter<Lap, LapsAdapter.LapViewHolder> {

    public LapsAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Lap> DIFF_CALLBACK = new DiffUtil.ItemCallback<Lap>() {
        @Override
        public boolean areItemsTheSame(@NonNull Lap oldItem, @NonNull Lap newItem) {
            return oldItem.getLapNumber() == newItem.getLapNumber();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Lap oldItem, @NonNull Lap newItem) {
            return oldItem.getLapTime().equals(newItem.getLapTime()) &&
                   oldItem.getTotalTime().equals(newItem.getTotalTime());
        }
    };

    @NonNull
    @Override
    public LapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.lap_item_view, parent, false);
        return new LapViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LapViewHolder holder, int position) {
        Lap currentLap = getItem(position);
        holder.lapNumber.setText("Lap " + currentLap.getLapNumber());
        holder.lapTime.setText(currentLap.getLapTime());
        holder.totalTime.setText(currentLap.getTotalTime());
    }

    static class LapViewHolder extends RecyclerView.ViewHolder {
        private final TextView lapNumber;
        private final TextView lapTime;
        private final TextView totalTime;

        public LapViewHolder(@NonNull View itemView) {
            super(itemView);
            lapNumber = itemView.findViewById(R.id.lap_number);
            lapTime = itemView.findViewById(R.id.lap_time);
            totalTime = itemView.findViewById(R.id.total_time);
        }
    }
}
