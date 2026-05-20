package com.twa.taskmaster.ui.task.logs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.SortLogs;
import com.twa.taskmaster.core.enums.TimePeriod;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.ui.dialogs.AddLogDialogFragment;
import com.twa.taskmaster.ui.timer.TimerActivity;
import com.twa.taskmaster.viewmodel.SharedTaskViewModel;

public class TaskLogFragment extends Fragment {

    private SharedTaskViewModel sharedViewModel;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private LinearLayout emptyState;
    private Task task;
    private Spinner sortSpinner;
    private Spinner sourceFilterSpinner;
    private FloatingActionButton fabAddLog, fabManual, fabPomodoro, fabStopwatch;
    private TextView labelManual, labelPomodoro, labelStopwatch;
    private boolean isFabMenuOpen = false;

    public static TaskLogFragment newInstance() {
        return new TaskLogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedTaskViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_logs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel.getTask().observe(getViewLifecycleOwner(), task -> {
            if (task != null) {
                this.task = task;
                setupViewPager();
            } else {
                Toast.makeText(getContext(), "Task is null", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize Views
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
        emptyState = view.findViewById(R.id.emptyState);
        sortSpinner = view.findViewById(R.id.sort_spinner);
        sourceFilterSpinner = view.findViewById(R.id.source_filter_spinner);

        // Initialize FABs and Labels
        fabAddLog = view.findViewById(R.id.fabAddLog);
        fabManual = view.findViewById(R.id.fab_manual);
        fabPomodoro = view.findViewById(R.id.fab_pomodoro);
        fabStopwatch = view.findViewById(R.id.fab_stopwatch);
        labelManual = view.findViewById(R.id.label_manual);
        labelPomodoro = view.findViewById(R.id.label_pomodoro);
        labelStopwatch = view.findViewById(R.id.label_stopwatch);

        setupFabMenu();
        setupSpinners();

        sharedViewModel.getFilteredLogs().observe(getViewLifecycleOwner(), logs -> {
            emptyState.setVisibility((logs == null || logs.isEmpty()) ? View.VISIBLE : View.GONE);
            viewPager.setVisibility((logs == null || logs.isEmpty()) ? View.GONE : View.VISIBLE);
        });
    }

    private void setupSpinners() {
        // Sort Spinner
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.sort_log_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SortLogs sortType;
                switch (position) {
                    case 1:
                        sortType = SortLogs.DATE_OLDEST;
                        break;
                    case 2:
                        sortType = SortLogs.DURATION_LONGEST;
                        break;
                    case 3:
                        sortType = SortLogs.DURATION_SHORTEST;
                        break;
                    default:
                        sortType = SortLogs.DATE_NEWEST;
                        break;
                }
                sharedViewModel.setSort(sortType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Source Filter Spinner
        ArrayAdapter<CharSequence> sourceFilterAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.source_filter_log_options, android.R.layout.simple_spinner_item);
        sourceFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceFilterSpinner.setAdapter(sourceFilterAdapter);
        sourceFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
             @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String source = parent.getItemAtPosition(position).toString();
                sharedViewModel.setSourceFilter(source);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupViewPager() {
        TaskLogsPagerAdapter pagerAdapter = new TaskLogsPagerAdapter(requireActivity());
        viewPager.setAdapter(pagerAdapter);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("All");
                    break;
                case 1:
                    tab.setText("Today");
                    break;
                case 2:
                    tab.setText("Week");
                    break;
                case 3:
                    tab.setText("Month");
                    break;
            }
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                TimePeriod filter = pagerAdapter.getFilterType(position);
                sharedViewModel.setCurrentFilter(filter);
            }
        });
    }

    private void setupFabMenu() {
        fabAddLog.setOnClickListener(v -> {
            if (isFabMenuOpen) {
                closeFabMenu();
            } else {
                openFabMenu();
            }
        });

        fabManual.setOnClickListener(v -> {
            showAddLogDialog();
            closeFabMenu();
        });

        fabPomodoro.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TimerActivity.class);
            intent.putExtra(TimerActivity.EXTRA_TASK, task);
            intent.putExtra(TimerActivity.EXTRA_INITIAL_TAB_INDEX, 0); // Pomodoro
            startActivity(intent);
            closeFabMenu();
        });

        fabStopwatch.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TimerActivity.class);
            intent.putExtra(TimerActivity.EXTRA_TASK, task);
            intent.putExtra(TimerActivity.EXTRA_INITIAL_TAB_INDEX, 1); // Stopwatch
            startActivity(intent);
            closeFabMenu();
        });
    }

    private void openFabMenu() {
        isFabMenuOpen = true;
        fabAddLog.animate().rotation(45f);
        showSubFab(fabStopwatch, labelStopwatch);
        showSubFab(fabPomodoro, labelPomodoro);
        showSubFab(fabManual, labelManual);
    }

    private void closeFabMenu() {
        isFabMenuOpen = false;
        fabAddLog.animate().rotation(0f);

        hideSubFab(fabStopwatch, labelStopwatch);
        hideSubFab(fabPomodoro, labelPomodoro);
        hideSubFab(fabManual, labelManual);
    }

    private void showSubFab(FloatingActionButton fab, TextView label) {
        fab.setVisibility(View.VISIBLE);
        label.setVisibility(View.VISIBLE);
        fab.setAlpha(0f);
        label.setAlpha(0f);
        fab.setTranslationY(100f);
        label.setTranslationY(100f);
        fab.animate().alpha(1f).translationY(0f).setDuration(200).start();
        label.animate().alpha(1f).translationY(0f).setDuration(200).start();
    }

    private void hideSubFab(FloatingActionButton fab, TextView label) {
        fab.animate().alpha(0f).translationY(100f).setDuration(200).withEndAction(() -> {
            fab.setVisibility(View.INVISIBLE);
            label.setVisibility(View.INVISIBLE);
        }).start();
    }

    private void showAddLogDialog() {
        if (task == null) return;
        AddLogDialogFragment dialog = AddLogDialogFragment.newInstance(task.getId());
        dialog.show(getChildFragmentManager(), "AddLogDialogFragment");
    }
}
