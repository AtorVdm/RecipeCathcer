package com.scaletools.recipecathcer.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * Created by Ator on 08/12/16.
 */

public class BitmapUtils {

    public static byte[] compress(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bytes = stream.toByteArray();
        return bytes;
    }

    public static Bitmap decompress(byte[] bytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap;
    }
}
