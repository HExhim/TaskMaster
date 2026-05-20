package com.twa.taskmaster.domain;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.twa.taskmaster.core.enums.TimePeriod;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.domain.model.DailyProductivity;
import com.twa.taskmaster.domain.model.ProductivityTrendData;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.domain.model.TimeSpentData;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Central Analytics Engine for calculating statistics and generating chart data.
 * This class abstracts the logic for processing raw Task and TaskLog data into
 * consumable formats for UI components (Charts, TextViews, etc).
 */
public class AnalyticsEngine {

    // --- Task Specific Analytics (for TaskAnalyticsFragment) ---

    public static List<BarEntry> calculateTaskTimeSpentByDay(List<TaskLogEntity> logs, int daysBack) {
        Map<Long, Long> timeByDay = new HashMap<>();
        LocalDate today = LocalDate.now();

        // Initialize map with 0 for the last 'daysBack' days
        for (int i = 0; i < daysBack; i++) {
            LocalDate date = today.minusDays(i);
            long epochDay = date.toEpochDay();
            timeByDay.put(epochDay, 0L);
        }

        for (TaskLogEntity log : logs) {
            LocalDate date = Instant.ofEpochMilli(log.getTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            long epochDay = date.toEpochDay();
            
            if (timeByDay.containsKey(epochDay)) {
                long durationMillis = TimeUnit.MINUTES.toMillis(log.getDurationMinutes());
                timeByDay.put(epochDay, timeByDay.get(epochDay) + durationMillis);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = daysBack - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long epochDay = date.toEpochDay();
            long timeMillis = timeByDay.getOrDefault(epochDay, 0L);
            float hours = (float) timeMillis / (1000 * 60 * 60);
            entries.add(new BarEntry(daysBack - 1 - i, hours)); // x-axis: 0 is oldest, daysBack-1 is today
        }
        return entries;
    }

    public static List<Entry> calculateTaskProgressOverTime(List<TaskLogEntity> logs) {
       List<Entry> entries = new ArrayList<>();
       if (logs == null || logs.isEmpty()) return entries;

       // Sort logs by timestamp
       List<TaskLogEntity> sortedLogs = new ArrayList<>(logs);
       Collections.sort(sortedLogs, (l1, l2) -> Long.compare(l1.getTimestamp(), l2.getTimestamp()));

       long cumulativeTime = 0;
       long startTime = sortedLogs.get(0).getTimestamp();
       
       for (int i = 0; i < sortedLogs.size(); i++) {
           TaskLogEntity log = sortedLogs.get(i);
           cumulativeTime += TimeUnit.MINUTES.toMillis(log.getDurationMinutes());
           float hours = (float) cumulativeTime / (1000 * 60 * 60);
           // x-axis could be index or relative time. Here using index for simplicity of "sessions"
           entries.add(new Entry(i, hours));
       }
       return entries;
    }

    public static List<BarEntry> calculateWeeklyConsistency(List<TaskLogEntity> logs) {
        float[] hoursByDay = new float[7]; // 0=Mon, 6=Sun
        
        for (TaskLogEntity log : logs) {
             LocalDate date = Instant.ofEpochMilli(log.getTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
             DayOfWeek dayOfWeek = date.getDayOfWeek();
             int index = dayOfWeek.getValue() - 1; // 1 (Mon) -> 0
             
             hoursByDay[index] += (float) log.getDurationMinutes() / 60f;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, hoursByDay[i]));
        }
        return entries;
    }
    
    public static int calculateCurrentStreak(List<TaskLogEntity> logs) {
        if (logs == null || logs.isEmpty()) return 0;

        // Get unique days with activity, sorted descending
        List<Long> uniqueDays = new ArrayList<>();
        for (TaskLogEntity log : logs) {
            LocalDate date = Instant.ofEpochMilli(log.getTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            long epochDay = date.toEpochDay();
            if (!uniqueDays.contains(epochDay)) {
                uniqueDays.add(epochDay);
            }
        }
        Collections.sort(uniqueDays, Collections.reverseOrder());

        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();
        
        int streak = 0;
        
        // Check if today or yesterday has activity to start the streak
        if (uniqueDays.isEmpty()) return 0;
        
        long lastActiveDay = uniqueDays.get(0);
        if (lastActiveDay != todayEpoch && lastActiveDay != todayEpoch - 1) {
            return 0; // Streak broken
        }

        long currentDayToCheck = lastActiveDay;
        for (Long activeDay : uniqueDays) {
            if (activeDay == currentDayToCheck) {
                streak++;
                currentDayToCheck--;
            } else {
                break;
            }
        }
        return streak;
    }
    
    public static int calculateBestStreak(List<TaskLogEntity> logs) {
         if (logs == null || logs.isEmpty()) return 0;

        List<Long> uniqueDays = new ArrayList<>();
        for (TaskLogEntity log : logs) {
            LocalDate date = Instant.ofEpochMilli(log.getTimestamp())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            long epochDay = date.toEpochDay();
            if (!uniqueDays.contains(epochDay)) {
                uniqueDays.add(epochDay);
            }
        }
        Collections.sort(uniqueDays); // Ascending

        if (uniqueDays.isEmpty()) return 0;
        
        int maxStreak = 1;
        int currentStreak = 1;
        
        for (int i = 0; i < uniqueDays.size() - 1; i++) {
            if (uniqueDays.get(i + 1) == uniqueDays.get(i) + 1) {
                currentStreak++;
            } else {
                maxStreak = Math.max(maxStreak, currentStreak);
                currentStreak = 1;
            }
        }
        maxStreak = Math.max(maxStreak, currentStreak);
        
        return maxStreak;
    }

    public static List<PieEntry> calculateSourceDistribution(List<TaskLogEntity> logs) {
        Map<String, Float> sourceMap = new HashMap<>();
        sourceMap.put("Pomodoro", 0f);
        sourceMap.put("Stopwatch", 0f);
        sourceMap.put("Manual", 0f);

        if (logs != null) {
            for (TaskLogEntity log : logs) {
                String source = log.getSource() != null ? log.getSource() : "Manual";
                // Normalize source names if needed, assuming typical values
                if (source.toLowerCase().contains("pomodoro")) {
                    sourceMap.put("Pomodoro", sourceMap.get("Pomodoro") + 1);
                } else if (source.toLowerCase().contains("stopwatch")) {
                    sourceMap.put("Stopwatch", sourceMap.get("Stopwatch") + 1);
                } else {
                    sourceMap.put("Manual", sourceMap.get("Manual") + 1);
                }
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        if (sourceMap.get("Pomodoro") > 0) entries.add(new PieEntry(sourceMap.get("Pomodoro"), "Pomodoro"));
        if (sourceMap.get("Stopwatch") > 0) entries.add(new PieEntry(sourceMap.get("Stopwatch"), "Stopwatch"));
        if (sourceMap.get("Manual") > 0) entries.add(new PieEntry(sourceMap.get("Manual"), "Manual"));
        
        return entries;
    }
    
    public static int calculateTotalSessions(List<TaskLogEntity> logs) {
        return logs != null ? logs.size() : 0;
    }
    
    public static float calculateAverageSessionDuration(List<TaskLogEntity> logs) {
        if (logs == null || logs.isEmpty()) return 0f;
        long totalMinutes = 0;
        for (TaskLogEntity log : logs) {
            totalMinutes += log.getDurationMinutes();
        }
        return (float) totalMinutes / logs.size();
    }


    // --- Overall Insights Analytics (for InsightsFragment) ---

    public static float calculateCompletionRate(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return 0f;
        int completed = 0;
        for (Task t : tasks) {
            if (t.isCompleted()) completed++;
        }
        return (float) completed / tasks.size() * 100f;
    }

    public static List<PieEntry> getCompletionRateData(List<Task> tasks) {
        int completed = 0;
        int pending = 0;
        if (tasks != null) {
            for (Task t : tasks) {
                if (t.isCompleted()) completed++;
                else pending++;
            }
        }
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(completed, "Completed"));
        entries.add(new PieEntry(pending, "Pending"));
        return entries;
    }

    public static List<BarEntry> calculateProductiveHours(List<Task> tasks) {
        int[] hours = new int[24];
        Calendar cal = Calendar.getInstance();

        if (tasks != null) {
            for (Task t : tasks) {
                long refTime = t.isCompleted()
                        ? t.getCompletedAt()
                        : t.getCreatedAt();

                if (refTime > 0) {
                    cal.setTimeInMillis(refTime);
                    hours[cal.get(Calendar.HOUR_OF_DAY)]++;
                }
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            entries.add(new BarEntry(i, hours[i]));
        }
        return entries;
    }
    
    public static List<BarEntry> calculateProductiveHoursByPeriod(List<Task> tasks) {
        // 0: Morning (5-11)
        // 1: Afternoon (12-16)
        // 2: Evening (17-22)
        // 3: Night (23-4)
        int[] periods = new int[4];
        Calendar cal = Calendar.getInstance();

        if (tasks != null) {
            for (Task t : tasks) {
                long refTime = t.isCompleted() ? t.getCompletedAt() : t.getCreatedAt();

                if (refTime > 0) {
                    cal.setTimeInMillis(refTime);
                    int hour = cal.get(Calendar.HOUR_OF_DAY);
                    
                    if (hour >= 5 && hour < 12) {
                        periods[0]++; // Morning
                    } else if (hour >= 12 && hour < 17) {
                        periods[1]++; // Afternoon
                    } else if (hour >= 17 && hour < 23) {
                        periods[2]++; // Evening
                    } else {
                        periods[3]++; // Night
                    }
                }
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            entries.add(new BarEntry(i, periods[i]));
        }
        return entries;
    }
    
    public static List<BarEntry> calculateProductiveDays(List<TaskLogEntity> logs) {
        float[] timeSpentByDay = new float[7]; // 0=Mon
        
        if (logs != null) {
            for (TaskLogEntity log : logs) {
                 LocalDate date = Instant.ofEpochMilli(log.getTimestamp())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                 DayOfWeek dayOfWeek = date.getDayOfWeek();
                 int index = dayOfWeek.getValue() - 1; // 1 (Mon) -> 0
                 timeSpentByDay[index] += (float) log.getDurationMinutes() / 60f;
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, timeSpentByDay[i], days[i]));
        }
        return entries;
    }

    public static List<PieEntry> calculatePriorityDistribution(List<Task> tasks) {
        int high = 0, medium = 0, low = 0;
        if (tasks != null) {
            for (Task t : tasks) {
                String p = t.getPriority();
                if ("High".equalsIgnoreCase(p)) high++;
                else if ("Medium".equalsIgnoreCase(p)) medium++;
                else if ("Low".equalsIgnoreCase(p)) low++;
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        if (high > 0) entries.add(new PieEntry(high, "High"));
        if (medium > 0) entries.add(new PieEntry(medium, "Medium"));
        if (low > 0) entries.add(new PieEntry(low, "Low"));
        return entries;
    }

    public static Map<String, List<Entry>> calculateCreationVsCompletionTrends(List<Task> tasks) {
        // Group by day
        Map<Long, Integer> createdByDay = new HashMap<>();
        Map<Long, Integer> completedByDay = new HashMap<>();
        
        LocalDate minDate = LocalDate.now();
        LocalDate maxDate = LocalDate.now();

        if (tasks != null) {
             for (Task t : tasks) {
                 LocalDate cDate = Instant.ofEpochMilli(t.getCreatedAt()).atZone(ZoneId.systemDefault()).toLocalDate();
                 long cEpoch = cDate.toEpochDay();
                 createdByDay.put(cEpoch, createdByDay.getOrDefault(cEpoch, 0) + 1);
                 
                 if (cDate.isBefore(minDate)) minDate = cDate;

                 if (t.isCompleted() && t.getCompletedAt() > 0) {
                     LocalDate compDate = Instant.ofEpochMilli(t.getCompletedAt()).atZone(ZoneId.systemDefault()).toLocalDate();
                     long compEpoch = compDate.toEpochDay();
                     completedByDay.put(compEpoch, completedByDay.getOrDefault(compEpoch, 0) + 1);
                 }
             }
        }
        
        List<Entry> createdEntries = new ArrayList<>();
        List<Entry> completedEntries = new ArrayList<>();
        
        long start = minDate.toEpochDay();
        long end = maxDate.toEpochDay();
        
        int index = 0;
        for (long day = start; day <= end; day++) {
            createdEntries.add(new Entry(index, createdByDay.getOrDefault(day, 0)));
            completedEntries.add(new Entry(index, completedByDay.getOrDefault(day, 0)));
            index++;
        }

        Map<String, List<Entry>> result = new HashMap<>();
        result.put("Created", createdEntries);
        result.put("Completed", completedEntries);
        return result;
    }
    
    public static List<BarEntry> calculateTimeSpentByTask(List<Task> tasks) {
        List<BarEntry> entries = new ArrayList<>();
        if (tasks != null) {
            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                long timeSpentMillis = t.getTimeSpent();
                float minutes = (float) timeSpentMillis / (1000 * 60);
                entries.add(new BarEntry(i, minutes));
            }
        }
        return entries;
    }

    public static List<BarEntry> calculateTaskTimeSpentByPriority(List<Task> tasks) {
        Map<String, Float> timeByPriority = new HashMap<>();
        timeByPriority.put("High", 0f);
        timeByPriority.put("Medium", 0f);
        timeByPriority.put("Low", 0f);

        if (tasks != null) {
            for (Task t : tasks) {
                String priority = t.getPriority() != null ? t.getPriority() : "Low";
                float hours = (float) t.getTimeSpent() / (1000 * 60 * 60);
                timeByPriority.put(priority, timeByPriority.getOrDefault(priority, 0f) + hours);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, timeByPriority.get("High")));
        entries.add(new BarEntry(1, timeByPriority.get("Medium")));
        entries.add(new BarEntry(2, timeByPriority.get("Low")));
        return entries;
    }

    public static List<BarEntry> calculateTaskTimeSpentByCategory(List<Task> tasks) {
        Map<String, Float> timeByCategory = new HashMap<>();
        List<String> categories = new ArrayList<>();

        if (tasks != null) {
            for (Task t : tasks) {
                String category = t.getCategory() != null ? t.getCategory() : "Uncategorized";
                if (!timeByCategory.containsKey(category)) {
                    categories.add(category);
                    timeByCategory.put(category, 0f);
                }
                float hours = (float) t.getTimeSpent() / (1000 * 60 * 60);
                timeByCategory.put(category, timeByCategory.get(category) + hours);
            }
        }
        
        // Sort categories alphabetically or by time? Let's just use insertion order or alphabetical
        Collections.sort(categories);

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            entries.add(new BarEntry(i, timeByCategory.get(categories.get(i))));
        }
        return entries;
    }
    
    public static List<String> getCategoryLabels(List<Task> tasks) {
         List<String> categories = new ArrayList<>();
         List<String> seen = new ArrayList<>();
         if (tasks != null) {
             for (Task t : tasks) {
                String category = t.getCategory() != null ? t.getCategory() : "Uncategorized";
                if (!seen.contains(category)) {
                    seen.add(category);
                }
             }
         }
         Collections.sort(seen);
         return seen;
    }

}