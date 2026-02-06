package app.mynote.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import app.mynote.core.MyNote;
import app.mynote.fragments.note.NotesAdapter;
import mynote.R;

public abstract class SwipeController extends ItemTouchHelper.SimpleCallback {

    public static final int BUTTON_WIDTH = 190;
    private static RecyclerView recyclerView;
    private final GestureDetector gestureDetector;
    private final Map<Integer, List<UnderlayButton>> buttonsBuffer;
    private final Queue<Integer> recoverQueue;
    private List<UnderlayButton> buttons;
    private final GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            for (UnderlayButton button : buttons) {
                if (button.onClick(e.getX(), e.getY()))
                    break;
            }

            return true;
        }
    };
    private int swipedPos = -1;
    private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent e) {
            if (swipedPos < 0) return false;
            Point point = new Point((int) e.getRawX(), (int) e.getRawY());

            RecyclerView.ViewHolder swipedViewHolder = recyclerView.findViewHolderForAdapterPosition(swipedPos);
            if (swipedViewHolder !=null) {
                View swipedItem = swipedViewHolder.itemView;
                Rect rect = new Rect();
                swipedItem.getGlobalVisibleRect(rect);

                if (e.getAction() == MotionEvent.ACTION_DOWN || e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_MOVE) {
                    if (rect.top < point.y && rect.bottom > point.y)
                        gestureDetector.onTouchEvent(e);
                    else {
                        recoverQueue.add(swipedPos);
                        swipedPos = -1;
                        recoverSwipedItem();
                    }
                }
            }

            return false;
        }
    };
    private float swipeThreshold = 0.5f;

    public SwipeController(Context context, RecyclerView recyclerView) {
        super(0, ItemTouchHelper.LEFT);
        SwipeController.recyclerView = recyclerView;
        this.buttons = new ArrayList<>();
        this.gestureDetector = new GestureDetector(context, gestureListener);
        SwipeController.recyclerView.setOnTouchListener(onTouchListener);
        buttonsBuffer = new HashMap<>();
        recoverQueue = new LinkedList<Integer>() {
            @Override
            public boolean add(Integer o) {
                if (contains(o))
                    return false;
                else
                    return super.add(o);
            }
        };

        attachSwipe();
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();

        if (swipedPos != pos)
            recoverQueue.add(swipedPos);

        swipedPos = pos;

        if (buttonsBuffer.containsKey(swipedPos))
            buttons = buttonsBuffer.get(swipedPos);
        else
            buttons.clear();

        buttonsBuffer.clear();
        swipeThreshold = 0.5f * buttons.size() * BUTTON_WIDTH;
        recoverSwipedItem();
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return swipeThreshold;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return 0.1f * defaultValue;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return 5.0f * defaultValue;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        int pos = viewHolder.getAdapterPosition();
        float translationX = dX;
        View itemView = viewHolder.itemView;

        if (pos < 0) {
            swipedPos = pos;
            return;
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                List<UnderlayButton> buffer = new ArrayList<>();

                if (!buttonsBuffer.containsKey(pos)) {
                    instantiateUnderlayButton(viewHolder, buffer);
                    buttonsBuffer.put(pos, buffer);
                } else {
                    buffer = buttonsBuffer.get(pos);
                }

                translationX = dX * buffer.size() * BUTTON_WIDTH / itemView.getWidth();
                drawButtons(c, itemView, buffer, pos, translationX);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive);
    }

    private synchronized void recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            int pos = recoverQueue.poll();
            if (pos > -1) {
                recyclerView.getAdapter().notifyItemChanged(pos);
            }
        }
    }

    private void drawButtons(Canvas c, View itemView, List<UnderlayButton> buffer, int pos, float dX) {
        float right = itemView.getRight();
        float dButtonWidth = (-1) * dX / buffer.size();
        int btnIndex = 0;

        for (UnderlayButton button : buffer) {
            btnIndex++;
            float left = right - dButtonWidth;
            button.onDraw(
                    c,
                    new RectF(
                            left,
                            itemView.getTop(),
                            right,
                            itemView.getBottom()
                    ),
                    pos,
                    button.pinFlag,
                    button.readOnlyFlag
            );

            right = left;
        }
    }

    public void attachSwipe() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public abstract void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons);

    public interface UnderlayButtonClickListener {
        void onClick(int pos);
    }

    public static class UnderlayButton {
        private final Bitmap bitmap;
        private final UnderlayButtonClickListener clickListener;
        private String text;
        private int color;
        private int pos;
        private RectF clickRegion;
        private boolean pinFlag;
        private boolean readOnlyFlag;

        public UnderlayButton(String text, Bitmap bitmap, int color, UnderlayButtonClickListener clickListener) {
            this.text = text;
            this.bitmap = bitmap;
            this.color = color;
            this.clickListener = clickListener;
        }

        public UnderlayButton(String text, Bitmap bitmap, int color, boolean pinFlag, UnderlayButtonClickListener clickListener) {
            this.text = text;
            this.bitmap = bitmap;
            this.color = color;
            this.pinFlag = pinFlag;
            this.clickListener = clickListener;
        }

        public UnderlayButton(String text, Bitmap bitmap, int color,int readonly, boolean readOnlyFlag, UnderlayButtonClickListener clickListener) {
            this.text = text;
            this.bitmap = bitmap;
            this.color = color;
            this.readOnlyFlag = readOnlyFlag;
            this.clickListener = clickListener;
        }

        public boolean onClick(float x, float y) {
            if (clickRegion != null && clickRegion.contains(x, y)) {
                clickListener.onClick(pos);
                return true;
            }

            return false;
        }

        public void onDraw(Canvas c, RectF rect, int pos, boolean pinFlag, boolean readOnlyFlag) {
            Paint p = new Paint();

            // Draw background
            p.setColor(color);
            c.drawRect(rect, p);

            // Draw Text
            p.setColor(Color.WHITE);
            p.setTextSize(40);

            if (pinFlag) {
                NotesAdapter adapter = (NotesAdapter) recyclerView.getAdapter();
                if (adapter.notesList.get(pos).getPinned()) {
                    color = ContextCompat.getColor(MyNote.getContext(), R.color.primary);
                    text = "Un Pin";

                } else {
                    color = ContextCompat.getColor(MyNote.getContext(), R.color.primary_light);
                    text = "Pin";
                }
            }

            if (readOnlyFlag) {
                NotesAdapter adapter = (NotesAdapter) recyclerView.getAdapter();
                if (adapter.notesList.get(pos).getReadonly()) {
                    color = ContextCompat.getColor(MyNote.getContext(), R.color.primary);
                    text = "Unlock";
                } else {
                    color = ContextCompat.getColor(MyNote.getContext(), R.color.primary_light);
                    text = "Lock";
            }
            }


            float spaceHeight = 20; // change to whatever you deem looks better
            float textWidth = p.measureText(text);
            Rect bounds = new Rect();
            p.getTextBounds(text, 0, text.length(), bounds);
            float combinedHeight = bitmap.getHeight() + spaceHeight + bounds.height();
            c.drawBitmap(bitmap, rect.centerX() - (bitmap.getWidth() / 2), rect.centerY() - (combinedHeight / 2), null);
            //If you want text as well with bitmap
            c.drawText(text, rect.centerX() - (textWidth / 2), rect.centerY() + (combinedHeight / 2), p);
            clickRegion = rect;
            this.pos = pos;
        }
    }
}
