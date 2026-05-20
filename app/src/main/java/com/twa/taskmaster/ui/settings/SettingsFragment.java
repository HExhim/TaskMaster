package com.twa.taskmaster.ui.settings;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.util.BackupHelper;
import com.twa.taskmaster.core.util.NetworkUtils;
import com.twa.taskmaster.core.util.ThemeHelper;
import com.twa.taskmaster.core.worker.DailyReportWorker;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.data.sync.SyncManager;
import com.twa.taskmaster.ui.main_activities.LoginActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SettingsFragment extends PreferenceFragmentCompat {

    private TaskRepository taskRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskRepository = new TaskRepository(requireActivity().getApplication());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        setupAccountPreferences();
        setupThemePreferences();
        setupDailyReportPreferences();
        setupDataPreferences();
        observeSyncState();
    }
    
    private void observeSyncState() {
        SyncManager.getInstance().getSyncState().observe(this, syncState -> {
            if (syncState == SyncManager.SyncState.SYNCED) {
                // Toast.makeText(requireContext(), "Sync Complete", Toast.LENGTH_SHORT).show();
            } else if (syncState == SyncManager.SyncState.NO_CONNECTION) {
                // Toast.makeText(requireContext(), "Sync Failed: No Connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAccountPreferences() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Preference emailPref = findPreference("account_email");
        if (emailPref != null && user != null) {
            emailPref.setSummary(user.isAnonymous() ? "Guest Account" : user.getEmail());
        }

        Preference logoutPref = findPreference("logout");
        if (logoutPref != null) {
            logoutPref.setOnPreferenceClickListener(preference -> {
                android.content.Context context = requireContext();
                java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                    com.twa.taskmaster.data.local.database.Database.getInstance(context).clearAllTables();

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        // Clear Guest Mode flag
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                        prefs.edit().remove("is_guest_mode").apply();

                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(context, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                });
                return true;
            });
        }
    }

    private void setupThemePreferences() {
        ListPreference themePref = findPreference("theme_preference");
        if (themePref != null) {
            themePref.setOnPreferenceChangeListener((preference, newValue) -> {
                ThemeHelper.applyTheme(requireContext());
                requireActivity().recreate();
                return true;
            });
        }
    }

    private void setupDailyReportPreferences() {
        SwitchPreferenceCompat dailySummaryPref = findPreference("daily_summary_enabled");
        Preference timePref = findPreference("daily_report_time");
        
        if (timePref != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            long timeInMillis = prefs.getLong("daily_report_time_millis", -1);
            
            Calendar c = Calendar.getInstance();
            if (timeInMillis != -1) {
                c.setTimeInMillis(timeInMillis);
            } else {
                // Default to 8 PM
                c.set(Calendar.HOUR_OF_DAY, 20);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
            }
            timePref.setSummary(DateFormat.getTimeFormat(requireContext()).format(c.getTime()));

            timePref.setOnPreferenceClickListener(preference -> {
                showTimePicker(timePref);
                return true;
            });
        }

        if (dailySummaryPref != null) {
            dailySummaryPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (boolean) newValue;
                if (enabled) {
                    scheduleDailyReport();
                } else {
                    WorkManager.getInstance(requireContext()).cancelUniqueWork("DailyReportWork");
                }
                return true;
            });
        }
    }

    private void showTimePicker(Preference timePref) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        new TimePickerDialog(requireContext(), (view, hourOfDay, minute1) -> {
            Calendar time = Calendar.getInstance();
            time.set(Calendar.HOUR_OF_DAY, hourOfDay);
            time.set(Calendar.MINUTE, minute1);
            time.set(Calendar.SECOND, 0);

            long millis = time.getTimeInMillis();
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
            editor.putLong("daily_report_time_millis", millis);
            editor.apply();

            timePref.setSummary(DateFormat.getTimeFormat(requireContext()).format(time.getTime()));
            scheduleDailyReport();

        }, hour, minute, DateFormat.is24HourFormat(requireContext())).show();
    }

    private void scheduleDailyReport() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean enabled = prefs.getBoolean("daily_summary_enabled", true);
        if (!enabled) return;

        long timeInMillis = prefs.getLong("daily_report_time_millis", -1);
        if (timeInMillis == -1) {
            // Default to 8 PM
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, 20);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            timeInMillis = c.getTimeInMillis();
        }

        Calendar currentDate = Calendar.getInstance();
        Calendar dueDate = Calendar.getInstance();
        dueDate.set(Calendar.HOUR_OF_DAY, 0);
        dueDate.set(Calendar.MINUTE, 0);
        dueDate.set(Calendar.SECOND, 0);
        dueDate.set(Calendar.MILLISECOND, 0);
        
        // Extract hour and minute from saved time
        Calendar savedTime = Calendar.getInstance();
        savedTime.setTimeInMillis(timeInMillis);
        
        dueDate.set(Calendar.HOUR_OF_DAY, savedTime.get(Calendar.HOUR_OF_DAY));
        dueDate.set(Calendar.MINUTE, savedTime.get(Calendar.MINUTE));

        // Adjust to next occurrence
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24);
        }

        long delay = dueDate.getTimeInMillis() - currentDate.getTimeInMillis();

        PeriodicWorkRequest dailyWorkRequest = new PeriodicWorkRequest.Builder(
                DailyReportWorker.class,
                24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "DailyReportWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyWorkRequest);

        Toast.makeText(requireContext(), "Daily report scheduled", Toast.LENGTH_SHORT).show();
    }

    private void setupDataPreferences() {
        Preference manualSyncPref = findPreference("manual_sync");
        if (manualSyncPref != null) {
            manualSyncPref.setOnPreferenceClickListener(preference -> {
                if (NetworkUtils.isNetworkAvailable(requireContext())) {
                    SyncManager.getInstance().syncAll();
                    Toast.makeText(requireContext(), "Sync started", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Offline: Cannot sync now", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference exportPref = findPreference("export_data");
        if (exportPref != null) {
            exportPref.setOnPreferenceClickListener(preference -> {
                Uri uri = taskRepository.exportTasksToCSV();
                if (uri != null) {
                    shareFile(uri);
                } else {
                    Toast.makeText(requireContext(), "Export failed", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference backupPref = findPreference("manual_backup");
        if (backupPref != null) {
            updateBackupSummary(backupPref);
            backupPref.setOnPreferenceClickListener(preference -> {
                if (BackupHelper.backupDatabase(requireContext())) {
                    Toast.makeText(requireContext(), "Backup successful", Toast.LENGTH_SHORT).show();
                    updateBackupSummary(backupPref);
                    // Update restore pref as well if it's visible
                    Preference restorePref = findPreference("manual_restore");
                    if (restorePref != null) updateRestoreSummary(restorePref);
                    
                    // Update delete backup pref if it's visible
                    Preference deleteBackupPref = findPreference("delete_backup");
                    if (deleteBackupPref != null) deleteBackupPref.setEnabled(true);

                } else {
                    Toast.makeText(requireContext(), "Backup failed", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        Preference restorePref = findPreference("manual_restore");
        if (restorePref != null) {
            updateRestoreSummary(restorePref);
            restorePref.setOnPreferenceClickListener(preference -> {
                if (BackupHelper.restoreDatabase(requireContext())) {
                    Toast.makeText(requireContext(), "Restore successful. Restarting...", Toast.LENGTH_SHORT).show();
                    restartApp();
                } else {
                    Toast.makeText(requireContext(), "Restore failed. No backup found?", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
        
        Preference deleteBackupPref = findPreference("delete_backup");
        if (deleteBackupPref != null) {
            long lastBackup = BackupHelper.getLastBackupTime(requireContext());
            deleteBackupPref.setEnabled(lastBackup > 0);
            
            deleteBackupPref.setOnPreferenceClickListener(preference -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Backup")
                    .setMessage("Are you sure you want to delete the backup? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (BackupHelper.deleteBackup(requireContext())) {
                            Toast.makeText(requireContext(), "Backup deleted", Toast.LENGTH_SHORT).show();
                            deleteBackupPref.setEnabled(false);
                            // Update other prefs summaries
                            Preference backupPref2 = findPreference("manual_backup");
                            if (backupPref2 != null) updateBackupSummary(backupPref2);
                            Preference restorePref2 = findPreference("manual_restore");
                            if (restorePref2 != null) updateRestoreSummary(restorePref2);
                        } else {
                            Toast.makeText(requireContext(), "Failed to delete backup", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return true;
            });
        }

        SwitchPreferenceCompat syncPref = findPreference("enable_sync");
        if (syncPref != null) {
            syncPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if ((boolean) newValue) {
                    // Only sync if internet is available
                    if (NetworkUtils.isNetworkAvailable(requireContext())) {
                         SyncManager.getInstance().syncAll();
                         Toast.makeText(requireContext(), "Auto Sync enabled", Toast.LENGTH_SHORT).show();
                    } else {
                         Toast.makeText(requireContext(), "Offline: Auto sync will run when online", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            });
        }
    }
    
    private void updateBackupSummary(Preference preference) {
        long lastBackup = BackupHelper.getLastBackupTime(requireContext());
        if (lastBackup > 0) {
            preference.setSummary("Last backup: " + DateFormat.getDateFormat(requireContext()).format(new Date(lastBackup)) + 
                    " " + DateFormat.getTimeFormat(requireContext()).format(new Date(lastBackup)));
        } else {
            preference.setSummary("No backup found");
        }
    }

    private void updateRestoreSummary(Preference preference) {
        long lastBackup = BackupHelper.getLastBackupTime(requireContext());
        if (lastBackup > 0) {
            preference.setSummary("Restore from: " + DateFormat.getDateFormat(requireContext()).format(new Date(lastBackup)) + 
                    " " + DateFormat.getTimeFormat(requireContext()).format(new Date(lastBackup)));
            preference.setEnabled(true);
        } else {
            preference.setSummary("No backup available");
            preference.setEnabled(false);
        }
    }

    private void shareFile(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Export Tasks"));
    }

    private void restartApp() {
        Intent intent = requireActivity().getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(requireActivity().getBaseContext().getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
            Runtime.getRuntime().exit(0);
        }
    }
}
