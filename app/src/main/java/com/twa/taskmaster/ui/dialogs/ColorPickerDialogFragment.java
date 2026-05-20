package com.twa.taskmaster.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.twa.taskmaster.R;

public class ColorPickerDialogFragment extends DialogFragment {
    private ColorSelectionListener listener;
    private int[] colors;

    public interface ColorSelectionListener {
        void onColorSelected(int color);
    }

    public void setColorSelectionListener(ColorSelectionListener listener) {
        this.listener = listener;
    }

    public void setColors(int[] colors) {
        this.colors = colors;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_color_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GridLayout colorGrid = view.findViewById(R.id.color_grid);
        colorGrid.removeAllViews();

        for (int color : colors) {
            ImageButton colorButton = new ImageButton(requireContext());

            // Set button params
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 64;
            params.height = 64;
            params.setMargins(8, 8, 8, 8);
            colorButton.setLayoutParams(params);

            // Style the button
            colorButton.setBackgroundColor(color);


            // Add click listener
            colorButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onColorSelected(color);
                    Log.d("ColorPicker","Selected color: "+ color);
                }
                dismiss();
            });

            colorGrid.addView(colorButton);
        }
    }
}