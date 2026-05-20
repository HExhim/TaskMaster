package com.twa.taskmaster.ui.calenderview;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.domain.model.Task;
import com.twa.taskmaster.ui.task.details.TaskDetailActivity;
import com.twa.taskmaster.viewmodel.CalendarViewModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private CalendarViewModel viewModel;
    private Map<LocalDate, List<Task>> taskMap = new HashMap<>();

    public CalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarView = view.findViewById(R.id.calendarView);
        calendarView.setSelectedDate(CalendarDay.today());

        viewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);

        observeTasks();

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            LocalDate selectedLocalDate = DateTimeUtils.toLocalDate(date.getDate());
            List<Task> tasksForDate = taskMap.getOrDefault(selectedLocalDate, new ArrayList<>());

            updateTaskListUI(tasksForDate);
        });
    }

    private void observeTasks() {
        viewModel.getAllTasks().observe(getViewLifecycleOwner(), this::decorateCalendar);
    }

    private void decorateCalendar(List<Task> tasks) {
        taskMap.clear();
        calendarView.removeDecorators(); // Clear old decorators

        Map<LocalDate, List<PerDayLayerDecorator.CalendarEvent>> eventsMap = new HashMap<>();

        for (Task task : tasks) {
            // 1. Handle Deadline
            if (task.getDeadline() > 0) {
                LocalDate deadlineDate = DateTimeUtils.toLocalDate(task.getDeadline());
                if (deadlineDate != null) {
                    // Add task to map for list view
                    List<Task> dayTasks = taskMap.computeIfAbsent(deadlineDate, k -> new ArrayList<>());
                    if (!dayTasks.contains(task)) {
                        dayTasks.add(task);
                    }

                    // Add event for decorator
                    eventsMap.computeIfAbsent(deadlineDate, k -> new ArrayList<>())
                            .add(new PerDayLayerDecorator.CalendarEvent(task, PerDayLayerDecorator.CalendarEvent.DEADLINE));
                }
            }

        }

        List<com.prolificinteractive.materialcalendarview.DayViewDecorator> decorators = new ArrayList<>();

        for (Map.Entry<LocalDate, List<PerDayLayerDecorator.CalendarEvent>> entry : eventsMap.entrySet()) {
            LocalDate date = entry.getKey();
            List<PerDayLayerDecorator.CalendarEvent> events = entry.getValue();

            // Create a specific decorator for this day
            // Note: CalendarDay months are 0-based, LocalDate is 1-based
            CalendarDay day = CalendarDay.from(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());

            decorators.add(new PerDayLayerDecorator(day, events));
        }

        calendarView.addDecorators(decorators);
        calendarView.invalidateDecorators();

        // Refresh UI for currently selected date
        CalendarDay selectedDate = calendarView.getSelectedDate();
        if (selectedDate != null) {
            LocalDate localDate = DateTimeUtils.toLocalDate(selectedDate.getDate());
            updateTaskListUI(taskMap.getOrDefault(localDate, new ArrayList<>()));
        }
    }

    private void updateTaskListUI(List<Task> tasks) {
        RecyclerView recyclerView = requireView().findViewById(R.id.taskRecyclerView);
        TextView noTasksText = requireView().findViewById(R.id.noTasks);

        if (tasks.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            noTasksText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            noTasksText.setVisibility(View.GONE);

            TaskCalendarAdapter adapter = new TaskCalendarAdapter(requireContext(), tasks, this::showTaskDetail);

            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
    }

    private void showTaskDetail(Task task) {
        Intent intent = new Intent(this.getActivity(), TaskDetailActivity.class);
        intent.putExtra("task_data", task);
        startActivity(intent);
    }

}
