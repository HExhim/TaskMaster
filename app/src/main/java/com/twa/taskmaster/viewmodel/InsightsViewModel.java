package com.twa.taskmaster.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.twa.taskmaster.core.enums.TimePeriod;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.domain.model.DailyProductivity;
import com.twa.taskmaster.domain.model.ProductivityData;
import com.twa.taskmaster.domain.model.ProductivityTrendData;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.domain.model.TimeSpentData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class InsightsViewModel extends ViewModel {

    private final TaskRepository repository;

    private final MutableLiveData<TimePeriod> selectedTimePeriod = new MutableLiveData<>(TimePeriod.WEEK);

    private final MediatorLiveData<ProductivityData> productivityData = new MediatorLiveData<>();
    private final MediatorLiveData<TimeSpentData> timeSpentData = new MediatorLiveData<>();
    private final MediatorLiveData<ProductivityTrendData> productivityTrends = new MediatorLiveData<>();

    // Advanced analytics
    private final MediatorLiveData<Integer> currentStreak = new MediatorLiveData<>();
    private final MediatorLiveData<Integer> maxStreak = new MediatorLiveData<>();
    private final MediatorLiveData<Float> avgCompletionTimeMinutes = new MediatorLiveData<>();
    private final MediatorLiveData<List<Integer>> productiveHourData = new MediatorLiveData<>();
    private final MediatorLiveData<List<Integer>> productiveDayData = new MediatorLiveData<>();
    private final MediatorLiveData<List<Integer>> priorityDistributionData = new MediatorLiveData<>();
    private final MediatorLiveData<ProductivityTrendData> creationCompletionTrendData = new MediatorLiveData<>();
    private final MediatorLiveData<Float> overdueRate = new MediatorLiveData<>();

    public InsightsViewModel(TaskRepository repository) {
        this.repository = repository;
        observeRepositoryData();
    }

    public void setTimePeriod(TimePeriod period) {
        selectedTimePeriod.setValue(period);
        observeRepositoryData();
    }

    private void observeRepositoryData() {
        TimePeriod period = selectedTimePeriod.getValue() != null ? selectedTimePeriod.getValue() : TimePeriod.WEEK;
        Calendar cal = Calendar.getInstance();
        long endDate = cal.getTimeInMillis();
        cal.add(Calendar.DATE, -period.getDays());
        long startDate = cal.getTimeInMillis();

        LiveData<List<Task>> tasksLiveData = repository.getTasksForPeriod(startDate, endDate);

        productivityData.addSource(tasksLiveData, this::computeProductivityData);
        timeSpentData.addSource(tasksLiveData, this::computeTimeSpentData);
        productivityTrends.addSource(tasksLiveData, this::computeProductivityTrendData);
        currentStreak.addSource(tasksLiveData, tasks -> computeStreaks(tasks, false));
        maxStreak.addSource(tasksLiveData, tasks -> computeStreaks(tasks, true));
        avgCompletionTimeMinutes.addSource(tasksLiveData, this::computeAvgCompletionTime);
        productiveHourData.addSource(tasksLiveData, this::computeProductiveHourData);
        productiveDayData.addSource(tasksLiveData, this::computeProductiveDayData);
        priorityDistributionData.addSource(tasksLiveData, this::computePriorityDistribution);
        creationCompletionTrendData.addSource(tasksLiveData, this::computeCreationCompletionTrend);
        overdueRate.addSource(tasksLiveData, this::computeOverdueRate);
    }

    private boolean isTaskOverdue(Task task) {
        return task.getDeadline() > 0 && task.getDeadline() < System.currentTimeMillis() && !task.isCompleted();
    }

    private void computeProductivityData(List<Task> tasks) {
        if (tasks == null) return;
        int tasksCreated = tasks.size();
        int tasksCompleted = (int) tasks.stream().filter(Task::isCompleted).count();
        int overdueTasks = (int) tasks.stream().filter(this::isTaskOverdue).count();
        float completionRate = tasksCreated > 0 ? (tasksCompleted * 1f / tasksCreated) : 0f;
        productivityData.setValue(new ProductivityData(currentDate(), tasksCompleted, tasksCreated, overdueTasks, completionRate));
    }

    private void computeTimeSpentData(List<Task> tasks) {
        float workHours = 0, personalHours = 0, otherHours = 0;
        if (tasks == null) return;
        for (Task task : tasks) {
            String category = task.getCategory() != null ? task.getCategory() : "Other";
            float minutes = task.getTimeSpent() / (1000f * 60f);
            switch (category) {
                case "Work": workHours += minutes / 60f; break;
                case "Personal": personalHours += minutes / 60f; break;
                default: otherHours += minutes / 60f; break;
            }
        }
        timeSpentData.setValue(new TimeSpentData(workHours, personalHours, otherHours));
    }

    private void computeProductivityTrendData(List<Task> tasks) {
        if (tasks == null) return;
        Map<String, DailyProductivity> dailyMap = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (Task task : tasks) {
            String day = sdf.format(new Date(task.getCompletedAt() != 0 ? task.getCompletedAt() : task.getCreatedAt()));
            DailyProductivity dp = dailyMap.getOrDefault(day, new DailyProductivity(day, 0, 0, 0));
            if (task.isCompleted()) dp.setTasksCompleted(dp.getTasksCompleted() + 1);
            if (isTaskOverdue(task)) dp.setOverdueTasks(dp.getOverdueTasks() + 1);
            dp.setTasksCreated(dp.getTasksCreated() + 1);
            dailyMap.put(day, dp);
        }
        List<DailyProductivity> trends = new ArrayList<>(dailyMap.values());
        productivityTrends.setValue(new ProductivityTrendData(trends));
    }

    private void computeAvgCompletionTime(List<Task> tasks) {
        if (tasks == null) return;
        long totalMinutes = 0;
        int count = 0;
        for (Task task : tasks) {
            if (task.isCompleted() && task.getCreatedAt() != 0 && task.getCompletedAt() != 0) {
                long diff = task.getCompletedAt() - task.getCreatedAt();
                totalMinutes += diff / (60 * 1000);
                count++;
            }
        }
        avgCompletionTimeMinutes.setValue(count > 0 ? (totalMinutes * 1f / count) : 0f);
    }

    private void computeStreaks(List<Task> tasks, boolean calculateMaxStreak) {
        if (tasks == null) return;

        Set<String> completedDays = new HashSet<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (Task task : tasks) {
            if (task.isCompleted() && task.getCompletedAt() != 0) {
                completedDays.add(sdf.format(new Date(task.getCompletedAt())));
            }
        }

        List<String> days = new ArrayList<>(completedDays);
        Collections.sort(days);

        int max = 0, current = 0;
        String prev = null;

        for (String day : days) {
            if (prev == null) {
                current = 1;
            } else {
                try {
                    Date prevDate = sdf.parse(prev);
                    Date currDate = sdf.parse(day);
                    long diff = (currDate.getTime() - prevDate.getTime()) / (1000 * 60 * 60 * 24);
                    current = (diff == 1) ? current + 1 : 1;
                } catch (Exception e) {
                    current = 1;
                }
            }
            if (current > max) max = current;
            prev = day;
        }

        if (calculateMaxStreak) {
            maxStreak.setValue(max);
        } else {
            currentStreak.setValue(current);
        }
    }

    private void computeProductiveHourData(List<Task> tasks) {
        if (tasks == null) return;
        int[] hours = new int[24];
        for (Task task : tasks) {
            if (task.isCompleted() && task.getCompletedAt() != 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date(task.getCompletedAt()));
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hours[hour]++;
            }
        }
        List<Integer> hourList = new ArrayList<>();
        for (int h : hours) hourList.add(h);
        productiveHourData.setValue(hourList);
    }

    private void computeProductiveDayData(List<Task> tasks) {
        if (tasks == null) return;
        int[] days = new int[7]; // 0=Sunday
        for (Task task : tasks) {
            if (task.isCompleted() && task.getCompletedAt() != 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date(task.getCompletedAt()));
                int day = cal.get(Calendar.DAY_OF_WEEK) - 1; // Sunday=0
                days[day]++;
            }
        }
        List<Integer> dayList = new ArrayList<>();
        for (int d : days) dayList.add(d);
        productiveDayData.setValue(dayList);
    }

    private void computePriorityDistribution(List<Task> tasks) {
        if (tasks == null) return;
        int[] priorities = new int[3]; // 0=High, 1=Medium, 2=Low
        for (Task task : tasks) {
            String priority = task.getPriority() != null ? task.getPriority() : "Low";
            switch (priority) {
                case "High": priorities[0]++; break;
                case "Medium": priorities[1]++; break;
                default: priorities[2]++; break;
            }
        }
        List<Integer> priorityList = new ArrayList<>();
        for (int p : priorities) priorityList.add(p);
        priorityDistributionData.setValue(priorityList);
    }

    private void computeCreationCompletionTrend(List<Task> tasks) {
        if (tasks == null) return;
        Map<String, DailyProductivity> dailyMap = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (Task task : tasks) {
            String day = sdf.format(new Date(task.getCreatedAt()));
            DailyProductivity dp = dailyMap.getOrDefault(day, new DailyProductivity(day, 0, 0, 0));
            dp.setTasksCreated(dp.getTasksCreated() + 1);
            if (task.isCompleted()) dp.setTasksCompleted(dp.getTasksCompleted() + 1);
            dailyMap.put(day, dp);
        }
        List<DailyProductivity> trends = new ArrayList<>(dailyMap.values());
        creationCompletionTrendData.setValue(new ProductivityTrendData(trends));
    }

    private void computeOverdueRate(List<Task> tasks) {
        if (tasks == null) return;
        int overdue = (int) tasks.stream().filter(this::isTaskOverdue).count();
        float rate = tasks.size() > 0 ? (overdue * 1f / tasks.size()) : 0f;
        overdueRate.setValue(rate);
    }

    private String currentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // --- Getters for LiveData ---

    public LiveData<ProductivityData> getProductivityData() { return productivityData; }
    public LiveData<TimeSpentData> getTimeSpentData() { return timeSpentData; }
    public LiveData<ProductivityTrendData> getProductivityTrends() { return productivityTrends; }
    public LiveData<Integer> getCurrentStreak() { return currentStreak; }
    public LiveData<Integer> getMaxStreak() { return maxStreak; }
    public LiveData<Float> getAvgCompletionTimeMinutes() { return avgCompletionTimeMinutes; }
    public LiveData<List<Integer>> getProductiveHourData() { return productiveHourData; }
    public LiveData<List<Integer>> getProductiveDayData() { return productiveDayData; }
    public LiveData<List<Integer>> getPriorityDistributionData() { return priorityDistributionData; }
    public LiveData<ProductivityTrendData> getCreationCompletionTrendData() { return creationCompletionTrendData; }
    public LiveData<Float> getOverdueRate() { return overdueRate; }
}
