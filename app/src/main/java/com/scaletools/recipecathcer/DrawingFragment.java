package com.scaletools.recipecathcer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.scaletools.recipecathcer.network.RequestProcessor;
import com.scaletools.recipecathcer.view.DrawingView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DrawingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DrawingFragment extends Fragment implements RequestQueue.RequestFinishedListener<NetworkResponse> {
    private static final String ARG_IMAGE = "imageBytes";

    private Context context;
    private RequestProcessor requestProcessor;

    private ImageView scannerLine;
    private ImageView scannerNet;
    private ImageView resultBounds;
    private DrawingView drawingView;

    private byte[] imageBytes;

    public DrawingFragment() {}

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param imageBytes Bytes of the imageBytes to edit.
     * @return A new instance of fragment DrawingFragment.
     */
    public static DrawingFragment newInstance(byte[] imageBytes) {
        DrawingFragment fragment = new DrawingFragment();
        Bundle args = new Bundle();
        args.putByteArray(ARG_IMAGE, imageBytes);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageBytes = getArguments().getByteArray(ARG_IMAGE);
        }
        requestProcessor = new RequestProcessor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_drawing, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        scannerLine = (ImageView) view.findViewById(R.id.scannerLine);
        scannerNet = (ImageView) view.findViewById(R.id.scannerNet);
        resultBounds = (ImageView) view.findViewById(R.id.resultBounds);
        drawingView = (DrawingView) view.findViewById(R.id.drawingView);

        drawingView.setBitmapBytes(imageBytes);
    }

    public void processImage() {
        Animation animScan = AnimationUtils.loadAnimation(context, R.anim.scanning);
        scannerLine.startAnimation(animScan);
        Animation animNet = AnimationUtils.loadAnimation(context, R.anim.scanning_net);
        scannerNet.startAnimation(animNet);

        Bitmap bitmap = drawingView.getImageBitmap();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        byte[] byteArray = stream.toByteArray();

        if (byteArray.length > 1024*1024) {
            Toast.makeText(context, "ERROR: Image is too big: " + byteArray.length, Toast.LENGTH_SHORT).show();
        } else {
            Log.e("INFO", "Image size: " + byteArray.length);
            requestProcessor.sendImageForRecognition(context, byteArray, this);
        }
    }

    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    public void onRequestFinished(Request<NetworkResponse> request) {
        if (!request.getUrl().endsWith("ocr")) return;
        List<Rect> rectangles = requestProcessor.getResponseBounds();
        showWordBounds(rectangles);
    }

    private void showWordBounds(final List<Rect> rectangles) {
        // TODO: FIX THIS WEIRD BUG WITH BITMAP AND SAVE POOR PHONE'S MEMORY!!! (May be after a cup of coffee (or two))
        Bitmap bitmap = Bitmap.createBitmap(drawingView.getImageBitmap().getWidth(),
                drawingView.getImageBitmap().getHeight(), Bitmap.Config.ARGB_8888);
        //Bitmap immutableBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        //Bitmap bitmap = immutableBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3.0f);
        paint.setColor(Color.rgb(255, 128, 0));


        if (rectangles != null) {
            for (Rect rect : rectangles) {
                canvas.drawRect(rect, paint);
            }
            resultBounds.setImageBitmap(bitmap);
        } else {
            Toast.makeText(context, "Failed to process the image.", Toast.LENGTH_SHORT).show();
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animFade = AnimationUtils.loadAnimation(context, R.anim.scanning_fade);
                Animation animAppear = AnimationUtils.loadAnimation(context, R.anim.result_appear);

                scannerLine.startAnimation(animFade);
                scannerNet.startAnimation(animFade);

                resultBounds.setVisibility(View.VISIBLE);
                resultBounds.startAnimation(animAppear);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scannerLine.clearAnimation();
                        scannerNet.clearAnimation();
                        resultBounds.clearAnimation();

                        MainActivity mainActivity = (MainActivity) context;
                        if (rectangles != null)
                            mainActivity.showRequestSuccessButtons();
                        else
                            mainActivity.showRequestFailButtons();
                    }
                }, 2000);
            }
        }, 5000);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    public DrawingView.State getState() {
        return drawingView.getState();
    }

    public void setState(DrawingView.State state) {
        drawingView.setState(state);
    }

    public void cutSelectedArea() {
        RectF rect = drawingView.getScaledSelectedRect();
        if (rect != null) {
            Bitmap currentBitmap = drawingView.getImageBitmap();
            Bitmap newBitmap = Bitmap.createBitmap(currentBitmap,
                    (int)rect.left, (int)rect.top,
                    (int)(rect.right - rect.left),
                    (int)(rect.bottom - rect.top), null, false);
            drawingView.setBitmap(newBitmap);
        }
    }

    public boolean sendJsonForParsing(Context context, @Nullable RequestQueue.RequestFinishedListener<NetworkResponse> listener) {
        return requestProcessor.sendJsonForParsing(context, listener);
    }

    public JSONObject getJsonRecipe() {
        return requestProcessor.getJsonRecipe();
    }
}
