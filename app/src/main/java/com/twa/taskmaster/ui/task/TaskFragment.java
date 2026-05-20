package com.twa.taskmaster.ui.task;

import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.SortTask;
import com.twa.taskmaster.core.util.NetworkUtils;
import com.twa.taskmaster.data.sync.SyncManager;
import com.twa.taskmaster.databinding.FragmentTaskBinding;
import com.twa.taskmaster.domain.TaskTouchCallback;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.ui.dialogs.AddCategoryDialogFragment;
import com.twa.taskmaster.ui.task.details.TaskDetailActivity;
import com.twa.taskmaster.ui.timer.TimerActivity;
import com.twa.taskmaster.viewmodel.TaskViewModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TaskFragment extends Fragment {
    private FragmentTaskBinding binding;
    private TaskAdapter adapter;
    private TaskViewModel viewModel;
    private ActionMode actionMode;
    private List<String> availableCategories = Collections.emptyList();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.toolbar);

        setupRecyclerView();
        setupFloatingActionButton();
        setupFilterChips();
        observeViewModel();
        observeSyncState();
    }
    
    private void observeSyncState() {
        SyncManager.getInstance().getSyncState().observe(getViewLifecycleOwner(), syncState -> {
            if (syncState == SyncManager.SyncState.PENDING) {
               // Toast.makeText(requireContext(), "Syncing...", Toast.LENGTH_SHORT).show();
            } else if (syncState == SyncManager.SyncState.SYNCED) {
               // Toast.makeText(requireContext(), "Sync Complete", Toast.LENGTH_SHORT).show();
            } else if (syncState == SyncManager.SyncState.NO_CONNECTION) {
               // Toast.makeText(requireContext(), "Sync Failed: No Connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(requireContext(), new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onPomodoroClick(Task task) {
                Intent intent = new Intent(requireContext(), TimerActivity.class);
                intent.putExtra(TimerActivity.EXTRA_INITIAL_TAB_INDEX, 0); // 0 for Pomodoro
                intent.putExtra(TimerActivity.EXTRA_TASK, task);
                startActivity(intent);
            }

            @Override
            public void onStopwatchClick(Task task) {
                Intent intent = new Intent(requireContext(), TimerActivity.class);
                intent.putExtra(TimerActivity.EXTRA_INITIAL_TAB_INDEX, 1); // 1 for Stopwatch
                intent.putExtra(TimerActivity.EXTRA_TASK, task);
                startActivity(intent);
            }

            @Override
            public void onTaskLongClick(Task task) {
                if (actionMode == null) {
                    actionMode = requireActivity().startActionMode(actionModeCallback);
                    adapter.setMultiSelectMode(true);
                }
                toggleSelection(task);
            }

            @Override
            public void onTaskClick(Task task) {
                if (actionMode != null) {
                    toggleSelection(task);
                } else {
                    navigateToTaskDetails(task);
                }
            }
        });

        binding.taskRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.taskRecycler.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new TaskTouchCallback(new TaskSwipeListener() {
            @Override
            public void onSwipeDelete(int position) {
                Object item = adapter.getItemAt(position);
                if (item instanceof Task) {
                    showDeleteConfirmationDialog((Task) item, position);
                }
            }

            @Override
            public void onSwipeEdit(int position) {
                Object item = adapter.getItemAt(position);
                if (item instanceof Task) {
                    navigateToEditTask((Task) item);
                    adapter.notifyItemChanged(position);
                }
            }
        }));
        itemTouchHelper.attachToRecyclerView(binding.taskRecycler);
    }

    private void toggleSelection(Task task) {
        adapter.toggleSelection(task.getId());
        int count = adapter.getSelectedTaskIds().size();

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
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.task_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Set<Integer> selectedIds = adapter.getSelectedTaskIds();
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete_selected) {
                viewModel.deleteTasks(selectedIds);
                Toast.makeText(requireContext(), "Deleting " + selectedIds.size() + " tasks", Toast.LENGTH_SHORT).show();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelections();
            actionMode = null;
        }
    };

    private void setupFloatingActionButton() {
        binding.addTaskFab.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), TaskActivity.class));
        });
    }

    private void setupFilterChips() {
        binding.chipClearFilters.setOnClickListener(v -> viewModel.clearFilters());

        binding.chipSort.setOnClickListener(this::showSortMenu);

        binding.chipPriority.setOnClickListener(v -> {
            Set<String> priorities = viewModel.getCurrentPriorities();
            binding.chipPriority.setChecked(priorities != null && !priorities.isEmpty());
            showPriorityMenu(v);
        });

        binding.chipStatus.setOnClickListener(v -> {
            Set<String> statuses = viewModel.getCurrentStatuses();
            binding.chipStatus.setChecked(statuses != null && !statuses.isEmpty());
            showStatusMenu(v);
        });

        binding.chipCategory.setOnClickListener(v -> {
            Set<String> cats = viewModel.getCurrentCategories();
            binding.chipCategory.setChecked(cats != null && !cats.isEmpty());
            showCategoryMenu(v);
        });
    }

    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 1, 0, "Title (Ascending)");
        popup.getMenu().add(0, 2, 0, "Title (Descending)");
        popup.getMenu().add(0, 3, 0, "Deadline (Earliest)");
        popup.getMenu().add(0, 4, 0, "Deadline (Latest)");
        popup.getMenu().add(0, 5, 0, "Priority (High to Low)");
        popup.getMenu().add(0, 6, 0, "Priority (Low to High)");
        popup.getMenu().add(0, 7, 0, "Recently Added");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: viewModel.setSortType(SortTask.TITLE_ASC); break;
                case 2: viewModel.setSortType(SortTask.TITLE_DESC); break;
                case 3: viewModel.setSortType(SortTask.DEADLINE_ASC); break;
                case 4: viewModel.setSortType(SortTask.DEADLINE_DESC); break;
                case 5: viewModel.setSortType(SortTask.PRIORITY_ASC); break;
                case 6: viewModel.setSortType(SortTask.PRIORITY_DESC); break;
                case 7: viewModel.setSortType(SortTask.CREATED_DESC); break;
            }
            return true;
        });
        popup.show();
    }

    private void showPriorityMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 0, 0, "All");
        popup.getMenu().add(0, 1, 0, "High");
        popup.getMenu().add(0, 2, 0, "Medium");
        popup.getMenu().add(0, 3, 0, "Low");

        popup.setOnMenuItemClickListener(item -> {
            String selected = item.getItemId() == 0 ? null : item.getTitle().toString();
            Set<String> priorities = new HashSet<>();
            if (selected != null) priorities.add(selected);
            viewModel.setFilters(priorities, viewModel.getCurrentStatuses(), viewModel.getCurrentCategories());
            return true;
        });
        popup.show();
    }

    private void showStatusMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 0, 0, "All");
        popup.getMenu().add(0, 1, 0, "Pending");
        popup.getMenu().add(0, 2, 0, "In Progress");
        popup.getMenu().add(0, 3, 0, "Completed");
        popup.getMenu().add(0, 4, 0, "Overdue");

        popup.setOnMenuItemClickListener(item -> {
            String selected = item.getItemId() == 0 ? null : item.getTitle().toString();
            Set<String> statuses = new HashSet<>();
            if (selected != null) statuses.add(selected);
            viewModel.setFilters(viewModel.getCurrentPriorities(), statuses, viewModel.getCurrentCategories());
            return true;
        });
        popup.show();
    }

    private void showCategoryMenu(View v) {
        PopupMenu popup = new PopupMenu(requireContext(), v);
        popup.getMenu().add(0, 0, 0, "All");
        
        for (String cat : availableCategories) {
            popup.getMenu().add(0, 1, 0, cat);
        }

        popup.setOnMenuItemClickListener(item -> {
            String selected = item.getItemId() == 0 ? null : item.getTitle().toString();
            Set<String> cats = new HashSet<>();
            if (selected != null) cats.add(selected);
            viewModel.setFilters(viewModel.getCurrentPriorities(), viewModel.getCurrentStatuses(), cats);
            return true;
        });
        popup.show();
    }

    private void observeViewModel() {
        // Show loading initially
        binding.progressBar.setVisibility(View.VISIBLE);

        viewModel.getTasksForUi().observe(getViewLifecycleOwner(), tasks -> {
            binding.progressBar.setVisibility(View.GONE);
            adapter.setGroupedTasks(tasks);
            binding.emptyView.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getUniqueCategories().observe(getViewLifecycleOwner(), cats -> {
            availableCategories = cats;
        });

        viewModel.getSelectedPrioritiesLive().observe(getViewLifecycleOwner(), this::updateFilterState);
        viewModel.getSelectedStatusesLive().observe(getViewLifecycleOwner(), this::updateFilterState);
        viewModel.getSelectedCategoriesLive().observe(getViewLifecycleOwner(), this::updateFilterState);
    }

    private void updateFilterState(Object ignored) {
        Set<String> priorities = viewModel.getCurrentPriorities();
        Set<String> statuses = viewModel.getCurrentStatuses();
        Set<String> categories = viewModel.getCurrentCategories();
        
        boolean hasFilter = (priorities != null && !priorities.isEmpty()) ||
                            (statuses != null && !statuses.isEmpty()) ||
                            (categories != null && !categories.isEmpty());

        binding.chipClearFilters.setVisibility(hasFilter ? View.VISIBLE : View.GONE);

        binding.chipPriority.setText((priorities == null || priorities.isEmpty()) ? "Priority" : "Priority: " + priorities.iterator().next() + (priorities.size() > 1 ? "..." : ""));
        binding.chipPriority.setChecked(priorities != null && !priorities.isEmpty());

        binding.chipStatus.setText((statuses == null || statuses.isEmpty()) ? "Status" : "Status: " + statuses.iterator().next() + (statuses.size() > 1 ? "..." : ""));
        binding.chipStatus.setChecked(statuses != null && !statuses.isEmpty());
        
        binding.chipCategory.setText((categories == null || categories.isEmpty()) ? "Category" : "Category: " + categories.iterator().next() + (categories.size() > 1 ? "..." : ""));
        binding.chipCategory.setChecked(categories != null && !categories.isEmpty());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.task_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem searchItem = menu.findItem(R.id.actionSearch);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.searchTasks(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_group) {
            showGroupDialog();
            return true;
        } else if (id == R.id.action_sync) {
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                SyncManager.getInstance().syncAll();
                Toast.makeText(requireContext(), "Sync started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Offline: Cannot sync now", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showGroupDialog() {
        String[] options = {"None", "By Category", "By Date", "By Priority"};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Group Tasks")
                .setItems(options, (dialog, which) -> viewModel.setGroupType(which))
                .show();
    }

    private void showDeleteConfirmationDialog(Task task, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.delete(task))
                .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    private void navigateToEditTask(Task task) {
        Intent intent = new Intent(requireContext(), TaskActivity.class);
        intent.putExtra("task_id", task.getId());
        startActivity(intent);
    }
    private void navigateToTaskDetails(Task task) {
        Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
        intent.putExtra("task_data", task);
        startActivity(intent);
    }
}
