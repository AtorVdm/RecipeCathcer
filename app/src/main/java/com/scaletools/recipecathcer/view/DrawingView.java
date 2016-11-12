package com.scaletools.recipecathcer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

/**
 * Created by Ator on 20/10/16.
 */

public class DrawingView extends ImageViewTouch {
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mPaint;
    private Context context;
    private Paint circlePaint;
    private Path circlePath;

    private Paint mRectPaint;
    private TextPaint mTextPaint;
    private boolean mDrawRect;

    private State state;
    private RectF selectedRect;
    private byte[] imageBytes;
    private RectF imageBounds;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        state = State.VIEW;

        mPath = new Path();
        circlePaint = new Paint();
        circlePath = new Path();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeJoin(Paint.Join.MITER);
        circlePaint.setStrokeWidth(4f);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GRAY);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(64);

        mRectPaint = new Paint();
        mRectPaint.setColor(Color.rgb(0, 161, 214));
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
        mRectPaint.setStrokeWidth(5);

        mTextPaint = new TextPaint();
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(30);

        imageBounds = new RectF();
    }

    public void setBitmapBytes(byte[] bytes) {
        this.imageBytes = bytes;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final int width = w;
        final int height = h;
        new Runnable() {
            @Override
            public void run() {
                if (imageBytes == null) return;

                mBitmap = decodeSampledBitmapFromByteArray(imageBytes, width, height);
                imageBytes = null;

                mCanvas = new Canvas(mBitmap);
                //mCanvas.drawBitmap(mBitmap, 0, 0, new Paint());
                setImageBitmap(mBitmap);
                selectedRect = null;
                imageBytes = null;

                float[] f = new float[9];
                getImageMatrix().getValues(f);

                // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
                final float scaleX = f[Matrix.MSCALE_X];
                final float scaleY = f[Matrix.MSCALE_Y];

                final int actW = Math.round(mBitmap.getWidth() * scaleX);
                final int actH = Math.round(mBitmap.getHeight() * scaleY);

                int top = (height - actH)/2;
                int left = (width - actW)/2;

                imageBounds = new RectF(left, top, left + actW, top + actH);
            }
        }.run();
    }

    public void setBitmap(Bitmap bitmap) {
        //setImageDrawable(new BitmapDrawable(getResources(), bitmap));
        mBitmap = bitmap;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawBitmap(mBitmap, 0, 0, null);
        canvas.drawPath(mPath,  mPaint);
        canvas.drawPath(circlePath,  circlePaint);
        if (mDrawRect) {
            canvas.drawRect(Math.min(mStartX, mEndX), Math.min(mStartY, mEndY),
                    Math.max(mEndX, mStartX), Math.max(mEndY, mStartY), mRectPaint);
            canvas.drawText("  (" + (int)Math.abs(mStartX - mEndX) + " , " + (int)Math.abs(mStartY - mEndY) + ")",
                    Math.max(mEndX, mStartX), Math.max(mEndY, mStartY), mTextPaint);
        } else if (selectedRect != null) {
            canvas.drawRect(selectedRect, mRectPaint);
        }
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start_drawing(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
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
            if (x < imageBounds.left || x > imageBounds.right ||
                    y < imageBounds.top || y > imageBounds.bottom)
                return true;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start_drawing(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move_drawing(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up_drawing();
                    invalidate();
                    break;
            }
        } else if (state == State.SELECT) {
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start_selecting(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move_selecting(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up_selecting();
                    invalidate();
                    break;
            }
        }
        return true;
    }

    private float mStartX, mStartY, mEndX, mEndY;

    private void touch_start_selecting(float x, float y) {
        mDrawRect = false;
        selectedRect = null;
        mStartX = x;
        mStartY = y;
    }

    private void touch_move_selecting(float x, float y) {
        if (x >= TOUCH_TOLERANCE || y >= TOUCH_TOLERANCE) {
            if (!mDrawRect || Math.abs(x - mEndX) > 5 || Math.abs(y - mEndY) > 5) {
                mEndX = x;
                mEndY = y;
            }

            mDrawRect = true;
        }
    }

    private void touch_up_selecting() {
        selectedRect = new RectF(Math.min(mStartX, mEndX), Math.min(mStartY, mEndY),
                    Math.max(mEndX, mStartX), Math.max(mEndY, mStartY));
        mDrawRect = false;
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
        this.state = state;
    }

    public Bitmap getImageBitmap() {
        return mBitmap;
    }

    public RectF getScaledSelectedRect() {
        if (selectedRect == null) return null;
        float scaleX = (float)mBitmap.getWidth()/getWidth();
        float scaleY = (float)mBitmap.getHeight()/getHeight();
        return new RectF(selectedRect.left * scaleX, selectedRect.top * scaleY,
                selectedRect.right * scaleX, selectedRect.bottom * scaleY);
    }

    public enum State {
        VIEW, EDIT, SELECT
    }
}
