package com.scaletools.recipecathcer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
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
import com.scaletools.recipecathcer.view.CropImageView;
import com.scaletools.recipecathcer.view.DrawingView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;


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
    private ImageViewTouch imageView;
    private CropImageView cropImageView;

    private Bitmap imageBitmap;

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
        requestProcessor = new RequestProcessor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drawing, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        scannerLine = (ImageView) view.findViewById(R.id.scannerLine);
        scannerNet = (ImageView) view.findViewById(R.id.scannerNet);
        resultBounds = (ImageView) view.findViewById(R.id.resultBounds);
        drawingView = (DrawingView) view.findViewById(R.id.drawingView);
        imageView = (ImageViewTouch) view.findViewById(R.id.recipeImage);
        cropImageView = (CropImageView) view.findViewById(R.id.cropImageView);

        byte[] imageBytes;
        if (getArguments() != null) {
            imageBytes = getArguments().getByteArray(ARG_IMAGE);
        } else {
            return;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
        options.inSampleSize = 2;
        Bitmap displayImageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
        imageView.setImageDrawable(new BitmapDrawable(context.getResources(), displayImageBitmap));
    }

    public boolean processImage() {
        Animation animScan = AnimationUtils.loadAnimation(context, R.anim.scanning);
        scannerLine.startAnimation(animScan);
        Animation animNet = AnimationUtils.loadAnimation(context, R.anim.scanning_net);
        scannerNet.startAnimation(animNet);

        Bitmap bitmap = imageBitmap;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
        byte[] byteArray = stream.toByteArray();

        if (byteArray.length > 1024*1024) {
            Toast.makeText(context, "ERROR: Image is too big: " + byteArray.length, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            Log.e("INFO", "Image size: " + byteArray.length);
            requestProcessor.sendImageForRecognition(context, byteArray, this);
            return true;
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
                    || (halfWidth / inSampleSize) >= reqWidth) {
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
        if (state == DrawingView.State.VIEW &&
                (drawingView.getState() == DrawingView.State.EDIT)) {
            approveDrawing();
        }
        if (state == DrawingView.State.EDIT) {
            imageView.zoomTo(1, 250);
            drawingView.setVisibility(View.VISIBLE);
        } else {
            drawingView.setVisibility(View.INVISIBLE);
        }
        if (state == DrawingView.State.SELECT) {
            cropImageView.setRatioCropRect(imageView.getBitmapRect(), -1f);
            cropImageView.setVisibility(View.VISIBLE);
        } else {
            cropImageView.setVisibility(View.INVISIBLE);
        }
        drawingView.setState(state);
    }

    public void approveDrawing() {
        Bitmap bitmapDrawing = drawingView.getImageBitmap();

        Rect recipeImageRect = new Rect();
        imageView.getBitmapRect().round(recipeImageRect);
        Bitmap croppedBitmapDrawing = Bitmap.createBitmap(bitmapDrawing,
                recipeImageRect.left, recipeImageRect.top,
                recipeImageRect.right - recipeImageRect.left,
                recipeImageRect.bottom - recipeImageRect.top);
        Bitmap scaledBitmapDrawing = Bitmap.createScaledBitmap(croppedBitmapDrawing,
                imageBitmap.getWidth(), imageBitmap.getHeight(), true);

        croppedBitmapDrawing.recycle();

        Canvas canvas = new Canvas(imageBitmap);
        canvas.drawBitmap(scaledBitmapDrawing, new Matrix(), new Paint());

        scaledBitmapDrawing.recycle();

        imageView.setImageDrawable(new BitmapDrawable(context.getResources(), imageBitmap));
    }

    public void cutSelectedArea() {
        // a rect cropped by user in screen coordinates
        Rect cutImageArea = new Rect();
        cropImageView.getCropRect().round(cutImageArea);
        // a rect that represents image position on a screen
        Rect recipeImageRect = new Rect();
        imageView.getBitmapRect().round(recipeImageRect);
        if (!cutImageArea.isEmpty()) {
            // a rect cropped by user in image coordinates (not scaled to the image size)
            Rect actualCutImageArea = new Rect(cutImageArea.left - recipeImageRect.left,
                    cutImageArea.top - recipeImageRect.top,
                    cutImageArea.right - recipeImageRect.left,
                    cutImageArea.bottom - recipeImageRect.top);

            // get the scale factors for both vertical and horizontal since we're dealing with a square inside of a rectangle
            float scalefactorH = (float)imageBitmap.getWidth() / (float)(recipeImageRect.right - recipeImageRect.left);
            float scalefactorV = (float)imageBitmap.getHeight() / (float)(recipeImageRect.bottom - recipeImageRect.top);

            // create a matrix and apply the scale factors
            Matrix m = new Matrix();
            m.postScale(scalefactorH, scalefactorV);

            // apply the matrix to a RectF
            RectF scaledActualCutImageArea = new RectF(actualCutImageArea);
            m.mapRect(scaledActualCutImageArea);

            Bitmap croppedBitmapDrawing = Bitmap.createBitmap(imageBitmap,
                    (int)scaledActualCutImageArea.left, (int)scaledActualCutImageArea.top,
                    (int)(scaledActualCutImageArea.right - scaledActualCutImageArea.left),
                    (int)(scaledActualCutImageArea.bottom - scaledActualCutImageArea.top));

            imageBitmap = croppedBitmapDrawing;
            imageView.setImageDrawable(new BitmapDrawable(context.getResources(), imageBitmap));
        }
    }

    public boolean sendJsonForParsing(Context context, @Nullable RequestQueue.RequestFinishedListener<NetworkResponse> listener) {
        return requestProcessor.sendJsonForParsing(context, listener);
    }

    public JSONObject getJsonRecipe() {
        return requestProcessor.getJsonRecipe();
    }
}
