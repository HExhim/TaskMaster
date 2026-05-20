package com.twa.taskmaster.ui.timer;

public class Lap {
    private final int lapNumber;
    private final String lapTime;
    private final String totalTime;

    public Lap(int lapNumber, String lapTime, String totalTime) {
        this.lapNumber = lapNumber;
        this.lapTime = lapTime;
        this.totalTime = totalTime;
    }

    public int getLapNumber() {
        return lapNumber;
    }

    public String getLapTime() {
        return lapTime;
    }

    public String getTotalTime() {
        return totalTime;
    }
}
