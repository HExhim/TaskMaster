package com.twa.taskmaster.ui.task.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.TimePeriod;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.domain.AnalyticsEngine;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.viewmodel.SharedTaskViewModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TaskAnalyticsFragment extends Fragment {

    private LineChart lineChartProgress;
    private BarChart barChartConsistency, barChartTimeSpent;
    private PieChart pieChartSource;
    private TextView tvCurrentStreak, tvBestStreak, tvTimeSpent, tvTotalSessions, tvAvgSessionDuration;
    private CircularProgressIndicator streakProgress;
    private SharedTaskViewModel sharedViewModel;
    private Integer colorOnSurface;

    public static TaskAnalyticsFragment newInstance() {
        return new TaskAnalyticsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedTaskViewModel.class);

        colorOnSurface = getThemeColor(com.google.android.material.R.attr.colorOnSurface);

        bindViews(view);
        setupCharts();

        sharedViewModel.getTask().observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                tvTimeSpent.setText("Total Time Spent: " + task.getFormattedTimeSpent());
            }
        });

        sharedViewModel.getLogs(TimePeriod.ALL).observe(getViewLifecycleOwner(), logs -> {
            if (logs != null) {
                updateAnalytics(logs);
            }
        });
    }
    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void bindViews(View view) {
        lineChartProgress = view.findViewById(R.id.lineChartProgress);
        barChartConsistency = view.findViewById(R.id.barChartConsistency);
        barChartTimeSpent = view.findViewById(R.id.barChartTimeSpent);
        pieChartSource = view.findViewById(R.id.pieChartSource);
        tvCurrentStreak = view.findViewById(R.id.tvCurrentStreak);
        tvBestStreak = view.findViewById(R.id.tvBestStreak);
        tvTimeSpent = view.findViewById(R.id.tvTimeSpent);
        tvTotalSessions = view.findViewById(R.id.tvTotalSessions);
        tvAvgSessionDuration = view.findViewById(R.id.tvAvgSessionDuration);
        streakProgress = view.findViewById(R.id.streakProgress);
    }

    private void setupCharts() {
        // General setup for charts (disabling descriptions, interactions etc if needed)
        // BarChart Time Spent
        barChartTimeSpent.getDescription().setEnabled(false);
        barChartTimeSpent.setDrawGridBackground(false);
        barChartTimeSpent.getAxisRight().setEnabled(false);
        
        XAxis xTimeSpent = barChartTimeSpent.getXAxis();
        xTimeSpent.setPosition(XAxis.XAxisPosition.BOTTOM);
        xTimeSpent.setDrawGridLines(false);
        xTimeSpent.setGranularity(1f);
        xTimeSpent.setTextColor(colorOnSurface);

        barChartTimeSpent.getAxisLeft().setTextColor(colorOnSurface);
        barChartTimeSpent.getLegend().setTextColor(colorOnSurface);

        // LineChart Progress
        lineChartProgress.getDescription().setEnabled(false);
        lineChartProgress.setDrawGridBackground(false);
        lineChartProgress.getAxisRight().setEnabled(false);

        XAxis xProgress = lineChartProgress.getXAxis();
        xProgress.setPosition(XAxis.XAxisPosition.BOTTOM);
        xProgress.setTextColor(colorOnSurface);
        xProgress.setGranularity(1f);
        xProgress.setGranularityEnabled(true);

        lineChartProgress.getAxisLeft().setTextColor(colorOnSurface);
        lineChartProgress.getLegend().setTextColor(colorOnSurface);

        // BarChart Consistency
        barChartConsistency.getDescription().setEnabled(false);
        barChartConsistency.setDrawGridBackground(false);
        barChartConsistency.getAxisRight().setEnabled(false);

        XAxis xConsistency = barChartConsistency.getXAxis();
        xConsistency.setPosition(XAxis.XAxisPosition.BOTTOM);
        xConsistency.setDrawGridLines(false);
        xConsistency.setGranularity(1f);
        xConsistency.setTextColor(colorOnSurface);
        
        String[] days = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        xConsistency.setValueFormatter(new IndexAxisValueFormatter(days));
        
        barChartConsistency.getAxisLeft().setTextColor(colorOnSurface);
        barChartConsistency.getLegend().setTextColor(colorOnSurface);
        
        // PieChart Source
        pieChartSource.getDescription().setEnabled(false);
        pieChartSource.setDrawEntryLabels(false);
        pieChartSource.getLegend().setTextColor(colorOnSurface);
        pieChartSource.setHoleColor(Color.TRANSPARENT);
    }

    private void updateAnalytics(List<TaskLogEntity> logs) {
        updateStreaks(logs);
        updateSessionStats(logs);
        updateSourceDistribution(logs);
        updateTimeSpentChart(logs);
        updateProgressChart(logs);
        updateConsistencyChart(logs);
    }

    private void updateStreaks(List<TaskLogEntity> logs) {
        int currentStreak = AnalyticsEngine.calculateCurrentStreak(logs);
        int bestStreak = AnalyticsEngine.calculateBestStreak(logs);

        tvCurrentStreak.setText("🔥 Current Streak: " + currentStreak + " days");
        tvBestStreak.setText("Best Streak: " + bestStreak + " days");
        
        // Assuming max streak goal is 30 for the progress bar visualization
        streakProgress.setMax(30);
        streakProgress.setProgress(Math.min(currentStreak, 30));
    }

    private void updateSessionStats(List<TaskLogEntity> logs) {
        int totalSessions = AnalyticsEngine.calculateTotalSessions(logs);
        float avgDuration = AnalyticsEngine.calculateAverageSessionDuration(logs);
        
        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvAvgSessionDuration.setText(String.format("%.0f min", avgDuration));
    }

    private void updateSourceDistribution(List<TaskLogEntity> logs) {
        List<PieEntry> entries = AnalyticsEngine.calculateSourceDistribution(logs);
        if (entries.isEmpty()) {
            pieChartSource.clear();
            return;
        }
        
        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextColor(colorOnSurface);
        set.setValueTextSize(12f);
        
        PieData data = new PieData(set);
        pieChartSource.setData(data);
        pieChartSource.invalidate();
    }

    private void updateTimeSpentChart(List<TaskLogEntity> logs) {
        // Last 7 days
        List<BarEntry> entries = AnalyticsEngine.calculateTaskTimeSpentByDay(logs, 7);
        BarDataSet set = new BarDataSet(entries, "Hours Spent (Last 7 Days)");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextColor(colorOnSurface);
        
        BarData data = new BarData(set);
        data.setValueTextColor(colorOnSurface);
        barChartTimeSpent.setData(data);
        
        // Set labels for X-axis (Past 7 days)
        LocalDate today = LocalDate.now();
        String[] labels = new String[7];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        for (int i = 0; i < 7; i++) {
            labels[i] = today.minusDays(6 - i).format(formatter);
        }
        
        XAxis xAxis = barChartTimeSpent.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        
        barChartTimeSpent.invalidate();
    }

    private void updateProgressChart(List<TaskLogEntity> logs) {
        List<Entry> entries = AnalyticsEngine.calculateTaskProgressOverTime(logs);
        if (entries.isEmpty()) {
            lineChartProgress.clear();
            return;
        }
        
        LineDataSet set = new LineDataSet(entries, "Total Hours Accumulated");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillAlpha(50);
        set.setValueTextColor(colorOnSurface);

        LineData data = new LineData(set);
        data.setValueTextColor(colorOnSurface);
        lineChartProgress.setData(data);
        
        // Simple X axis labels as "Session X"
        XAxis xAxis = lineChartProgress.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{}) {
            @Override
            public String getFormattedValue(float value) {
                return "S" + (int)value;
            }
        });
        
        lineChartProgress.invalidate();
    }

    private void updateConsistencyChart(List<TaskLogEntity> logs) {
        List<BarEntry> entries = AnalyticsEngine.calculateWeeklyConsistency(logs);
        BarDataSet set = new BarDataSet(entries, "Avg Hours per Day of Week");
        set.setColors(ColorTemplate.COLORFUL_COLORS);
        set.setValueTextColor(colorOnSurface);


        BarData data = new BarData(set);
        data.setValueTextColor(colorOnSurface);
        barChartConsistency.setData(data);
        barChartConsistency.invalidate();
    }
}
