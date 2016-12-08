package com.scaletools.recipecathcer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.scaletools.recipecathcer.helper.State;
import com.scaletools.recipecathcer.util.DrawingUtils;

import static com.scaletools.recipecathcer.util.DrawingUtils.TOUCH_TOLERANCE;

/**
 * Created by Ator on 20/10/16.
 */

public class CroppingView extends ImageView {

    //region Fields
    private Bitmap mBitmap;
    private Path mPath;
    private Paint mPaint;
    private Paint circlePaint;
    private Path circlePath;

    private State state;
    private float startX, startY;

    private Paint mBackgroundPaint;
    private Bitmap mBackgroundBitmap;
    private Canvas mBackgroundCanvas;
    //endregion


    //region Init
    public CroppingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CroppingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        state = State.VIEW;

        reset();

        circlePaint = DrawingUtils.getCirclePaint();
        mPaint = DrawingUtils.getRectPaint();

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(DrawingUtils.getShadeColor());
    }

    private void reset() {
        if (mPath == null)
            mPath = new Path();
        circlePath = new Path();
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
        setImageBitmap(mBitmap);
        updateBackground();
    }

    private void updateBackground() {
        mBackgroundBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        mBackgroundCanvas = new Canvas(mBackgroundBitmap);
        mBackgroundCanvas.drawPaint(mBackgroundPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Path drawingPath = new Path(mPath);
        if (!circlePath.isEmpty()) {
            drawingPath.close();
        }

        mBackgroundCanvas.drawPath(drawingPath,  mPaint);
        canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
        canvas.drawPath(circlePath, circlePaint);
    }
    //endregion


    //region Touch
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state == State.VIEW) {
            super.onTouchEvent(event);
        } else if (state == State.SELECT) {
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

    private float mX, mY;

    private void touch_start_drawing(float x, float y) {
        updateBackground();
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
        startX = x;
        startY = y;
    }

    private void touch_move_drawing(float x, float y) {
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
        mPath.lineTo(startX, startY);
        circlePath.reset();
    }
    //endregion


    //region get/set
    public void setState(State state) {
        this.state = state;
        reset();
    }

    public Path getPath() {
        return mPath;
    }
    //endregion
}
