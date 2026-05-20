package com.twa.taskmaster.ui.timer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.twa.taskmaster.R;
import com.twa.taskmaster.databinding.ActivityTimerBinding;
import com.twa.taskmaster.domain.model.Task;

public class TimerActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB_INDEX = "initial_tab_index";
    public static final String EXTRA_TASK = "extra_task";

    private ActivityTimerBinding binding;
    private TimerViewModel viewModel;
    private LapsAdapter lapsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTimerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(TimerViewModel.class);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupRecyclerView();
        setupTabs();
        setupClickListeners();
        observeViewModel();

        // Set initial state from Intent
        if (savedInstanceState == null) {
            Task task = (Task) getIntent().getSerializableExtra(EXTRA_TASK);
            viewModel.setTask(task);

            int initialTabIndex = getIntent().getIntExtra(EXTRA_INITIAL_TAB_INDEX, 0);
            TimerViewModel.TimerMode initialMode = initialTabIndex == 0 ? TimerViewModel.TimerMode.POMODORO : TimerViewModel.TimerMode.STOPWATCH;
            viewModel.setMode(initialMode);

            if (binding.tabLayout.getSelectedTabPosition() != initialTabIndex) {
                if (initialTabIndex >= 0 && initialTabIndex < binding.tabLayout.getTabCount()) {
                    binding.tabLayout.getTabAt(initialTabIndex).select();
                }
            }
        }
    }

    private void setupRecyclerView() {
        lapsAdapter = new LapsAdapter();
        binding.lapsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.lapsRecyclerView.setAdapter(lapsAdapter);
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                TimerViewModel.TimerMode newMode = tab.getPosition() == 0 ? TimerViewModel.TimerMode.POMODORO : TimerViewModel.TimerMode.STOPWATCH;
                // The ViewModel now handles the logic of when to allow a mode switch
                viewModel.setMode(newMode);
                updateUIForMode(newMode);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupClickListeners() {
        binding.fabStartPause.setOnClickListener(v -> viewModel.toggleTimer());
        binding.buttonReset.setOnClickListener(v -> viewModel.resetTimer());
        binding.buttonSkipBreak.setOnClickListener(v -> {
            if (viewModel.currentMode == TimerViewModel.TimerMode.POMODORO) {
                viewModel.skipPomodoroSession();
            } else { // STOPWATCH
                viewModel.recordLap();
            }
        });
    }

    private void observeViewModel() {
        viewModel.timeDisplay.observe(this, time -> binding.timerDisplay.setText(time));

        viewModel.isTimerRunning.observe(this, isRunning -> {
            binding.fabStartPause.setImageResource(isRunning ? R.drawable.ic_pause : R.drawable.ic_play);

            // Disable tab layout when timer is running to prevent switching modes
            LinearLayout tabStrip = ((LinearLayout) binding.tabLayout.getChildAt(0));
            for (int i = 0; i < tabStrip.getChildCount(); i++) {
                tabStrip.getChildAt(i).setEnabled(!isRunning);
            }
        });

        viewModel.pomodoroSessionCount.observe(this, this::updatePomodoroSessionIndicators);

        viewModel.showResetButton.observe(this, show -> binding.buttonReset.setVisibility(show ? View.VISIBLE : View.GONE));
        viewModel.showSkipButton.observe(this, show -> {
            if (viewModel.currentMode == TimerViewModel.TimerMode.POMODORO) {
                binding.buttonSkipBreak.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
        viewModel.showLapButton.observe(this, show -> {
            if (viewModel.currentMode == TimerViewModel.TimerMode.STOPWATCH) {
                binding.buttonSkipBreak.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.laps.observe(this, laps -> lapsAdapter.submitList(laps));

        viewModel.task.observe(this, task -> {
            if (task != null) {
                getSupportActionBar().setTitle(task.getTitle());
            } else {
                getSupportActionBar().setTitle("Timer");
            }
        });
    }

    private void updateUIForMode(TimerViewModel.TimerMode mode) {
        if (mode == TimerViewModel.TimerMode.POMODORO) {
            binding.pomodoroSessions.setVisibility(View.VISIBLE);
            binding.lapsRecyclerView.setVisibility(View.GONE);
            binding.buttonSkipBreak.setText("Skip");
        } else { // STOPWATCH
            binding.pomodoroSessions.setVisibility(View.GONE);
            binding.lapsRecyclerView.setVisibility(View.VISIBLE);
            binding.buttonSkipBreak.setText("Lap");
        }
    }

    private void updatePomodoroSessionIndicators(int count) {
        binding.pomodoroSessions.removeAllViews();
        for (int i = 0; i < 4; i++) {
            ImageView sessionIndicator = new ImageView(this);
            int size = getResources().getDimensionPixelSize(R.dimen.session_indicator_size);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            int margin = getResources().getDimensionPixelSize(R.dimen.session_indicator_margin);
            params.setMargins(margin, 0, margin, 0);
            sessionIndicator.setLayoutParams(params);

            if (i < count % 4) {
                sessionIndicator.setImageResource(R.drawable.ic_pomodoro_session_filled);
            } else {
                sessionIndicator.setImageResource(R.drawable.ic_pomodoro_session_outline);
            }
            binding.pomodoroSessions.addView(sessionIndicator);
        }
    }

    @Override
    public void onBackPressed() {
        if (viewModel.hasProgressToSave()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Unsaved Progress")
                    .setMessage("You have an unsaved timer session. What would you like to do?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        viewModel.pauseTimerAndSave();
                        super.onBackPressed();
                    })
                    .setNegativeButton("Discard", (dialog, which) -> {
                        viewModel.preventSaveOnExit();
                        super.onBackPressed();
                    })
                    .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // This ensures the same logic is triggered for the up button.
        return true;
    }
}
