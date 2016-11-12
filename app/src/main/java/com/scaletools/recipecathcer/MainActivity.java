package com.scaletools.recipecathcer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.scaletools.recipecathcer.view.DrawingView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RequestQueue.RequestFinishedListener<NetworkResponse> {
    private static final int PICK_IMAGE = 0;
    List<FloatingActionButton> fabList = new ArrayList<>();
    private FloatingActionButton fabCapture;
    private FloatingActionButton fabGood;
    private FloatingActionButton fabDismiss;
    private FloatingActionButton fabBrush;
    private FloatingActionButton fabSelect;
    private FloatingActionButton fabContinue;
    private FloatingActionButton fabBack;
    private FloatingActionButton fabUpload;

    private CameraFragment cameraFragment;
    private DrawingFragment drawingFragment;
    private ResultFragment resultFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraFragment = CameraFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activityFragment, cameraFragment);
        transaction.commit();

        fabCapture = (FloatingActionButton) findViewById(R.id.fabCapture);
        fabGood = (FloatingActionButton) findViewById(R.id.fabGood);
        fabDismiss = (FloatingActionButton) findViewById(R.id.fabDismiss);
        fabBrush = (FloatingActionButton) findViewById(R.id.fabBrush);
        fabSelect = (FloatingActionButton) findViewById(R.id.fabSelect);
        fabContinue = (FloatingActionButton) findViewById(R.id.fabContinue);
        fabBack = (FloatingActionButton) findViewById(R.id.fabBack);
        fabUpload = (FloatingActionButton) findViewById(R.id.fabUpload);
        fabList = Arrays.asList(fabCapture, fabGood, fabDismiss, fabBrush,
                fabSelect, fabContinue, fabBack, fabUpload);

        captureMode();
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    public void clickImageCapture(View v) {
        cameraFragment.takePicture();
    }

    public void clickImageApprove(View v) {
        scanMode();
        drawingFragment.processImage();
    }

    public void clickImageDismiss(View v) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activityFragment, cameraFragment);
        transaction.commit();
        captureMode();
        cameraFragment.openCamera();
    }

    public void clickImageBrush(View v) {
        if (drawingFragment != null) {
            if (drawingFragment.getState() == DrawingView.State.EDIT) {
                fabBrush.setImageResource(R.drawable.ic_brush);
                fabBrush.setAlpha(1.0f);
                decisionMode();
                drawingFragment.setState(DrawingView.State.VIEW);
            } else if (drawingFragment.getState() == DrawingView.State.VIEW) {
                fabBrush.setAlpha(0.3f);
                fabBrush.setImageResource(R.drawable.ic_check);
                drawingMode();
                fabBrush.invalidate();
                drawingFragment.setState(DrawingView.State.EDIT);
            } else if (drawingFragment.getState() == DrawingView.State.SELECT) {
                // canceling selecting with this button
            }
        }
    }

    public void clickImageSelect(View v) {
        if (drawingFragment != null) {
            if (drawingFragment.getState() == DrawingView.State.SELECT) {
                decisionMode();
                fabSelect.setImageResource(R.drawable.ic_select_area);
                fabSelect.setAlpha(1.0f);
                drawingFragment.setState(DrawingView.State.VIEW);
                drawingFragment.cutSelectedArea();
            } else if (drawingFragment.getState() == DrawingView.State.VIEW) {
                selectingMode();
                fabSelect.setAlpha(0.3f);
                fabSelect.setImageResource(R.drawable.ic_check);
                fabSelect.invalidate();
                drawingFragment.setState(DrawingView.State.SELECT);
            } else if (drawingFragment.getState() == DrawingView.State.EDIT) {
                // canceling brushing with this button
            }
        }
    }

    public void clickImageContinue(View v) {
        if (!drawingFragment.sendJsonForParsing(this, this))
            Toast.makeText(this, "Failed to send for parsing.", Toast.LENGTH_SHORT).show();
    }

    public void clickImageBack(View v) {
        decisionMode();
    }

    public void clickImageUpload(View v) {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {pickIntent});

        startActivityForResult(chooserIntent, PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    //Display an error
                    return;
                }
                InputStream inputStream = getContentResolver().openInputStream(data.getData());

                byte[] bytes = readBytes(inputStream);
                openDrawingFragment(bytes);
            }
        } catch (FileNotFoundException ex) {
            Toast.makeText(this, "Image was not found.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Cannot read this image.", Toast.LENGTH_SHORT).show();
        }
    }

    public void imageCaptured(Image image) {
        ByteBuffer buffer = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            buffer = image.getPlanes()[0].getBuffer();
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        openDrawingFragment(bytes);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            image.close();
        }
    }

    private void openDrawingFragment(byte[] imageBytes) {
        drawingFragment = DrawingFragment.newInstance(imageBytes);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activityFragment, drawingFragment);
        transaction.commit();

        decisionMode();
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    public void showRequestSuccessButtons() {
        fabContinue.setImageResource(R.drawable.ic_continue);
        displayButtons(fabContinue, fabBack);
    }

    public void showRequestFailButtons() {
        fabContinue.setImageResource(R.drawable.ic_try_again);
        displayButtons(fabContinue, fabBack);
    }

    private void captureMode() {
        displayButtons(fabCapture, fabUpload);
    }

    private void decisionMode() {
        displayButtons(fabGood, fabDismiss, fabBrush, fabSelect);
    }

    private void scanMode() {
        displayButtons();
    }

    private void drawingMode() {
        displayButtons(fabBrush);
    }

    private void selectingMode() {
        displayButtons(fabSelect);
    }

    private void displayButtons(FloatingActionButton... fabs) {
        List<FloatingActionButton> fabListTemp = new ArrayList<>(fabList);
        for (FloatingActionButton fab : fabs) {
            fab.show();
            fabListTemp.remove(fab);
        }
        for (FloatingActionButton fab : fabListTemp) {
            fab.hide();
        }
    }

    @Override
    public void onRequestFinished(Request<NetworkResponse> request) {
        if (!request.getUrl().endsWith("parse")) return;
        resultFragment = ResultFragment.newInstance();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activityFragment, resultFragment);
        transaction.commit();

        resultFragment.showResult(drawingFragment.getJsonRecipe());
        displayButtons();
    }
}
