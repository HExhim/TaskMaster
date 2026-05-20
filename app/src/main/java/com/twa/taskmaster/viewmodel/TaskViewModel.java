package com.twa.taskmaster.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.twa.taskmaster.core.enums.SortTask;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.data.repository.TaskLogRepository;
import com.twa.taskmaster.data.repository.TaskRepository;
import com.twa.taskmaster.core.enums.SyncState;
import com.twa.taskmaster.domain.model.GroupedTasks;
import com.twa.taskmaster.domain.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;

    private final LiveData<List<Task>> allTasks;

    private final MediatorLiveData<List<Object>> tasksForUi = new MediatorLiveData<>();

    // Filter states
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<SortTask> sortType = new MutableLiveData<>(SortTask.DEADLINE_ASC);
    private final MutableLiveData<Set<String>> selectedCategories = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Set<String>> selectedPriorities = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Set<String>> selectedStatuses = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<GroupType> groupType = new MutableLiveData<>(GroupType.NONE);
    private final MutableLiveData<SyncState> syncState = new MutableLiveData<>(SyncState.SYNCED);

    public LiveData<List<Object>> getTasksForUi() {
        return tasksForUi;
    }

    public LiveData<List<String>> getUniqueCategories() {
        return androidx.lifecycle.Transformations.map(allTasks, tasks -> {
            if (tasks == null) return Collections.emptyList();
            return tasks.stream()
                    .map(Task::getCategory)
                    .filter(c -> c != null && !c.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        });
    }

    public enum GroupType {
        NONE,
        CATEGORY,
        DATE,
        PRIORITY,
        STATUS
    }

    public TaskViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        taskLogRepository = new TaskLogRepository(application);

        allTasks = taskRepository.getAllTasks();

        // Setup mediator live data sources
        setupFilteredTasksObserver();
    }

    private void setupFilteredTasksObserver() {
        tasksForUi.addSource(allTasks, tasks -> applyFilters());
        tasksForUi.addSource(searchQuery, query -> applyFilters());
        tasksForUi.addSource(sortType, sort -> applyFilters());
        tasksForUi.addSource(selectedCategories, categories -> applyFilters());
        tasksForUi.addSource(selectedPriorities, priorities -> applyFilters());
        tasksForUi.addSource(selectedStatuses, statuses -> applyFilters());
        tasksForUi.addSource(groupType, group -> applyFilters());
    }


    // ========== CRUD Operations ==========
    public void insert(Task task) {
        taskRepository.insertTask(task);
         }

    public void update(Task task) {
        taskRepository.update(task);

    }

    public void delete(Task task) {
        taskRepository.delete(task);
        taskLogRepository.deleteLogsforTask(task.getId());
    }

    public void deleteTasks(Set<Integer> taskIds) {

        taskRepository.deleteTasksById(new ArrayList<>(taskIds));
        taskLogRepository.deleteLogsforTasks(new ArrayList<>(taskIds));
    }

    // ========== Search/Filter/Sort/Group Methods ==========
    public void searchTasks(String query) {
        searchQuery.setValue(query != null ? query.trim().toLowerCase() : "");
    }

    public void setGroupType(int type) {
        GroupType group;
        switch (type) {
            case 1: group = GroupType.CATEGORY; break;
            case 2: group = GroupType.DATE; break;
            case 3: group = GroupType.PRIORITY; break;
            case 4: group = GroupType.STATUS; break;
            default: group = GroupType.NONE; break;
        }
        groupType.setValue(group);
    }

    public void setSortType(SortTask sort) {
        sortType.setValue(sort);
    }

    public void clearFilters() {
        searchQuery.setValue("");
        selectedCategories.setValue(new HashSet<>());
        selectedPriorities.setValue(new HashSet<>());
        selectedStatuses.setValue(new HashSet<>());
        sortType.setValue(SortTask.CREATED_DESC);
        groupType.setValue(GroupType.NONE);
    }

    public void setFilters(Set<String> priorities, Set<String> statuses, Set<String> categories) {
        selectedPriorities.setValue(priorities);
        selectedStatuses.setValue(statuses);
        selectedCategories.setValue(categories);
    }

    public Set<String> getCurrentPriorities() { return selectedPriorities.getValue(); }
    public Set<String> getCurrentStatuses() { return selectedStatuses.getValue(); }
    public Set<String> getCurrentCategories() { return selectedCategories.getValue(); }

    public LiveData<Set<String>> getSelectedPrioritiesLive() { return selectedPriorities; }
    public LiveData<Set<String>> getSelectedStatusesLive() { return selectedStatuses; }
    public LiveData<Set<String>> getSelectedCategoriesLive() { return selectedCategories; }

    private void applyFilters() {
        List<Task> baseList = allTasks.getValue();

        if (baseList == null) {
            tasksForUi.setValue(Collections.emptyList());
            return;
        }

        List<Task> filteredList = new ArrayList<>(baseList);

        // Apply search filter
        String query = searchQuery.getValue();
        if (query != null && !query.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(task -> matchesSearchQuery(task, query))
                    .collect(Collectors.toList());
        }

        // Apply category filter
        Set<String> categories = selectedCategories.getValue();
        if (categories != null && !categories.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(task -> task.getCategory() != null && categories.contains(task.getCategory()))
                    .collect(Collectors.toList());
        }

        // Apply priority filter
        Set<String> priorities = selectedPriorities.getValue();
        if (priorities != null && !priorities.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(task -> task.getPriority() != null && priorities.contains(task.getPriority()))
                    .collect(Collectors.toList());
        }

        // Apply status filter
        Set<String> statuses = selectedStatuses.getValue();
        if (statuses != null && !statuses.isEmpty()) {
            filteredList = filteredList.stream()
                    .filter(task -> {
                        String status = "Pending";
                        if (task.isCompleted()) {
                            status = "Completed";
                        } else if (task.getDeadline() > 0 && System.currentTimeMillis() > task.getDeadline()) {
                            status = "Overdue";
                        } else if (task.getTimeSpent() > 0) {
                            status = "In Progress";
                        }
                        return statuses.contains(status);
                    })
                    .collect(Collectors.toList());
        }

        // Apply sorting
        SortTask currentSort = sortType.getValue();
        if (currentSort != null) {
            Comparator<Task> comparator = getComparatorForSortType(currentSort);
            filteredList.sort(comparator);
        }

        GroupType group = groupType.getValue();
        if (group != null && group != GroupType.NONE) {
            List<GroupedTasks> groupedList = groupTasks(filteredList, group);
            List<Object> flattenedList = new ArrayList<>();
            for (GroupedTasks g : groupedList) {
                flattenedList.add(g.getGroupTitle());
                flattenedList.addAll(g.getTasks());
            }
            tasksForUi.setValue(flattenedList);
        } else {
            tasksForUi.setValue(new ArrayList<>(filteredList));
        }
    }

    private List<GroupedTasks> groupTasks(List<Task> taskList, GroupType groupType) {
        if (taskList == null || taskList.isEmpty()) return Collections.emptyList();

        Map<String, List<Task>> groupedMap = new LinkedHashMap<>();

        for (Task task : taskList) {
            String key;
            switch (groupType) {
                case CATEGORY:
                    key = task.getCategory() != null ? task.getCategory() : "Uncategorized";
                    break;
                case PRIORITY:
                    key = task.getPriority() != null ? task.getPriority() : "No Priority";
                    break;
                case DATE:
                    key = task.getDeadline() > 0 ? DateTimeUtils.formatDate(task.getDeadline()) : "No Deadline";
                    break;
                case STATUS:
                    key = task.getStatus() != null ? task.getStatus() : "No Status";
                    break;
                default:
                    key = "Ungrouped";
            }
            groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(task);
        }

        List<GroupedTasks> result = new ArrayList<>();
        for (Map.Entry<String, List<Task>> entry : groupedMap.entrySet()) {
            result.add(new GroupedTasks(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private boolean matchesSearchQuery(Task task, String query) {
        return task.getTitle().toLowerCase().contains(query) ||
                (task.getDescription() != null && task.getDescription().toLowerCase().contains(query)) ||
                (task.getCategory() != null && task.getCategory().toLowerCase().contains(query));
    }

    private Comparator<Task> getComparatorForSortType(SortTask sortType) {
        switch (sortType) {
            case TITLE_ASC:
                return Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
            case TITLE_DESC:
                return Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER).reversed();
            case DEADLINE_ASC:
                return Comparator.comparing(Task::getDeadline);
            case DEADLINE_DESC:
                return Comparator.comparing(Task::getDeadline).reversed();
            case PRIORITY_ASC:
                return Comparator.comparing(task -> {
                    if (task.getPriority() == null) return 4;
                    switch (task.getPriority()) {
                        case "High": return 1;
                        case "Medium": return 2;
                        case "Low": return 3;
                        default: return 4;
                    }
                });
            case PRIORITY_DESC:
                return Comparator.<Task, Integer>comparing(task -> {
                    if (task.getPriority() == null) return 4;
                    switch (task.getPriority()) {
                        case "High": return 1;
                        case "Medium": return 2;
                        case "Low": return 3;
                        default: return 4;
                    }
                }).reversed();
            case CREATED_DESC:
                 return Comparator.comparing(Task::getCreatedAt).reversed();
            default:
                return Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
        }
    }
}
