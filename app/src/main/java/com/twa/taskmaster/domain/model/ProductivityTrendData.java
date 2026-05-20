package com.twa.taskmaster.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductivityTrendData {
    private final List<DailyProductivity> dailyProductivity;

    public ProductivityTrendData(List<DailyProductivity> dailyProductivity) {
        this.dailyProductivity = dailyProductivity != null ? dailyProductivity : new ArrayList<>();
    }

    public List<DailyProductivity> getDailyProductivity() {
        return Collections.unmodifiableList(dailyProductivity);
    }

    public float getAverageCompletionRate() {
        if (dailyProductivity.isEmpty()) return 0f;

        float totalRate = 0f;
        for (DailyProductivity dp : dailyProductivity) {
            totalRate += dp.getCompletionRate();
        }
        return totalRate / dailyProductivity.size();
    }
}
