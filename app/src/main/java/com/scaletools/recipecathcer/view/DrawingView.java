package com.scaletools.recipecathcer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.scaletools.recipecathcer.util.DrawingInitUtils;

/**
 * Created by Ator on 20/10/16.
 */

public class DrawingView extends ImageView {
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mPaint;
    private Paint circlePaint;
    private Path circlePath;

    private State state;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        state = State.VIEW;

        mPath = new Path();
        circlePath = new Path();

        circlePaint = DrawingInitUtils.getCirclePaint();
        mPaint = DrawingInitUtils.getPathPaint();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final int width = w;
        final int height = h;
        new Runnable() {
            @Override
            public void run() {
                updateBitmap(width, height);
            }
        }.run();
    }

    private void updateBitmap(int width, int height) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        setImageBitmap(mBitmap);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(mPath,  mPaint);
        canvas.drawPath(circlePath,  circlePaint);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start_drawing(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touch_move_drawing(float x, float y) {/*
        if (!getBitmapRect().contains(x + STROKE_WIDTH, y + STROKE_WIDTH) &&
                !getBitmapRect().contains(x - STROKE_WIDTH, y - STROKE_WIDTH)) return;*/
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;

            circlePath.reset();
            circlePath.addCircle(mX, mY, 30, Path.Direction.CW);
        }
    }

    private void touch_up_drawing() {
        mPath.lineTo(mX, mY);
        circlePath.reset();
        mCanvas.drawPath(mPath,  mPaint);
        // kill this so we don't double draw
        mPath.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state == State.VIEW) {
            super.onTouchEvent(event);
        } else if (state == State.EDIT) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start_drawing(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move_drawing(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up_drawing();
                    break;
            }
            invalidate();
        }
        return true;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        if ((this.state == State.EDIT || this.state == State.SELECT)
                && state == State.VIEW) {
            updateBitmap(getWidth(), getHeight());
        }
        this.state = state;
    }

    public Bitmap getImageBitmap() {
        return mBitmap;
    }

    public enum State {
        VIEW, EDIT, SELECT
    }
}
