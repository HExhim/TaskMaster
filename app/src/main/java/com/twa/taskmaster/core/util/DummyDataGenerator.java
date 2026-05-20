package com.twa.taskmaster.core.util;

import android.os.Build;

import com.twa.taskmaster.core.enums.TaskExecutionType;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.data.repository.ReminderRepository;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.domain.model.Reminder;
import com.twa.taskmaster.domain.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

public class DummyDataGenerator {

    private static final String[] TITLES = {
            "Complete project report", "Review code changes", "Team meeting", "Client presentation",
            "Bug fixes", "UI redesign", "Database optimization", "API documentation",
            "Test automation", "Performance testing", "Unit testing",
            "Firebase Fixes", "Reviewing", "Unit testing", "Follow Up"
    };

    private static final String[] DESCRIPTIONS = {
            "Need to finish by EOD", "High priority item", "Follow up with team", "Prepare slides and demo",
            "Critical issue from production", "New design mockups", "Query optimization needed",
            "Update Swagger docs", "Write integration tests", "Load testing for new feature",
            "Optimize loading", "Finalize integration tests", "Final testing for new feature",
            "Edit Swagger docs", "Edit integration tests", "Edit testing for new feature"
    };

    private static final String[] CATEGORIES = {"Work", "Personal", "Study", "Health"};
    private static final String[] PRIORITIES = {"High", "Medium", "Low"};
    private static final String[] SOURCES = {"Manual", "Focus Session", "Pomodoro", "Stopwatch"};
    private static final String[] STATUSES = {"Pending", "In Progress", "Completed", "Overdue"};
    private static final String[] COLORS = {"#FF5733", "#33FF57", "#3357FF", "#F033FF", "#FF33F0"};

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final ReminderRepository reminderRepository;
    private final Random random = new Random();

    public DummyDataGenerator(TaskRepository taskRepository, TaskLogRepository taskLogRepository,ReminderRepository reminderRepository) {
        this.taskRepository = taskRepository;
        this.taskLogRepository = taskLogRepository;
        this.reminderRepository = reminderRepository;
    }

    public void createAndInsertDummyTasks(int count) {
        taskRepository.deleteAlltask();
        taskLogRepository.deleteAllLogs();
        reminderRepository.deleteAllReminders();
        long now = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            Task task = new Task();
            task.setId(0); // Room auto-generates

            task.setTitle(TITLES[random.nextInt(TITLES.length)]);
            task.setDescription(DESCRIPTIONS[random.nextInt(DESCRIPTIONS.length)]);
            task.setCategory(CATEGORIES[random.nextInt(CATEGORIES.length)]);
            task.setPriority(PRIORITIES[random.nextInt(PRIORITIES.length)]);

            long createdAt = now - TimeUnit.DAYS.toMillis(random.nextInt(30));
            task.setCreatedAt(createdAt);

            // Pick random TaskExecutionType
            TaskExecutionType[] types = TaskExecutionType.values();
            TaskExecutionType type = types[random.nextInt(types.length)];
            task.setExecutionType(type);


            long startDateTime = createdAt + TimeUnit.HOURS.toMillis(random.nextInt(6));
            long deadline = 0;

            switch (type) {
                case INSTANT:
                    task.setStartDateTime(startDateTime);
                    task.setDeadline(0);

                    break;

                case SCHEDULED:
                    deadline = startDateTime + TimeUnit.DAYS.toMillis(1 + random.nextInt(30));
                    task.setStartDateTime(startDateTime);
                    task.setDeadline(deadline);

                    break;

                case RECURRING:
                    deadline = startDateTime + TimeUnit.DAYS.toMillis(7 + random.nextInt(30));
                    task.setStartDateTime(startDateTime);
                    // generate 3 non-duplicate days of week
                    List<Integer> daysOfWeek = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        daysOfWeek = RandomGenerator.getDefault().ints(1, 5).limit(3).boxed().toList();
                    }
                    // Remove Duplicates
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        daysOfWeek = daysOfWeek.stream().distinct().toList();
                    }

                    task.setRecurrenceDaysOfWeek(daysOfWeek);
                    task.setDeadline(deadline);
                    break;
            }


            long updatedAt = startDateTime + TimeUnit.HOURS.toMillis(random.nextInt(24));
            task.setUpdatedAt(updatedAt);

            boolean isCompleted = random.nextBoolean();
            task.setCompleted(isCompleted);
            
            // Insert task, get ID
            long taskId = taskRepository.insertTask(task);
            task.setId((int) taskId);

            generateRandomReminder(task);

            generateDummyLogs(task, createdAt, now);

            taskRepository.update(task);
        }
    }

    private void generateRandomReminder(Task task) {
        Random random = new Random();
        int reminderCount = random.nextInt(3) + 1;
        List<Reminder> reminders = new ArrayList<>();
        for (int i = 0; i < reminderCount; i++) {
            Reminder reminder = new Reminder();
            reminder.setTaskId(task.getId());
            reminder.setReminderTime(Calendar.getInstance().getTimeInMillis() + TimeUnit.DAYS.toMillis(random.nextInt(10)));
            reminders.add(reminder);
        }
        task.setReminders(reminders);
    }

    private void generateDummyLogs(Task task, long createdAt, long now) {
        Random random = new Random();
        int logCount = random.nextInt(8) + 4;
        long totalTimeSpent = 0;

        // --- Ensure at least 1 log is in "today" ---
        long todayStart = getStartOfDay(System.currentTimeMillis());
        long todayLogTime = todayStart + TimeUnit.HOURS.toMillis(random.nextInt(12)); // sometime today

        TaskLogEntity todayLog = new TaskLogEntity();
        todayLog.setTaskId(task.getId());
        todayLog.setTaskName(task.getTitle());
        todayLog.setTimestamp(todayLogTime);
        todayLog.setDurationMinutes(30 + random.nextInt(61)); // 30–90 mins
        todayLog.setEndTimeMillis(todayLog.getTimestamp() + TimeUnit.MINUTES.toMillis(todayLog.getDurationMinutes()));
        List<String> sources = List.of("Manual", "Pomodoro", "Stopwatch");
        todayLog.setSource((sources.get(random.nextInt(sources.size()))).toString());
        todayLog.setNote("Worked on " + task.getTitle().toLowerCase() + " today");
        taskLogRepository.insertLog(todayLog);

        totalTimeSpent += TimeUnit.MINUTES.toMillis(todayLog.getDurationMinutes());

        // --- Generate the rest randomly (past 30 days) ---
        for (int j = 1; j < logCount; j++) {
            TaskLogEntity logEntity = new TaskLogEntity();
            logEntity.setTaskId(task.getId());
            logEntity.setTaskName(task.getTitle());

            long logTime;
            if (createdAt >= now) {
                logTime = now - TimeUnit.MINUTES.toMillis(1);
            } else {
                logTime = ThreadLocalRandom.current().nextLong(createdAt, now);
            }

            int duration = 5 + random.nextInt(116);
            long endTimeMillis = logTime + TimeUnit.MINUTES.toMillis(duration);

            logEntity.setTimestamp(logTime);
            logEntity.setDurationMinutes(duration);
            logEntity.setEndTimeMillis(endTimeMillis);
            logEntity.setSource("Manual");

            taskLogRepository.insertLog(logEntity);

            totalTimeSpent += TimeUnit.MINUTES.toMillis(duration);
            task.setTimeSpent(totalTimeSpent);
        }

        // Update task total time
        task.setTimeSpent(totalTimeSpent);
    }

    private long getStartOfDay(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

}
