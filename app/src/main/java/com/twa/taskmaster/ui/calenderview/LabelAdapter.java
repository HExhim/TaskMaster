package com.twa.taskmaster.ui.calenderview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LabelAdapter extends RecyclerView.Adapter<LabelAdapter.LabelViewHolder> {
    private final List<String> labels;

    public LabelAdapter(List<String> labels) {
        this.labels = labels;
    }

    @NonNull
    @Override
    public LabelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new LabelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LabelViewHolder holder, int position) {
        holder.textView.setText(labels.get(position));
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }

    static class LabelViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public LabelViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
