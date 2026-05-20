package com.twa.taskmaster.ui.dialogs;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.twa.taskmaster.R;
import com.twa.taskmaster.data.local.dao.CategoryDao;
import com.twa.taskmaster.data.local.database.Database;
import com.twa.taskmaster.data.local.entity.CategoryEntity;
import com.twa.taskmaster.databinding.DialogAddCategoryBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AddCategoryDialogFragment extends DialogFragment {
    private CategoryAddListener listener;

    private DialogAddCategoryBinding binding;
    private int selectedColor = Color.parseColor("#E91E63");
    private CategoryDao categoryDao;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private List<Integer> usedColors = new ArrayList<>();

    public void setUsedColors(List<Integer> colors) {
        this.usedColors = colors;
    }

    private static final int[] COLOR_PALETTE = {
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#673AB7"), // Deep Purple
            Color.parseColor("#3F51B5"), // Indigo
            Color.parseColor("#03A9F4"), // Light Blue
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#009688"), // Teal
            Color.parseColor("#8BC34A"), // Light Green
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#FF5722"), // Deep Orange
            Color.parseColor("#795548"), // Brown
            Color.parseColor("#607D8B")  // Blue Grey
    };

    public interface CategoryAddListener {
        void onCategoryAdded(String categoryName, int color);
    }

    public void setCategoryAddListener(CategoryAddListener listener) {
        this.listener = listener;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogAddCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categoryDao = Database.getInstance(requireContext()).categoryDao();

        // Initialize UI
        initializeColorPreview();
        setupButtons();
    }

    private void initializeColorPreview() {
        // Set initial color
        updateColorPreview();

        binding.colorPickerButton.setOnClickListener(v -> showColorPickerDialog());
    }

    private void updateColorPreview() {
        // Create a new ColorDrawable to ensure proper color application
        binding.colorPreview.setBackground(new ColorDrawable(selectedColor));

        // Force redraw
        binding.colorPreview.invalidate();

        Log.d("ColorPreview", "Updating preview with color: " + selectedColor);
    }

    private void showColorPickerDialog() {
        // Filter out colors already used
        int[] availableColors = Arrays.stream(COLOR_PALETTE)
                .filter(color -> !usedColors.contains(color))
                .toArray();

        ColorPickerDialogFragment dialog = new ColorPickerDialogFragment();
        dialog.setColors(availableColors);
        dialog.setColorSelectionListener(color -> {
            selectedColor = color;
            Log.d("ColorPicker", "Selected color: " + color);
            updateColorPreview();
        });
        dialog.show(getParentFragmentManager(), "ColorPickerDialog");
    }


    private void setupButtons() {
        binding.saveButton.setOnClickListener(v -> saveCategory());
    }

    private void saveCategory() {
        String name = binding.categoryNameInput.getText().toString().trim();

        if (name.isEmpty()) {
            binding.categoryNameInput.setError(getString(R.string.category_name_required));
            return;
        }

        if (usedColors.contains(selectedColor)) {
            Toast.makeText(getContext(),
                    "This color is already assigned to another category",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        CategoryEntity category = new CategoryEntity();
        category.setName(name);
        category.setColor(String.format("#%06X", (0xFFFFFF & selectedColor)));

        listener.onCategoryAdded(name, selectedColor);
        executor.execute(() -> {
            categoryDao.insert(category);
            requireActivity().runOnUiThread(this::dismiss);
        });

        Log.d("CategoryDialog","Category Saved");
        Toast.makeText(getContext(), "Category Created", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}