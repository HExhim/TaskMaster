package com.twa.taskmaster.ui.main_activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.twa.taskmaster.core.util.ThemeHelper;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this); // Apply the theme before calling super
        super.onCreate(savedInstanceState);
    }
}
