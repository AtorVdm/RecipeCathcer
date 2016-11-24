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

public class CroppingView extends ImageView {
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mPaint;
    private Context context;
    private Paint circlePaint;
    private Path circlePath;

    private State state;

    public CroppingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CroppingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
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

    private Bitmap decodeSampledBitmapFromByteArray(byte[] imageBytes, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inMutable = true;

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    || (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
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
