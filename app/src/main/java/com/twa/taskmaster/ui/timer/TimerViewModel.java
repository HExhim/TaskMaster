package com.twa.taskmaster.ui.timer;

import android.app.Application;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.domain.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimerViewModel extends AndroidViewModel {

    public enum TimerMode {
        POMODORO,
        STOPWATCH
    }

    public enum PomodoroState {
        WORK,
        SHORT_BREAK,
        LONG_BREAK
    }

    private static final long POMODORO_WORK_DURATION_MS = TimeUnit.MINUTES.toMillis(25);
    private static final long POMODORO_SHORT_BREAK_DURATION_MS = TimeUnit.MINUTES.toMillis(5);
    private static final long POMODORO_LONG_BREAK_DURATION_MS = TimeUnit.MINUTES.toMillis(15);
    private static final int POMODORO_SESSIONS_UNTIL_LONG_BREAK = 4;

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final MutableLiveData<Task> _task = new MutableLiveData<>();
    public LiveData<Task> task = _task;

    private final MutableLiveData<String> _timeDisplay = new MutableLiveData<>();
    public LiveData<String> timeDisplay = _timeDisplay;

    private final MutableLiveData<Boolean> _isTimerRunning = new MutableLiveData<>(false);
    public LiveData<Boolean> isTimerRunning = _isTimerRunning;

    private final MutableLiveData<PomodoroState> _pomodoroState = new MutableLiveData<>(PomodoroState.WORK);
    public LiveData<PomodoroState> pomodoroState = _pomodoroState;

    private final MutableLiveData<Integer> _pomodoroSessionCount = new MutableLiveData<>(0);
    public LiveData<Integer> pomodoroSessionCount = _pomodoroSessionCount;

    private final MutableLiveData<Boolean> _showResetButton = new MutableLiveData<>(false);
    public LiveData<Boolean> showResetButton = _showResetButton;

    private final MutableLiveData<Boolean> _showSkipButton = new MutableLiveData<>(false);
    public LiveData<Boolean> showSkipButton = _showSkipButton;

    private final MutableLiveData<Boolean> _showLapButton = new MutableLiveData<>(false);
    public LiveData<Boolean> showLapButton = _showLapButton;

    private final MutableLiveData<List<Lap>> _laps = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Lap>> laps = _laps;
    private final List<Lap> lapsList = new ArrayList<>();

    TimerMode currentMode;
    private CountDownTimer pomodoroTimer;
    private long pomodoroTimeLeftInMillis;
    private long currentPomodoroSessionDuration;

    private final Handler stopwatchHandler = new Handler(Looper.getMainLooper());
    private long stopwatchStartTime = 0L;
    private long stopwatchTimeInMillis = 0L;
    private long stopwatchUpdateTime = 0L;
    private long stopwatchStoredTime = 0L;
    private long lastLapTime = 0L;

    private boolean shouldSaveOnExit = true;

    public TimerViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        taskLogRepository = new TaskLogRepository(application);
        setMode(TimerMode.POMODORO);
    }

    public void setTask(Task task) {
        if (task != null) {
            _task.setValue(task);
        }
    }

    public void setMode(TimerMode mode) {
        if (currentMode == mode) return;
        pauseTimerAndSave();
        currentMode = mode;
        resetTimer();
    }

    public void toggleTimer() {
        if (_isTimerRunning.getValue() != null && _isTimerRunning.getValue()) {
            pauseTimer();
        } else {
            startTimer();
        }
    }

    private void startTimer() {
        _isTimerRunning.setValue(true);
        _showResetButton.setValue(true);
        if (currentMode == TimerMode.POMODORO) {
            _showSkipButton.setValue(true);
            startPomodoro();
        } else { // STOPWATCH
            _showLapButton.setValue(true);
            startStopwatch();
        }
    }

    private void pauseTimer() {
        _isTimerRunning.setValue(false);
        if (currentMode == TimerMode.POMODORO) {
            if (pomodoroTimer != null) {
                pomodoroTimer.cancel();
            }
        } else { // STOPWATCH
            stopwatchHandler.removeCallbacks(stopwatchRunnable);
            stopwatchStoredTime += stopwatchTimeInMillis;
            stopwatchTimeInMillis = 0;
        }
    }

    void pauseTimerAndSave() {
        if (_isTimerRunning.getValue() != null && _isTimerRunning.getValue()) {
            pauseTimer();
        }

        if (currentMode == TimerMode.POMODORO) {
            if (_pomodoroState.getValue() == PomodoroState.WORK) {
                long elapsedTime = currentPomodoroSessionDuration - pomodoroTimeLeftInMillis;
                updateTaskTimeSpent(elapsedTime);
                pomodoroTimeLeftInMillis = currentPomodoroSessionDuration;
            }
        } else { // STOPWATCH
            if (stopwatchStoredTime > 0) {
                updateTaskTimeSpent(stopwatchStoredTime);
                stopwatchStoredTime = 0;
            }
        }
    }

    public void resetTimer() {
        pauseTimer();
        _isTimerRunning.setValue(false);
        _showResetButton.setValue(false);

        if (currentMode == TimerMode.POMODORO) {
            _pomodoroSessionCount.setValue(0);
            _pomodoroState.setValue(PomodoroState.WORK);
            currentPomodoroSessionDuration = POMODORO_WORK_DURATION_MS;
            pomodoroTimeLeftInMillis = currentPomodoroSessionDuration;
            updatePomodoroDisplay();
            _showSkipButton.setValue(false);
        } else { // STOPWATCH
            stopwatchStartTime = 0L;
            stopwatchTimeInMillis = 0L;
            stopwatchUpdateTime = 0L;
            stopwatchStoredTime = 0L;
            lastLapTime = 0L;
            lapsList.clear();
            _laps.setValue(new ArrayList<>(lapsList));
            updateStopwatchDisplay();
            _showLapButton.setValue(false);
        }
    }

    private void startPomodoro() {
        if (pomodoroTimeLeftInMillis <= 0) {
            moveToNextPomodoroState();
        }

        pomodoroTimer = new CountDownTimer(pomodoroTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                pomodoroTimeLeftInMillis = millisUntilFinished;
                updatePomodoroDisplay();
            }

            @Override
            public void onFinish() {
                if (_pomodoroState.getValue() == PomodoroState.WORK) {
                    updateTaskTimeSpent(currentPomodoroSessionDuration);
                }
                moveToNextPomodoroState();
            }
        }.start();
    }

    public void skipPomodoroSession() {
        if (currentMode != TimerMode.POMODORO) return;
        moveToNextPomodoroState();
    }

    private void moveToNextPomodoroState() {
        if (pomodoroTimer != null) {
            pomodoroTimer.cancel();
        }

        PomodoroState currentState = _pomodoroState.getValue();
        int currentSessionCount = _pomodoroSessionCount.getValue() != null ? _pomodoroSessionCount.getValue() : 0;

        if (currentState == PomodoroState.WORK) { // WORK -> BREAK
            currentSessionCount++;
            _pomodoroSessionCount.setValue(currentSessionCount);
            if (currentSessionCount % POMODORO_SESSIONS_UNTIL_LONG_BREAK == 0) {
                _pomodoroState.setValue(PomodoroState.LONG_BREAK);
                currentPomodoroSessionDuration = POMODORO_LONG_BREAK_DURATION_MS;
            } else {
                _pomodoroState.setValue(PomodoroState.SHORT_BREAK);
                currentPomodoroSessionDuration = POMODORO_SHORT_BREAK_DURATION_MS;
            }
        } else { // BREAK -> WORK (or initial state)
            _pomodoroState.setValue(PomodoroState.WORK);
            currentPomodoroSessionDuration = POMODORO_WORK_DURATION_MS;
        }

        pomodoroTimeLeftInMillis = currentPomodoroSessionDuration;
        updatePomodoroDisplay();
        _isTimerRunning.setValue(false);
        _showSkipButton.setValue(false);
    }

    private void updatePomodoroDisplay() {
        long minutes = (pomodoroTimeLeftInMillis / 1000) / 60;
        long seconds = (pomodoroTimeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        _timeDisplay.setValue(timeLeftFormatted);
    }

    private void startStopwatch() {
        stopwatchStartTime = SystemClock.uptimeMillis();
        stopwatchHandler.postDelayed(stopwatchRunnable, 0);
    }

    private final Runnable stopwatchRunnable = new Runnable() {
        @Override
        public void run() {
            stopwatchTimeInMillis = SystemClock.uptimeMillis() - stopwatchStartTime;
            stopwatchUpdateTime = stopwatchStoredTime + stopwatchTimeInMillis;
            updateStopwatchDisplay();
            stopwatchHandler.postDelayed(this, 10);
        }
    };

    private void updateStopwatchDisplay() {
        _timeDisplay.setValue(formatStopwatchTime(stopwatchUpdateTime));
    }

    private String formatStopwatchTime(long timeInMillis) {
        long secs = timeInMillis / 1000;
        long mins = secs / 60;
        secs %= 60;
        long milliseconds = (timeInMillis % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", mins, secs, milliseconds);
    }

    public void recordLap() {
        if (currentMode != TimerMode.STOPWATCH || (_isTimerRunning.getValue() != null && !_isTimerRunning.getValue())) return;
        long now = stopwatchUpdateTime;
        long lapDuration = now - lastLapTime;
        String totalTimeFormatted = formatStopwatchTime(now);
        String lapTimeFormatted = formatStopwatchTime(lapDuration);
        if (lapsList.isEmpty()) {
            lapTimeFormatted = totalTimeFormatted;
        }
        lastLapTime = now;
        int lapNumber = lapsList.size() + 1;
        lapsList.add(0, new Lap(lapNumber, lapTimeFormatted, totalTimeFormatted));
        _laps.setValue(new ArrayList<>(lapsList));
    }

    private void updateTaskTimeSpent(long timeToAddInMs) {
        long oneMinuteInMillis = TimeUnit.MINUTES.toMillis(1);
        if (timeToAddInMs < oneMinuteInMillis) {
            if (timeToAddInMs > 0) {
                Toast.makeText(getApplication(), "Session less than a minute. Not saved.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Task currentTask = _task.getValue();
        if (currentTask != null) {
            currentTask.addTimeSpent(timeToAddInMs);
            new Thread(() -> taskRepository.update(currentTask)).start();

            long endTime = System.currentTimeMillis();
            long startTime = endTime - timeToAddInMs;
            long durationMinutes = TimeUnit.MILLISECONDS.toMinutes(timeToAddInMs);

            TaskLogEntity log = new TaskLogEntity();
            log.setTaskId(currentTask.getId());
            log.setTimestamp(startTime);
            log.setEndTimeMillis(endTime);
            log.setDurationMinutes((int) durationMinutes);
            log.setSource(currentMode == TimerMode.POMODORO ? "Pomodoro" : "Stopwatch");
            log.setNote("");

            new Thread(() -> taskLogRepository.insertLog(log)).start();

        } else {
            Toast.makeText(this.getApplication(), "Task is Null, cannot save time.", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean hasProgressToSave() {
        if (currentMode == TimerMode.POMODORO) {
            if (_pomodoroState.getValue() == PomodoroState.WORK) {
                long elapsedTime = currentPomodoroSessionDuration - pomodoroTimeLeftInMillis;
                return elapsedTime > 0;
            }
            return false;
        } else { // STOPWATCH
            return stopwatchUpdateTime > 0;
        }
    }

    public void preventSaveOnExit() {
        shouldSaveOnExit = false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (shouldSaveOnExit) {
            pauseTimerAndSave();
        }
        if (pomodoroTimer != null) {
            pomodoroTimer.cancel();
        }
        stopwatchHandler.removeCallbacks(stopwatchRunnable);
    }
}
