package com.twa.taskmaster.ui.analytics;

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
import com.google.android.material.chip.ChipGroup;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.TimePeriod;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.domain.AnalyticsEngine;
import com.twa.taskmaster.domain.model.Task;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class InsightsFragment extends Fragment {

    private ChipGroup timePeriodChipGroup;
    private TextView tasksCreatedCompletedText, completionRateText;
    private PieChart completionRatePieChart, priorityPieChart;
    private BarChart productiveHourBarChart, productiveDayBarChart, timeSpentBarChart;
    private TaskRepository taskRepository;
    private TaskLogRepository taskLogRepository;
    private Integer colorOnSurface;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_insights, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskRepository = new TaskRepository(requireActivity().getApplication());
        taskLogRepository = new TaskLogRepository(requireActivity().getApplication());
        colorOnSurface = getThemeColor(com.google.android.material.R.attr.colorOnSurface);

        bindViews(view);
        setupCharts();

        // Default: Today
        updateAnalytics(TimePeriod.TODAY);

        timePeriodChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int chipId = checkedIds.get(0);
                TimePeriod period = TimePeriod.TODAY; // default

                if (chipId == R.id.chipToday) period = TimePeriod.TODAY;
                else if (chipId == R.id.chipWeek) period = TimePeriod.WEEK;
                else if (chipId == R.id.chipMonth) period = TimePeriod.MONTH;
                else if (chipId == R.id.chipYear) period = TimePeriod.YEAR;
                else if (chipId == R.id.chipAll) period = TimePeriod.ALL;

                updateAnalytics(period);
            }
        });

    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }


    private void bindViews(View view) {
        timePeriodChipGroup = view.findViewById(R.id.timePeriodChipGroup);
        tasksCreatedCompletedText = view.findViewById(R.id.tasksCreatedCompletedText);
        completionRateText = view.findViewById(R.id.completionRateText);
        completionRatePieChart = view.findViewById(R.id.completionRatePieChart);
        priorityPieChart = view.findViewById(R.id.priorityPieChart);
        productiveHourBarChart = view.findViewById(R.id.productiveHourBarChart);
        productiveDayBarChart = view.findViewById(R.id.productiveDayBarChart);
        timeSpentBarChart = view.findViewById(R.id.timeSpentBarChart);

    }

    private void setupCharts() {

        // ---------- Productive Day Bar Chart ----------
        XAxis xDay = productiveDayBarChart.getXAxis();
        xDay.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        xDay.setPosition(XAxis.XAxisPosition.BOTTOM);
        xDay.setTextColor(colorOnSurface);
        xDay.setGranularity(1f);
        xDay.setDrawGridLines(false);

        productiveDayBarChart.getAxisLeft().setTextColor(colorOnSurface);
        productiveDayBarChart.getAxisRight().setEnabled(false);
        productiveDayBarChart.getDescription().setEnabled(false);
        productiveDayBarChart.getLegend().setTextColor(colorOnSurface);
        productiveDayBarChart.setDrawGridBackground(false);


        // ---------- Productive Hour Bar Chart ----------
        XAxis xHour = productiveHourBarChart.getXAxis();
        xHour.setPosition(XAxis.XAxisPosition.BOTTOM);
        xHour.setTextColor(colorOnSurface);
        xHour.setGranularity(1f);
        xHour.setDrawGridLines(false);
        xHour.setValueFormatter(new IndexAxisValueFormatter(new String[]{
                "Morning", "Afternoon", "Evening", "Night"
        }));

        productiveHourBarChart.getAxisLeft().setTextColor(colorOnSurface);
        productiveHourBarChart.getAxisRight().setEnabled(false);
        productiveHourBarChart.getDescription().setEnabled(false);
        productiveHourBarChart.getLegend().setTextColor(colorOnSurface);
        productiveHourBarChart.setDrawGridBackground(false);


        // ---------- Time Spent Bar Chart ----------
        XAxis xTime = timeSpentBarChart.getXAxis();
        xTime.setPosition(XAxis.XAxisPosition.BOTTOM);
        xTime.setTextColor(colorOnSurface);
        xTime.setGranularity(1f);
        xTime.setDrawGridLines(false);

        timeSpentBarChart.getAxisLeft().setTextColor(colorOnSurface);
        timeSpentBarChart.getAxisRight().setEnabled(false);
        timeSpentBarChart.getDescription().setEnabled(false);
        timeSpentBarChart.getLegend().setTextColor(colorOnSurface);
        timeSpentBarChart.setDrawGridBackground(false);


        // pie charts
        completionRatePieChart.getLegend().setTextColor(colorOnSurface);
        completionRatePieChart.getDescription().setEnabled(false);
        completionRatePieChart.getLegend().setEnabled(true);
        completionRatePieChart.setHoleColor(Color.TRANSPARENT);
        completionRatePieChart.setDrawEntryLabels(false);

        priorityPieChart.getLegend().setEnabled(true);
        priorityPieChart.setHoleColor(Color.TRANSPARENT);
        priorityPieChart.setDrawEntryLabels(false);
        priorityPieChart.getLegend().setTextColor(colorOnSurface);
        priorityPieChart.getDescription().setEnabled(false);

    }


    private void updateAnalytics(TimePeriod period) {
        Calendar cal = Calendar.getInstance();
        long endDate = cal.getTimeInMillis();

        long startDate;
        if (period == TimePeriod.ALL) {
            startDate = 0;
        } else {
            cal.add(Calendar.DATE, -period.getDays());
            startDate = cal.getTimeInMillis();
        }

        // Tasks
        taskRepository.getTasksForPeriod(startDate, endDate)
                .observe(getViewLifecycleOwner(), tasks -> {
                    if (tasks == null || tasks.isEmpty()) {
                        showEmptyState();
                        return;
                    }
                    updateOverview(tasks);
                    updatePriorityDistribution(tasks);
                    updateProductiveHours(tasks);
                    updateTimeSpent(tasks);
                });

        // Logs
        taskLogRepository.getLogsForPeriod(period)
                .observe(getViewLifecycleOwner(), logs -> {
                    if (logs != null && !logs.isEmpty()) {
                        updateProductiveDays(logs);
                    }
                });
    }

    // -------- Overview --------
    private void updateOverview(List<Task> tasks) {
        int created = tasks.size();
        int completed = 0;
        for (Task t : tasks) {
            if (t.isCompleted()) completed++;
        }

        tasksCreatedCompletedText.setText(completed + "/" + created);
        float rate = AnalyticsEngine.calculateCompletionRate(tasks);
        completionRateText.setText(String.format("%.1f%%", rate));

        List<PieEntry> entries = AnalyticsEngine.getCompletionRateData(tasks);

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextColor(colorOnSurface);
        set.setValueTextSize(12f);
        completionRatePieChart.setData(new PieData(set));

        completionRatePieChart.invalidate();
    }

    private void updateProductiveHours(List<Task> tasks) {
        List<BarEntry> entries = AnalyticsEngine.calculateProductiveHoursByPeriod(tasks);

        BarDataSet set = new BarDataSet(entries, "Tasks by Hour");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextColor(colorOnSurface);

        BarData data = new BarData(set);
        data.setValueTextColor(colorOnSurface);
        productiveHourBarChart.setData(data);

        productiveHourBarChart.invalidate();
    }

    // -------- Productive Days --------
    private void updateProductiveDays(List<TaskLogEntity> logs) {
        List<BarEntry> entries = AnalyticsEngine.calculateProductiveDays(logs);

        BarDataSet set = new BarDataSet(entries, "Productive Hours by Day");
        set.setColors(ColorTemplate.COLORFUL_COLORS);
        set.setValueTextColor(colorOnSurface);

        BarData data = new BarData(set);
        data.setBarWidth(0.9f);
        data.setValueTextColor(colorOnSurface);

        productiveDayBarChart.setData(data);

        productiveDayBarChart.getData().notifyDataChanged();
        productiveDayBarChart.notifyDataSetChanged();
        productiveDayBarChart.invalidate();
    }

    // -------- Priority Distribution --------
    private void updatePriorityDistribution(List<Task> tasks) {
        List<PieEntry> entries = AnalyticsEngine.calculatePriorityDistribution(tasks);

        PieDataSet set = new PieDataSet(entries, "Priority");
        set.setColors(ColorTemplate.COLORFUL_COLORS);
        set.setValueTextColor(colorOnSurface);
        set.setValueTextSize(12f);

        PieData data = new PieData(set);
        data.setValueTextColor(colorOnSurface);
        priorityPieChart.setData(data);

        priorityPieChart.invalidate();
    }


    // -------- Time Spent --------
    private void updateTimeSpent(List<Task> tasks) {
        // By default showing category distribution for time spent in this view
        List<BarEntry> entries = AnalyticsEngine.calculateTaskTimeSpentByCategory(tasks);
        List<String> labels = AnalyticsEngine.getCategoryLabels(tasks);

        BarDataSet set = new BarDataSet(entries, "Time Spent by Category (Hours)");
        set.setColors(ColorTemplate.JOYFUL_COLORS);
        set.setValueTextColor(colorOnSurface);

        BarData data = new BarData(set);
        data.setValueTextColor(colorOnSurface);

        timeSpentBarChart.setData(data);


        if (!labels.isEmpty()) {
            timeSpentBarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            timeSpentBarChart.getXAxis().setGranularity(1f);
        }

        timeSpentBarChart.invalidate();
    }

    private void showEmptyState() {
        tasksCreatedCompletedText.setText("0/0");
        completionRateText.setText("0%");
        completionRatePieChart.clear();
        priorityPieChart.clear();
        productiveHourBarChart.clear();
        productiveDayBarChart.clear();
        timeSpentBarChart.clear();
    }
}
