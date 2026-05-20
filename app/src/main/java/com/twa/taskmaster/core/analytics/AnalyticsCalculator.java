package com.twa.taskmaster.core.analytics;

import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.domain.model.TaskLog;
import com.twa.taskmaster.core.util.DateTimeUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AnalyticsCalculator {

    public static class Report {
        public String title;
        public String dateRange;

        public long totalTimeSpentMillis;
        public int totalTasks;
        public int completedTasks;
        public float completionRate;

        public int avgSessionMinutes;
        public int longestSessionMinutes;
        public int shortestSessionMinutes;

        public int avgTasksPerDay;
        public int streakDays;
        public int maxStreakDays;
        public int activeDays;

        public Map<String, Long> timeSpentPerCategory = new HashMap<>();
        public Map<String, Long> timeSpentPerDay = new TreeMap<>();
        public Map<String, Integer> tasksPerPriority = new HashMap<>();
        public Map<String, Integer> taskCountPerDay = new TreeMap<>();
        public Map<String, Integer> taskStartsByHour = new HashMap<>();
    }

    public static Report generate(List<Task> tasks, List<TaskLog> logs, String title, String dateRange) {
        Report report = new Report();
        report.title = title;
        report.dateRange = dateRange;

        long totalTime = 0;
        int totalSessions = 0;
        int longest = 0;
        int shortest = Integer.MAX_VALUE;

        Set<String> activeDaySet = new HashSet<>();

        for (Task task : tasks) {
            if (task.isCompleted()) report.completedTasks++;
            report.totalTasks++;
            String category = task.getCategory();
            long time = task.getTimeSpent();
            totalTime += time;

            report.timeSpentPerCategory.put(
                    category,
                    report.timeSpentPerCategory.getOrDefault(category, 0L) + time);

            String pr = task.getPriority();
            report.tasksPerPriority.put(pr, report.tasksPerPriority.getOrDefault(pr, 0) + 1);
        }

        for (TaskLog log : logs) {
            long minutes = log.getDuration();
            totalSessions++;
            longest = Math.max(longest, (int) minutes);
            shortest = Math.min(shortest, (int) minutes);

            String day = DateTimeUtils.formatDate(log.getTimestamp());
            report.timeSpentPerDay.put(day,
                    report.timeSpentPerDay.getOrDefault(day, 0L) + TimeUnit.MINUTES.toMillis(minutes));

            report.taskCountPerDay.put(day,
                    report.taskCountPerDay.getOrDefault(day, 0) + 1);

            activeDaySet.add(day);

            // Start hour
            int hour = DateTimeUtils.getHour(log.getTimestamp());
            String hourKey = hour + ":00";
            report.taskStartsByHour.put(hourKey,
                    report.taskStartsByHour.getOrDefault(hourKey, 0) + 1);
        }

        report.totalTimeSpentMillis = totalTime;
        report.completionRate = report.totalTasks == 0 ? 0 : (float) report.completedTasks / report.totalTasks;
        report.avgSessionMinutes = totalSessions == 0 ? 0 : (int) (totalTime / 60000) / totalSessions;
        report.longestSessionMinutes = longest;
        report.shortestSessionMinutes = shortest == Integer.MAX_VALUE ? 0 : shortest;

        report.activeDays = activeDaySet.size();
        report.avgTasksPerDay = report.activeDays == 0 ? 0 : report.totalTasks / report.activeDays;

        computeStreaks(report, report.taskCountPerDay);
        return report;
    }

    private static void computeStreaks(Report report, Map<String, Integer> dailyTaskMap) {
        List<String> days = new ArrayList<>(dailyTaskMap.keySet());
        days.sort(String::compareTo);

        int currentStreak = 0;
        int maxStreak = 0;
        String prevDate = null;

        for (String date : days) {
            if (prevDate == null) {
                currentStreak = 1;
            } else {
                int diff = DateTimeUtils.daysBetween(prevDate, date);
                if (diff == 1) {
                    currentStreak++;
                } else if (diff > 1) {
                    currentStreak = 1;
                }
            }
            maxStreak = Math.max(maxStreak, currentStreak);
            prevDate = date;
        }

        report.streakDays = currentStreak;
        report.maxStreakDays = maxStreak;
    }
}
