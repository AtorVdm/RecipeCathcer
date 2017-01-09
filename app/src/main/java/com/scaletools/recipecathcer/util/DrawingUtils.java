package com.scaletools.recipecathcer.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/**
 * Created by Ator on 13/11/16.
 */

public class DrawingUtils {
    public static final float TOUCH_TOLERANCE = 4;

    public static Paint getCirclePaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(4f);
        return paint;
    }

    public static Paint getPathPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(64);
        return paint;
    }

    public static Paint getRectPaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.TRANSPARENT);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        return paint;
    }

    public static Paint getBoundsPaint() {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.0f);
        paint.setColor(Color.rgb(255, 128, 0));
        return paint;
    }

    public static int getShadeColor() {
        return Color.parseColor("#B0000000");
    }

    public static Bitmap getEmptyBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        return bitmap;
    }

    public static Bitmap replaceColorIn(Bitmap bitmap, int from, int to) {
        int [] allPixels = new int [bitmap.getHeight() * bitmap.getWidth()];

        bitmap.getPixels(allPixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for(int i = 0; i < allPixels.length; i++)
        {
            if(allPixels[i] == from)
            {
                allPixels[i] = to;
            }
        }

        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        result.setPixels(allPixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        return result;
    }
}
