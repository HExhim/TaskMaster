package com.twa.taskmaster.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.twa.taskmaster.R;
import com.twa.taskmaster.data.local.entity.TaskLogEntity;
import com.twa.taskmaster.viewmodel.TaskLogViewModel;

public class AddLogDialogFragment extends DialogFragment {

    private static final String ARG_TASK_ID = "task_id";
    private TextInputEditText etDuration;
    private TaskLogViewModel taskLogViewModel;

    public static AddLogDialogFragment newInstance(long taskId) {
        AddLogDialogFragment fragment = new AddLogDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID, taskId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_log, null);

        taskLogViewModel = new ViewModelProvider(requireActivity()).get(TaskLogViewModel.class);


        // Initialize views and set up date/time pickers
        EditText etNotes = view.findViewById(R.id.etNotes);
        TextInputLayout tilDuration = view.findViewById(R.id.tilDuration);
        etDuration = view.findViewById(R.id.etDuration);

        // Set up duration input
        etDuration.setOnClickListener(v -> showDurationPicker());

        builder.setView(view)
                .setTitle("Add Time Log")
                .setPositiveButton("Save", (dialog, which) -> {
                    String notes = etNotes.getText().toString();
                    String duration = etDuration.getText().toString();
                    // Validate and save log
                    saveLog(notes, duration);
                })
                .setNegativeButton("Cancel", null);

        return builder.create();
    }

    private void showDurationPicker() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_duration_picker, null);

        NumberPicker hourPicker = dialogView.findViewById(R.id.hour_picker);
        NumberPicker minutePicker = dialogView.findViewById(R.id.minute_picker);

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(23);
        hourPicker.setValue(1);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setValue(0);

        new MaterialAlertDialogBuilder(getContext(), com.google.android.material.R.style.MaterialAlertDialog_Material3)
                .setTitle("Select Duration")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int selectedHours = hourPicker.getValue();
                    int selectedMinutes = minutePicker.getValue();

                    String formattedDuration = "";
                    if (selectedHours > 0) {
                        formattedDuration += selectedHours + "h ";
                    }
                    if (selectedMinutes > 0 || selectedHours == 0) {
                        formattedDuration += selectedMinutes + "m";
                    }

                    // Set duration in EditText
                    etDuration.setText(formattedDuration.trim());
                    long durationMillis = (selectedHours * 60L + selectedMinutes) * 60_000;

                    // Use the result as needed (e.g. assign to task duration)
                    Toast.makeText(getContext(),
                            "Duration: " + selectedHours + "h " + selectedMinutes + "m",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void saveLog(String notes, String durationStr) {
        long taskId = getArguments().getLong(ARG_TASK_ID);

        // Parse duration string like "2h 30m" to total minutes
        int totalMinutes = 0;
        try {
            if (durationStr.contains("h")) {
                int hours = Integer.parseInt(durationStr.split("h")[0].trim());
                totalMinutes += hours * 60;
                durationStr = durationStr.substring(durationStr.indexOf("h") + 1).trim();
            }
            if (durationStr.contains("m")) {
                int minutes = Integer.parseInt(durationStr.split("m")[0].trim());
                totalMinutes += minutes;
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Invalid duration format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create TaskLogEntity
        TaskLogEntity log = new TaskLogEntity();
        log.setTaskId((int) taskId);
        log.setTimestamp(System.currentTimeMillis());
        log.setEndTimeMillis(System.currentTimeMillis() + totalMinutes * 60_000L);
        log.setDurationMinutes(totalMinutes);
        log.setNote(notes);
        log.setSource("Manual"); // You can tag this however you like

        taskLogViewModel.insertLog(log); // <- Send to ViewModel

        Toast.makeText(getContext(), "Log saved", Toast.LENGTH_SHORT).show();
    }

}
