package com.twa.taskmaster.domain;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.twa.taskmaster.R;
import com.twa.taskmaster.ui.task.TaskSwipeListener;

public class TaskTouchCallback extends ItemTouchHelper.SimpleCallback {

    private final TaskSwipeListener listener;
    private final Paint paint = new Paint();

    public TaskTouchCallback(TaskSwipeListener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.listener = listener;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (direction == ItemTouchHelper.LEFT) {
            listener.onSwipeDelete(position);
        } else if (direction == ItemTouchHelper.RIGHT) {
            listener.onSwipeEdit(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        Context context = itemView.getContext();

        int itemHeight = itemView.getHeight();

        Drawable icon;
        int iconTop, iconLeft, iconRight, iconBottom;
        int iconMargin;

        if (dX > 0) {
            // Swiping Right (Edit)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_edit);
            if (icon != null) {
                paint.setColor(ContextCompat.getColor(context, R.color.green_800));
                canvas.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), paint);

                iconMargin = (itemHeight - icon.getIntrinsicHeight()) / 4;
                iconTop = itemView.getTop() + (itemHeight - icon.getIntrinsicHeight()) / 2;
                iconLeft = itemView.getLeft() + iconMargin;
                iconRight = iconLeft + icon.getIntrinsicWidth();
                iconBottom = iconTop + icon.getIntrinsicHeight();

                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                icon.draw(canvas);
            }

        } else if (dX < 0) {
            // Swiping Left (Delete)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
            if (icon != null) {
                paint.setColor(ContextCompat.getColor(context, R.color.red_800));
                canvas.drawRect(itemView.getRight() + dX, itemView.getTop(),
                        itemView.getRight(), itemView.getBottom(), paint);

                iconMargin = (itemHeight - icon.getIntrinsicHeight()) / 4;
                iconTop = itemView.getTop() + (itemHeight - icon.getIntrinsicHeight()) / 2;
                iconRight = itemView.getRight() - iconMargin;
                iconLeft = iconRight - icon.getIntrinsicWidth();
                iconBottom = iconTop + icon.getIntrinsicHeight();

                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                icon.draw(canvas);
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }


}
