package com.twa.taskmaster.ui.task;

import android.app.Activity;
import android.graphics.Gainmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.twa.taskmaster.R;
import com.twa.taskmaster.core.enums.RecurrenceRule;
import com.twa.taskmaster.core.util.DateTimeUtils;
import com.twa.taskmaster.domain.model.Task;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class RecurrenceHandler {
    private final TaskActivity activity;
    private final ChipGroup weeklyOptionsLayout;

    private RecurrenceRule selectedRule = RecurrenceRule.DAILY;

    public RecurrenceHandler(TaskActivity activity) {
        this.activity = activity;

        weeklyOptionsLayout = activity.findViewById(R.id.chipGroupWeekdays);
    }



}
