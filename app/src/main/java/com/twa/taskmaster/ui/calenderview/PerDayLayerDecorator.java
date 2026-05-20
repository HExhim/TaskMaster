package com.twa.taskmaster.ui.calenderview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;
import com.twa.taskmaster.domain.model.Task;

import java.util.List;

public class PerDayLayerDecorator implements DayViewDecorator {

    public static class CalendarEvent {
        public static final int DEADLINE = Color.parseColor("#EF5350"); // Red
        public final int color;
        public final Task task;

        public CalendarEvent(Task task,int color) {
            this.task = task;
            this.color = color;
        }
    }

    private final CalendarDay targetDay;
    private final List<CalendarEvent> events;

    public PerDayLayerDecorator(CalendarDay targetDay, List<CalendarEvent> events) {
        this.targetDay = targetDay;
        this.events = events;
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(targetDay);
    }

    @Override
    public void decorate(DayViewFacade view) {
        if (events != null && !events.isEmpty()) {
            view.addSpan(new MultiDotSpan(5f, events));
        }
    }

    private static class MultiDotSpan extends DotSpan {
        private final List<CalendarEvent> events;
        private final float radius;

        public MultiDotSpan(float radius, List<CalendarEvent> events) {
            this.radius = radius;
            this.events = events;
        }

        @Override
        public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom, CharSequence charSequence, int start, int end, int lineNum) {
            int count = Math.min(events.size(), 4); // Max 4 dots
            if (count == 0) return;

            float spacing = 10f;
            float totalWidth = (count * radius * 2) + ((count - 1) * spacing);
            float centerX = (left + right) / 2f;
            float startX = centerX - (totalWidth / 2f) + radius;
            // Draw below the text. 'bottom' is the bottom of the text line.
            float centerY = bottom + radius + 12; 

            int oldColor = paint.getColor();

            for (int i = 0; i < count; i++) {

                paint.setColor(events.get(i).color);
                
                float cx = startX + (i * (radius * 2 + spacing));
                canvas.drawCircle(cx, centerY, radius, paint);
            }

            paint.setColor(oldColor);
        }
    }
}
