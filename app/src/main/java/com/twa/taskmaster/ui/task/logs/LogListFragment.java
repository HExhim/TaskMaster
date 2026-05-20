package com.twa.taskmaster.ui.task.logs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.TimePeriod;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.domain.model.TaskLog;
import com.twa.taskmaster.viewmodel.SharedTaskViewModel;

import java.util.List;

public class LogListFragment extends Fragment{

    private static final String ARG_FILTER = "filter_type";

    private LogsAdapter logsAdapter;
    private SharedTaskViewModel sharedViewModel;
    private ActionMode actionMode;
    private TextView tvTotalTime;

    public static LogListFragment newInstance(TimePeriod filterType) {
        LogListFragment fragment = new LogListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FILTER, filterType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedTaskViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.logRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        tvTotalTime = view.findViewById(R.id.tvTotalTime);

        logsAdapter = new LogsAdapter(getContext(), new LogsAdapter.OnLogActionListener() {

            @Override
            public void onLogLongClick(TaskLogEntity tasklog) {
                if (actionMode == null) {
                    actionMode = requireActivity().startActionMode(actionModeCallback);
                    logsAdapter.setMultiSelectMode(true);
                }
                toggleSelection(tasklog);
            }

            @Override
            public void onLogClick(TaskLogEntity taskLog) {
                if (actionMode != null) {
                    toggleSelection(taskLog);
                }

            }
        });
        recyclerView.setAdapter(logsAdapter);

        if (getArguments() != null) {
            TimePeriod filterType = (TimePeriod) getArguments().getSerializable(ARG_FILTER);
            
            sharedViewModel.getLogs(filterType).observe(getViewLifecycleOwner(), logs -> {
                if (logs != null) {
                    logsAdapter.submitList(logs);
                    String summary = sharedViewModel.calculateTotalTime(logs);
                    tvTotalTime.setText("Total Logged: " + summary);
                    
                    if (actionMode != null) {
                        actionMode.finish();
                    }
                }
            });
        }
    }
    private void toggleSelection(TaskLogEntity taskLog) {
        logsAdapter.toggleSelection(taskLog.getId());
        int count = logsAdapter.getSelectedItems().size();

        if (count == 0 && actionMode != null) {
            actionMode.finish();
        } else if (actionMode != null) {
            actionMode.setTitle(count + " selected");
            actionMode.invalidate();
        }
    }


    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.task_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete_selected) {
                List<Long> selectedItems = logsAdapter.getSelectedItems();
                sharedViewModel.deleteLogs(selectedItems);
                Toast.makeText(getContext(), selectedItems.size() + " logs deleted", Toast.LENGTH_SHORT).show();
                mode.finish(); // Finish action mode after deletion
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            logsAdapter.clearSelection();
            actionMode = null;
        }
    };
}
