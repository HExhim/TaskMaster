package com.twa.taskmaster.ui.task.logs;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.twa.taskmaster.core.enums.TimePeriod;

public class TaskLogsPagerAdapter extends FragmentStateAdapter {

    private final TimePeriod[] filterTypes = {
            TimePeriod.ALL,
            TimePeriod.TODAY,
            TimePeriod.WEEK,
            TimePeriod.MONTH
    };

    public TaskLogsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return LogListFragment.newInstance(filterTypes[position]);
    }

    @Override
    public int getItemCount() {
        return filterTypes.length;
    }

    public TimePeriod getFilterType(int position) {
        if (position >= 0 && position < filterTypes.length) {
            return filterTypes[position];
        }
        return TimePeriod.ALL;
    }
}
