package com.twa.taskmaster.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.twa.taskmaster.databinding.DialogLogFilterBinding;
import com.twa.taskmaster.viewmodel.SharedTaskViewModel;

public class LogFilterDialogFragment extends BottomSheetDialogFragment {

    private DialogLogFilterBinding binding;
    private final SharedTaskViewModel viewModel;

    public LogFilterDialogFragment(SharedTaskViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public static LogFilterDialogFragment newInstance(SharedTaskViewModel viewModel) {
        return new LogFilterDialogFragment(viewModel);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogLogFilterBinding.inflate(LayoutInflater.from(getContext()));

        binding.radioAll.setChecked(true);

        binding.btnApply.setOnClickListener(v -> {
            String range = getSelectedDateRange();
            boolean showCompleted = binding.chkCompleted.isChecked();
            boolean showIncomplete = binding.chkIncomplete.isChecked();

            //viewModel.applyLogFilter(range, showCompleted, showIncomplete);
            dismiss();
        });

        binding.btnReset.setOnClickListener(v -> {
           // viewModel.applyLogFilter("ALL",true,true);
            dismiss();
        });

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setContentView(binding.getRoot());
        return dialog;
    }

    private String getSelectedDateRange() {
        if (binding.radioToday.isChecked()) return "TODAY";
        if (binding.radioWeek.isChecked()) return "WEEK";
        if (binding.radioMonth.isChecked()) return "MONTH";
        return "ALL";
    }
}

