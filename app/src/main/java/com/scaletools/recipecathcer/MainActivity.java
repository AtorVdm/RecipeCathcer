package com.scaletools.recipecathcer;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.scaletools.recipecathcer.fragment.ChoosingFragment;
import com.scaletools.recipecathcer.fragment.DrawingFragment;
import com.scaletools.recipecathcer.fragment.ResultFragment;
import com.scaletools.recipecathcer.helper.ImageCatcher;
import com.scaletools.recipecathcer.network.RequestProcessor;
import com.scaletools.recipecathcer.view.DrawingView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements RequestQueue.RequestFinishedListener<NetworkResponse>, ImageCatcher {
    List<FloatingActionButton> fabList = new ArrayList<>();
    private FloatingActionButton fabGood;
    private FloatingActionButton fabDismiss;
    private FloatingActionButton fabBrush;
    private FloatingActionButton fabSelect;
    private FloatingActionButton fabContinue;
    private FloatingActionButton fabBack;

    private ChoosingFragment choosingFragment;
    private DrawingFragment drawingFragment;
    private ResultFragment resultFragment;

    //region @Override
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fabGood = (FloatingActionButton) findViewById(R.id.fabGood);
        fabDismiss = (FloatingActionButton) findViewById(R.id.fabDismiss);
        fabBrush = (FloatingActionButton) findViewById(R.id.fabBrush);
        fabSelect = (FloatingActionButton) findViewById(R.id.fabSelect);
        fabContinue = (FloatingActionButton) findViewById(R.id.fabContinue);
        fabBack = (FloatingActionButton) findViewById(R.id.fabBack);
        fabList = Arrays.asList(fabGood, fabDismiss, fabBrush,
                fabSelect, fabContinue, fabBack);

        showChoosingFragment();
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return super.onCreateView(name, context, attrs);
    }

    @Override
    public void onRequestFinished(Request<NetworkResponse> request) {
        if (!request.getTag().equals(RequestProcessor.PARSE)) return;
        resultFragment = ResultFragment.newInstance();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activityFragment, resultFragment);
        transaction.commit();

        resultFragment.showResult(drawingFragment.getJsonRecipe());
        displayButtons();
    }

    @Override
    public void catchImage(Bitmap bitmap) {
        showDrawingFragment(bitmap);
    }
    //endregion

    //region onClick
    public void clickImageApprove(View v) {
        if (drawingFragment.processImage()) {
            scanMode();
        }
    }

    public void clickImageDismiss(View v) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activityFragment, choosingFragment);
        transaction.commit();
        choosingMode();
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
        drawingFragment.cancelOcrResult();
        decisionMode();
    }
    //endregion


    //region Modes
    public void showRequestButtons(boolean success) {
        fabContinue.setImageResource(success? R.drawable.ic_continue: R.drawable.ic_try_again);
        displayButtons(fabContinue, fabBack);
    }

    private void choosingMode() {
        displayButtons();
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
        Set<FloatingActionButton> fabSet = new HashSet<>(fabList);
        for (FloatingActionButton fab : fabs) {
            fab.show();
            fabSet.remove(fab);
        }
        for (FloatingActionButton fab : fabSet) {
            fab.hide();
        }
    }
    //endregion


    //region Fragment
    private void showChoosingFragment() {
        choosingFragment = ChoosingFragment.newInstance(this);
        replaceFragment(choosingFragment);

        choosingMode();
    }

    private void showDrawingFragment(Bitmap bitmap) {
        drawingFragment = DrawingFragment.newInstance(bitmap);
        replaceFragment(drawingFragment);

        decisionMode();
    }

    private void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activityFragment, fragment);
        transaction.commit();
    }
    //endregion
}
