package com.twa.taskmaster.domain.model;

public class TimeSpentData {
    private final float workHours;
    private final float personalHours;
    private final float otherHours;

    public TimeSpentData(float workHours, float personalHours, float otherHours) {
        this.workHours = Math.max(workHours, 0);
        this.personalHours = Math.max(personalHours, 0);
        this.otherHours = Math.max(otherHours, 0);
    }

    public float getWorkHours() { return workHours; }
    public float getPersonalHours() { return personalHours; }
    public float getOtherHours() { return otherHours; }

    // Derived: Total Hours
    public float getTotalHours() {
        return workHours + personalHours + otherHours;
    }

    // Derived: Work Percentage
    public float getWorkPercentage() {
        float total = getTotalHours();
        return total > 0 ? (workHours / total) * 100f : 0f;
    }

    // Derived: Personal Percentage
    public float getPersonalPercentage() {
        float total = getTotalHours();
        return total > 0 ? (personalHours / total) * 100f : 0f;
    }

    // Derived: Other Percentage
    public float getOtherPercentage() {
        float total = getTotalHours();
        return total > 0 ? (otherHours / total) * 100f : 0f;
    }
}
