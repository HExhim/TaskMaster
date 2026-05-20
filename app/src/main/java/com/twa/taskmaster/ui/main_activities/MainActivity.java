package com.twa.taskmaster.ui.main_activities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.util.BackupHelper;
import com.twa.taskmaster.core.util.DummyDataGenerator;
import com.twa.taskmaster.core.worker.DailyReportWorker;
import com.twa.taskmaster.data.repository.ReminderRepository;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.data.sync.SyncManager;
import com.twa.taskmaster.ui.main_activities.LoginActivity;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends BaseActivity {

    private TaskRepository taskRepository;
    private TaskLogRepository taskLogRepository;
    private ReminderRepository reminderRepository;

    private View progressBarMain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        if (!checkAuthStatus()) return;

        setContentView(R.layout.activity_main);
        progressBarMain = findViewById(R.id.progressBarMain);

        initRepositories();
        handleInitialDataLoad();

        //generateDummyData();

        setupNavigation();
        requestPermissionsIfNeeded();
        requestExactAlarmIfNeeded();
        scheduleDailyReport();
    }
    private void handleInitialDataLoad() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstRun = prefs.getBoolean("is_first_run", true);
        long lastBackupTime = BackupHelper.getLastBackupTime(this); // preserved for future use

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (isFirstRun) {
            // On first run, attempt to pull from Firebase if there's a non-anonymous user
            if (user != null) {
                // dispatch sync off UI thread if SyncManager does not already
                Executors.newSingleThreadExecutor().execute(() -> SyncManager.getInstance().syncAll());
            }
            prefs.edit().putBoolean("is_first_run", false).apply();
        } else {
            // Subsequent runs: keep data fresh by syncing when a real user is present
            if (user != null) {
                Executors.newSingleThreadExecutor().execute(() -> SyncManager.getInstance().syncAll());
            }
            // else: no-op for anonymous / signed-out users
        }
    }

    private void initRepositories() {
        taskRepository = new TaskRepository(getApplication());
        taskLogRepository = new TaskLogRepository(getApplication());
        reminderRepository = new ReminderRepository(getApplication());
    }

    private void generateDummyData() {
        showLoading(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            new DummyDataGenerator(taskRepository, taskLogRepository, reminderRepository)
                    .createAndInsertDummyTasks(50);

            new Handler(Looper.getMainLooper()).post(() -> showLoading(false));
        });
    }

    private void setupNavigation() {
        NavHostFragment navHost =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHost == null) throw new IllegalStateException("NavHostFragment is null.");

        NavController navController = navHost.getNavController();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        NavigationUI.setupWithNavController(bottomNav, navController);
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }

        if (checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM}, 100);
        }
    }

    private void requestExactAlarmIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private boolean checkAuthStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean guestMode = prefs.getBoolean("is_guest_mode", false);

        if (user == null && guestMode) {
            FirebaseAuth.getInstance().signInAnonymously().addOnSuccessListener(r ->
                    SyncManager.getInstance().syncAll()
            );
            return true;
        }

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return false;
        }

        return true;
    }

    private void showLoading(boolean visible) {
        if (progressBarMain != null) {
            progressBarMain.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void scheduleDailyReport() {
        Calendar now = Calendar.getInstance();
        Calendar due = Calendar.getInstance();
        due.set(Calendar.HOUR_OF_DAY, 20);
        due.set(Calendar.MINUTE, 0);
        due.set(Calendar.SECOND, 0);

        if (due.before(now)) {
            due.add(Calendar.HOUR_OF_DAY, 24);
        }

        long delay = due.getTimeInMillis() - now.getTimeInMillis();

        PeriodicWorkRequest dailyReport = new PeriodicWorkRequest.Builder(
                DailyReportWorker.class,
                24, TimeUnit.HOURS
        ).setInitialDelay(delay, TimeUnit.MILLISECONDS).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyReportWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyReport
        );
    }
}
