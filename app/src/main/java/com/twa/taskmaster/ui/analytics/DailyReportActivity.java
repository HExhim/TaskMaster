package com.twa.taskmaster.ui.analytics;

import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.TimePeriod;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.domain.AnalyticsEngine;
import com.twa.taskmaster.domain.model.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyReportActivity extends AppCompatActivity {

    private TextView tvDate, tvProductivityScore, tvTasksCompleted, tvTotalTime, tvCompletionRate, tvMostProductive;
    private BarChart chartHourlyActivity;
    private PieChart chartPriority;
    private Button btnClose;

    private TaskRepository taskRepository;
    private TaskLogRepository taskLogRepository;
    private Integer colorOnSurface;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_report);

        taskRepository = new TaskRepository(getApplication());
        taskLogRepository = new TaskLogRepository(getApplication());
        
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        colorOnSurface = typedValue.data;

        bindViews();
        setupCharts();
        loadData();
    }

    private void bindViews() {
        tvDate = findViewById(R.id.tvDate);
        tvProductivityScore = findViewById(R.id.tvProductivityScore);
        tvTasksCompleted = findViewById(R.id.tvTasksCompleted);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        tvMostProductive = findViewById(R.id.tvMostProductive);
        chartHourlyActivity = findViewById(R.id.chartHourlyActivity);
        chartPriority = findViewById(R.id.chartPriority);
        btnClose = findViewById(R.id.btnClose);
        
        btnClose.setOnClickListener(v -> finish());
    }

    private void setupCharts() {
        // Hourly Chart
        chartHourlyActivity.getDescription().setEnabled(false);
        chartHourlyActivity.setDrawGridBackground(false);
        chartHourlyActivity.getAxisRight().setEnabled(false);
        chartHourlyActivity.getLegend().setEnabled(false);
        
        XAxis xAxis = chartHourlyActivity.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(colorOnSurface);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{
                "Morn", "Aft", "Eve", "Night"
        }));

        chartHourlyActivity.getAxisLeft().setTextColor(colorOnSurface);

        // Priority Chart
        chartPriority.getDescription().setEnabled(false);
        chartPriority.setDrawEntryLabels(false);
        chartPriority.setHoleColor(android.graphics.Color.TRANSPARENT);
        chartPriority.getLegend().setTextColor(colorOnSurface);
    }

    private void loadData() {
        // Set Date
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));

        // Calculate start/end of today
        Calendar cal = Calendar.getInstance();
        long endDate = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startDate = cal.getTimeInMillis();

        // Fetch Data
        taskRepository.getTasksForPeriod(startDate, endDate).observe(this, tasks -> {
            if (tasks != null) {
                updateTaskMetrics(tasks);
                updateCharts(tasks);
            }
        });

        taskLogRepository.getLogsForPeriod(TimePeriod.TODAY).observe(this, logs -> {
            if (logs != null) {
                updateLogMetrics(logs);
            }
        });
    }

    private void updateTaskMetrics(List<Task> tasks) {
        int completed = 0;
        for (Task t : tasks) {
            if (t.isCompleted()) completed++;
        }
        tvTasksCompleted.setText(String.valueOf(completed));

        float completionRate = AnalyticsEngine.calculateCompletionRate(tasks);
        tvCompletionRate.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));

        // Simple productivity score logic
        if (completionRate > 80) tvProductivityScore.setText("Productive Day! 🚀");
        else if (completionRate > 50) tvProductivityScore.setText("Good Progress 👍");
        else tvProductivityScore.setText("Keep Going 💪");
    }

    private void updateLogMetrics(List<TaskLogEntity> logs) {
        long totalMinutes = 0;
        for (TaskLogEntity log : logs) {
            totalMinutes += log.getDurationMinutes();
        }
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        tvTotalTime.setText(String.format(Locale.getDefault(), "%dh %dm", hours, minutes));
        
        // Most productive hour (simplified)
        // Would need more logic in AnalyticsEngine to get "Best Hour" from logs specifically
        // For now, placeholder or reuse existing logic if applicable
    }

    private void updateCharts(List<Task> tasks) {
        // Hourly Activity
        List<BarEntry> hourlyEntries = AnalyticsEngine.calculateProductiveHoursByPeriod(tasks);
        BarDataSet hourlySet = new BarDataSet(hourlyEntries, "Activity");
        hourlySet.setColors(ColorTemplate.MATERIAL_COLORS);
        hourlySet.setValueTextColor(colorOnSurface);
        
        BarData hourlyData = new BarData(hourlySet);
        hourlyData.setValueTextColor(colorOnSurface);
        chartHourlyActivity.setData(hourlyData);
        chartHourlyActivity.invalidate();

        // Priority
        List<PieEntry> priorityEntries = AnalyticsEngine.calculatePriorityDistribution(tasks);
        PieDataSet prioritySet = new PieDataSet(priorityEntries, "");
        prioritySet.setColors(ColorTemplate.COLORFUL_COLORS);
        prioritySet.setValueTextColor(colorOnSurface);
        
        PieData priorityData = new PieData(prioritySet);
        chartPriority.setData(priorityData);
        chartPriority.invalidate();
    }
}
