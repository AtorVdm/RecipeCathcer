package com.scaletools.recipecathcer.fragment;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
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
import com.android.volley.RequestQueue;
import com.scaletools.recipecathcer.MainActivity;
import com.scaletools.recipecathcer.R;
import com.scaletools.recipecathcer.helper.State;
import com.scaletools.recipecathcer.network.RequestProcessor;
import com.scaletools.recipecathcer.util.DrawingUtils;
import com.scaletools.recipecathcer.view.CroppingView;
import com.scaletools.recipecathcer.view.DrawingView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;


public class DrawingFragment extends Fragment {

    //region Constants
    private static final String TAG = "DrawingFragment";
    private static final String BITMAP = "BITMAP";
    private static final String STATE = "STATE";
    private static final String REQUEST_PROCESSOR = "REQUEST_PROCESSOR";
    private static final String RESULT_VISIBLE = "RESULT_VISIBLE";
    private static final int MAX_IMAGE_LENGTH = 1024 * 1024;
    public static final int DELAY_MILLIS = 2000;
    //endregion


    //region Fields
    private Context context;
    private RequestProcessor requestProcessor;

    private ImageView scannerLine;
    private ImageView scannerNet;
    private ImageView resultBounds;
    private DrawingView drawingView;
    private ImageViewTouch imageView;
    private CroppingView croppingView;

    private Bitmap imageBitmap;
    private State state;
    //endregion


    //region Init
    public DrawingFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param bitmap Bitmap to edit.
     * @return A new instance of fragment DrawingFragment.
     */
    public static DrawingFragment newInstance(Bitmap bitmap) {
        DrawingFragment fragment = new DrawingFragment();
        Bundle args = new Bundle();
        args.putParcelable(BITMAP, bitmap);
        fragment.setArguments(args);
        return fragment;
    }
    //endregion


    //region @Override
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestProcessor = new RequestProcessor();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // DO NOT REMOVE! Inflates the layout for this fragment
        return inflater.inflate(R.layout.fragment_drawing, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        scannerLine = (ImageView) view.findViewById(R.id.scannerLine);
        scannerNet = (ImageView) view.findViewById(R.id.scannerNet);
        resultBounds = (ImageView) view.findViewById(R.id.resultBounds);
        drawingView = (DrawingView) view.findViewById(R.id.drawingView);
        imageView = (ImageViewTouch) view.findViewById(R.id.recipeImage);
        croppingView = (CroppingView) view.findViewById(R.id.croppingView);

        if (getArguments() != null) {
            imageBitmap = getArguments().getParcelable(BITMAP);
            getArguments().putParcelable(BITMAP, null);
            updateImageView(imageBitmap);
        } else {
            Log.e(TAG, "No arguments passed!");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save the fragment's state here
        outState.putSerializable(STATE, getState());
        outState.putParcelable(BITMAP, imageBitmap);
        outState.putParcelable(REQUEST_PROCESSOR, requestProcessor);
        outState.putBoolean(RESULT_VISIBLE, resultBounds.getVisibility() == View.VISIBLE ? true : false);
    }

    @Override
    @SuppressWarnings("ResourceType")
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            //Restore the fragment's state here
            imageBitmap = savedInstanceState.getParcelable(BITMAP);
            setState((State) savedInstanceState.getSerializable(STATE));
            updateImageView(imageBitmap);
            requestProcessor = savedInstanceState.getParcelable(REQUEST_PROCESSOR);
            if (savedInstanceState.getBoolean(RESULT_VISIBLE))
                showWordBounds(false);
        }
    }

    private void updateImageView(Bitmap imageBitmap) {
        if (imageBitmap != null && imageView != null) {
            imageView.setImageDrawable(new BitmapDrawable(context.getResources(), imageBitmap));
        }
    }
    //endregion


    //region Behaviour
    public boolean processImage(@Nullable RequestQueue.RequestFinishedListener<NetworkResponse> listener) {
        Animation animScan = AnimationUtils.loadAnimation(context, R.anim.scanning);
        Animation animNet = AnimationUtils.loadAnimation(context, R.anim.scanning_net);

        Bitmap bitmap = imageBitmap;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Log.i(TAG, "Image size: " + byteArray.length);
        if (byteArray.length > MAX_IMAGE_LENGTH) {
            Toast.makeText(context, "ERROR: Image is too big: " + byteArray.length, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            zoomImageView();
            scannerLine.startAnimation(animScan);
            requestProcessor.sendImageForRecognition(context, byteArray, listener);
            scannerNet.startAnimation(animNet);
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            return true;
        }
    }

    public void showWordBounds(boolean animated) {
        final List<Rect> rectangles = requestProcessor.getResponseBounds();
        if (rectangles != null) {
            Bitmap bitmap = DrawingUtils.getEmptyBitmap(imageBitmap.getWidth(), imageBitmap.getHeight());
            Paint paint = DrawingUtils.getBoundsPaint();
            Canvas canvas = new Canvas(bitmap);

            for (Rect rect : rectangles) {
                canvas.drawRect(rect, paint);
            }

            resultBounds.setImageBitmap(bitmap);
        } else {
            Log.e(TAG, "Response bounds weren't calculated");
            Toast.makeText(context, "Can't process the image.", Toast.LENGTH_SHORT).show();
        }
        if (animated)
            showResultBounds(rectangles != null);
        else
            resultBounds.setVisibility(View.VISIBLE);
    }

    private void showResultBounds(final boolean success) {
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

                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.showRequestButtons(success);
                }

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scannerLine.clearAnimation();
                        scannerNet.clearAnimation();
                        resultBounds.clearAnimation();

                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    }
                }, DELAY_MILLIS);
            }
        }, DELAY_MILLIS);
    }

    public void approveDrawing() {
        Bitmap bitmapDrawing = drawingView.getImageBitmap();
        float[] values = new float[9];
        imageView.getImageMatrix().getValues(values);
        // coordinates of zoomed area that is being painted
        float left = (-values[Matrix.MTRANS_X]) / values[Matrix.MSCALE_X];
        float top = (-values[Matrix.MTRANS_Y]) / values[Matrix.MSCALE_Y];
        float width = (imageView.getWidth() - values[Matrix.MTRANS_X]) / values[Matrix.MSCALE_X] - left;
        float height = (imageView.getHeight() - values[Matrix.MTRANS_Y]) / values[Matrix.MSCALE_Y] - top;

        // scaling the drawing to paint on the main bitmap
        Bitmap scaledBitmapDrawing = Bitmap.createScaledBitmap(bitmapDrawing, (int) (width), (int) (height), true);

        // drawing the painted area on top of the main bitmap
        Canvas canvas = new Canvas(imageBitmap);
        canvas.drawBitmap(scaledBitmapDrawing, left, top, new Paint());

        scaledBitmapDrawing.recycle();

        imageView.setImageDrawable(new BitmapDrawable(context.getResources(), imageBitmap), imageView.getDisplayMatrix(), -1.0F, -1.0F);
    }

    public void cutSelectedArea() {
        if (!croppingView.isUpdated()) return;

        // a rect that represents image position on a screen
        Rect recipeImageRect = new Rect();
        imageView.getBitmapRect().round(recipeImageRect);

        // get the scale factors for both vertical and horizontal since we're dealing with a square inside of a rectangle
        float scaleFactorH = (float) imageBitmap.getWidth() / (float) (recipeImageRect.right - recipeImageRect.left);
        float scaleFactorV = (float) imageBitmap.getHeight() / (float) (recipeImageRect.bottom - recipeImageRect.top);

        // create a matrix and apply the scale factors
        Matrix m = new Matrix();
        m.postTranslate(-recipeImageRect.left, -recipeImageRect.top);
        m.postScale(scaleFactorH, scaleFactorV);

        Bitmap resultBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), imageBitmap.getConfig());
        Canvas canvas = new Canvas(resultBitmap);

        // draw initial bitmap on the canvas
        canvas.drawBitmap(imageBitmap, 0, 0, null);

        Bitmap backgroundSelectionBitmap = croppingView.getBackgroundBitmap();
        backgroundSelectionBitmap = DrawingUtils.replaceColorIn(backgroundSelectionBitmap, DrawingUtils.getShadeColor(), Color.BLACK);

        // draw black area which replaces parts of the bitmap that weren't selected
        canvas.drawBitmap(backgroundSelectionBitmap, m, null);

        // find variables of the matrix for painted area
        float[] values = new float[9];
        m.getValues(values);
        float areaX = values[Matrix.MTRANS_X];
        float areaY = values[Matrix.MTRANS_Y];
        float areaWidth = values[Matrix.MSCALE_X] * backgroundSelectionBitmap.getWidth();
        float areaHeight = values[Matrix.MSCALE_Y] * backgroundSelectionBitmap.getHeight();

        // paint unaffected areas in case of zoomed image
        Paint blackPaint = new Paint(Color.BLACK);
        canvas.drawRect(0, 0, areaX, imageBitmap.getHeight(), blackPaint);
        canvas.drawRect(0, 0, imageBitmap.getWidth(), areaY, blackPaint);
        canvas.drawRect(areaX + areaWidth, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), blackPaint);
        canvas.drawRect(0, areaY + areaHeight, imageBitmap.getWidth(), imageBitmap.getHeight(), blackPaint);

        imageBitmap = resultBitmap;
        Matrix displayMatrix = imageView.getDisplayMatrix();
        imageView.setImageBitmap(imageBitmap, displayMatrix, -1.0f, -1.0f);
    }

    public boolean sendJsonForParsing(@Nullable RequestQueue.RequestFinishedListener<NetworkResponse> listener) {
        boolean result = requestProcessor.sendJsonForParsing(context, listener);
        if (result) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
        return result;
    }

    public boolean cancelOcrResult() {
        if (resultBounds.getDrawable() != null) {
            resultBounds.setVisibility(View.INVISIBLE);
            requestProcessor.reset();
            setScaleEnabled(true);
            return true;
        } else {
            return false;
        }
    }
    //endregion


    //region get/set
    public State getState() {
        if (state == null) {
            state = State.VIEW;
        }
        return state;
    }

    public void setState(State state) {
        if (state == State.BRUSH) {
            //zoomImageView();
            setScaleEnabled(false);
            drawingView.setVisibility(View.VISIBLE);
        } else {
            setScaleEnabled(true);
            drawingView.setVisibility(View.INVISIBLE);
        }
        if (state == State.SELECT) {
            setScaleEnabled(false);
            croppingView.setVisibility(View.VISIBLE);
        } else {
            setScaleEnabled(true);
            croppingView.setVisibility(View.INVISIBLE);
        }
        this.state = state;
        drawingView.setState(state);
        croppingView.setState(state);
    }

    public JSONObject getJsonRecipe() {
        return requestProcessor.getJsonRecipe();
    }
    //endregion


    //region Helpers
    private void zoomImageView() {
        imageView.zoomTo(1, 250);
    }

    private void setScaleEnabled(boolean enabled) {
        imageView.setScaleEnabled(enabled);
    }
    //endregion
}
