package com.twa.taskmaster.ui.task;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TimePicker;

import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.ui.dialogs.AddCategoryDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskUiBinder {

    private final TaskActivity activity;
    private final TextInputEditText edtTitle, edtDescription, edtStartDateTime, edtDueDateTime;
    private MaterialSwitch switchReminder;
    private TimePicker reminderTimePicker;
    private final MaterialButton btnSaveTask;
    private final Spinner prioritySpinner;
    private final ChipGroup categoryGroup;
    private final ChipGroup chipGroupWeekdays;

    private final List<String> categories = new ArrayList<>();
    private final Map<String, Integer> categoryColors = new HashMap<>();

    private String selectedCategory = null;

    private final Map<String, Integer> DEFAULT_CATEGORIES = new HashMap<>();

    public TaskUiBinder(TaskActivity activity) {
        this.activity = activity;

        edtTitle = activity.findViewById(R.id.edtTitle);
        edtDescription = activity.findViewById(R.id.edtDescription);
        edtStartDateTime = activity.findViewById(R.id.edtStartDateTime);
        edtDueDateTime = activity.findViewById(R.id.edtDueDateTime);
        prioritySpinner = activity.findViewById(R.id.autoCompletePriority);
        categoryGroup = activity.findViewById(R.id.categoryGroup);
        switchReminder = activity.findViewById(R.id.switchReminder);
        reminderTimePicker = activity.findViewById(R.id.reminderTimePicker);
        chipGroupWeekdays = activity.findViewById(R.id.chipGroupWeekdays);

        btnSaveTask = activity.findViewById(R.id.btnSaveTask);

        initDefaultCategories();
        setupSpinners();
        setupCategoryChips();
        setupDatePickers();
        setupReminder();

    }

    private void setupReminder() {
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                reminderTimePicker.setVisibility(View.VISIBLE);
            } else {
                reminderTimePicker.setVisibility(View.GONE);
            }
        });


    }


    // --------------------------------------
    // Date Pickers
    // --------------------------------------
    private void setupDatePickers() {
        edtStartDateTime.setOnClickListener(v -> showDateTimePicker(edtStartDateTime));
        edtDueDateTime.setOnClickListener(v -> showDateTimePicker(edtDueDateTime));
    }

    private void showDateTimePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();

        try {
            if (!target.getText().toString().isEmpty()) {
                long millis = DateTimeUtils.convertToTimestamp(target.getText().toString());
                calendar.setTimeInMillis(millis);
            }
        } catch (ParseException e) {
            Log.e("DatePicker", "Invalid date format", e);
        }

        DatePickerDialog datePicker = new DatePickerDialog(
                activity,
                (view, y, m, d) -> {
                    calendar.set(y, m, d);
                    showTimePicker(target, calendar);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePicker.show();
    }

    private void showTimePicker(TextInputEditText target, Calendar calendar) {
        new TimePickerDialog(
                activity,
                (view, h, m) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, h);
                    calendar.set(Calendar.MINUTE, m);

                    target.setText(DateTimeUtils.formatDateTime(calendar.getTimeInMillis()));
                    target.setError(null);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        ).show();
    }

    // --------------------------------------
    // Category Management
    // --------------------------------------
    private void initDefaultCategories() {
        DEFAULT_CATEGORIES.put("Home", Color.parseColor("#F44336"));
        DEFAULT_CATEGORIES.put("Work", Color.parseColor("#536DFE"));
        DEFAULT_CATEGORIES.put("Personal", Color.parseColor("#4CAF50"));
        DEFAULT_CATEGORIES.put("Finance", Color.parseColor("#FFC107"));
        DEFAULT_CATEGORIES.put("Health", Color.parseColor("#E040FB"));
        DEFAULT_CATEGORIES.put("Education", Color.parseColor("#00BCD4"));

        categories.addAll(DEFAULT_CATEGORIES.keySet());
        categoryColors.putAll(DEFAULT_CATEGORIES);
    }

    private void setupCategoryChips() {
        categoryGroup.removeAllViews();

        for (String category : categories) {
            addCategoryChip(category, categoryColors.get(category));
        }

        addAddCategoryChip();

        if (!categories.isEmpty()) {
            selectedCategory = categories.get(0);
            checkCategory(selectedCategory);
        }
    }

    private void addCategoryChip(String name, int color) {
        Chip chip = new Chip(activity, null, com.google.android.material.R.style.Widget_Material3_Chip_Filter);
        chip.setText(name);
        chip.setCheckable(true);
        chip.setChipBackgroundColor(ColorStateList.valueOf(color));

        chip.setOnClickListener(v -> {
            selectedCategory = name;
            checkCategory(name);
        });

        int addChipIndex = getAddChipIndex();
        categoryGroup.addView(chip, addChipIndex);
    }

    private void addAddCategoryChip() {
        Chip addChip = new Chip(activity);
        addChip.setId(R.id.addNewChip);
        addChip.setText("+ New");
        addChip.setCheckable(false);

        addChip.setOnClickListener(v -> showAddCategoryDialog());
        categoryGroup.addView(addChip);
    }

    private int getAddChipIndex() {
        View addChip = categoryGroup.findViewById(R.id.addNewChip);
        return addChip == null ? categoryGroup.getChildCount() : categoryGroup.indexOfChild(addChip);
    }

    private void showAddCategoryDialog() {
        AddCategoryDialogFragment dialog = new AddCategoryDialogFragment();
        dialog.setCategoryAddListener((name, color) -> {
            categories.add(name);
            categoryColors.put(name, color);
            addCategoryChip(name, color);
            checkCategory(name, name);
        });
        dialog.show(activity.getSupportFragmentManager(), "AddCategoryDialog");
    }
    
    private void checkCategory(String name) {
        checkCategory(name, null);
    }

    private void checkCategory(String name, String fallback) {
        boolean found = false;
        for (int i = 0; i < categoryGroup.getChildCount(); i++) {
            View v = categoryGroup.getChildAt(i);
            if (v instanceof Chip) {
                Chip chip = (Chip) v;
                boolean isMatch = chip.getText().toString().equals(name);
                chip.setChecked(isMatch);
                if (isMatch) found = true;
            }
        }
        
        // If we are setting a category (like from existing task) and it's not in the list, maybe add it?
        // But for now assuming standard behavior.
    }
    
    public void selectCategory(String category) {
        if (category == null) return;
        // If category exists, select it. If not, maybe add it?
        // For simplicity, let's try to find it.
        if (!categories.contains(category)) {
            categories.add(category);
            categoryColors.put(category, Color.GRAY); // Default color
            addCategoryChip(category, Color.GRAY);
        }
        checkCategory(category);
        selectedCategory = category;
    }


    // --------------------------------------
    // Priority Spinner
    // --------------------------------------
    private void setupSpinners() {
        String[] priorities = {"Low", "Medium", "High"};

        prioritySpinner.setAdapter(new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                priorities
        ));
    }

    // --------------------------------------
    // Public Getters
    // --------------------------------------
    public MaterialButton getBtnSaveTask() {
        return btnSaveTask;
    }

    public TextInputEditText getEdtTitle() {
        return edtTitle;
    }

    public TextInputEditText getEdtDescription() {
        return edtDescription;
    }

    public Spinner getPrioritySpinner() {
        return prioritySpinner;
    }

    public TextInputEditText getEdtStartDateTime() {
        return edtStartDateTime;
    }

    public TextInputEditText getEdtDueDateTime() {
        return edtDueDateTime;
    }
    public TimePicker getReminderTimePicker() {
        return reminderTimePicker;
    }

    public MaterialSwitch getSwitchReminder() {
        return switchReminder;
    }

    public ChipGroup getChipGroupWeekdays() {
        return chipGroupWeekdays;
    }
    
    public String getSelectedCategory() {
        return selectedCategory;
    }
}
