package com.scaletools.recipecathcer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.scaletools.recipecathcer.fragment.ChoosingFragment;
import com.scaletools.recipecathcer.fragment.DrawingFragment;
import com.scaletools.recipecathcer.fragment.ResultFragment;
import com.scaletools.recipecathcer.helper.ImageCatcher;
import com.scaletools.recipecathcer.helper.State;
import com.scaletools.recipecathcer.network.RequestProcessor;
import com.scaletools.recipecathcer.network.volley.VolleySingleton;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements RequestQueue.RequestFinishedListener<NetworkResponse>, ImageCatcher {

    //region Constants
    private static final String FAB_VISIBILITY_ARRAY = "FAB_VISIBILITY_ARRAY";
    private static final String FAB_RESOURCE_ARRAY = "FAB_RESOURCE_ARRAY";
    private static final String FAB_ALPHA_ARRAY = "FAB_ALPHA_ARRAY";
    private static final String CHOOSING_FRAGMENT = "CHOOSING_FRAGMENT";
    private static final String DRAWING_FRAGMENT = "DRAWING_FRAGMENT";
    private static final String RESULT_FRAGMENT = "RESULT_FRAGMENT";
    private static final String TAG = "MainActivity";
    //endregion


    //region Fields
    private List<FloatingActionButton> fabList = new ArrayList<>();
    private FloatingActionButton fabGood;
    private FloatingActionButton fabDismiss;
    private FloatingActionButton fabBrush;
    private FloatingActionButton fabSelect;
    private FloatingActionButton fabContinue;
    private FloatingActionButton fabBack;

    private ChoosingFragment choosingFragment;
    private DrawingFragment drawingFragment;
    private ResultFragment resultFragment;
    //endregion


    //region @Override
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fabGood = fab(R.id.fabGood);
        fabDismiss = fab(R.id.fabDismiss);
        fabBrush = fab(R.id.fabBrush);
        fabSelect = fab(R.id.fabSelect);
        fabContinue = fab(R.id.fabContinue);
        fabBack = fab(R.id.fabBack);
        fabList = Arrays.asList(fabGood, fabDismiss, fabBrush,
                fabSelect, fabContinue, fabBack);
        
        if (savedInstanceState == null) {
            showChoosingFragment();
        }
    }

    private FloatingActionButton fab(int rId) {
        return (FloatingActionButton) findViewById(rId);
    }

    @Override
    public void onRequestFinished(Request<NetworkResponse> request) {
        try {
            if (request.getTag().equals(RequestProcessor.OCR)) {
                drawingFragment.showWordBounds(true);
            } else if (request.getTag().equals(RequestProcessor.PARSE)) {
                showResultFragment(drawingFragment.getJsonRecipe());
            }

            VolleySingleton.getInstance(this).getRequestQueue().removeRequestFinishedListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Error processing finished response: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void catchImage(Bitmap bitmap) {
        showDrawingFragment(bitmap);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int[] fabVisibilityArray = new int[fabList.size()];
        int[] fabResourceArray = new int[fabList.size()];
        float[] fabAlphaArray = new float[fabList.size()];
        try {
            for (int i = 0; i < fabList.size(); i++) {
                fabVisibilityArray[i] = fabList.get(i).getVisibility();
                fabResourceArray[i] = fabList.get(i).getTag() != null ?
                        (int) fabList.get(i).getTag() : -1;
                fabAlphaArray[i] = fabList.get(i).getAlpha();
            }
            outState.putIntArray(FAB_VISIBILITY_ARRAY, fabVisibilityArray);
            outState.putIntArray(FAB_RESOURCE_ARRAY, fabResourceArray);
            outState.putFloatArray(FAB_ALPHA_ARRAY, fabAlphaArray);

            FragmentManager sfm = getSupportFragmentManager();

            if (choosingFragment != null && sfm.getFragments().contains(choosingFragment))
                sfm.putFragment(outState, CHOOSING_FRAGMENT, choosingFragment);
            if (drawingFragment != null && sfm.getFragments().contains(drawingFragment))
                sfm.putFragment(outState, DRAWING_FRAGMENT, drawingFragment);
            if (resultFragment != null && sfm.getFragments().contains(resultFragment))
                sfm.putFragment(outState, RESULT_FRAGMENT, resultFragment);
        } catch (Exception e) {
            Log.e(TAG, "Error saving instance state: " + e.getLocalizedMessage());
        }
    }

    @Override
    @SuppressWarnings("ResourceType")
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int[] fabVisibilityArray = savedInstanceState.getIntArray(FAB_VISIBILITY_ARRAY);
        int[] fabResourceArray = savedInstanceState.getIntArray(FAB_RESOURCE_ARRAY);
        float[] fabAlphaArray = savedInstanceState.getFloatArray(FAB_ALPHA_ARRAY);
        try {
            for (int i = 0; i < fabList.size(); i++) {
                fabList.get(i).setVisibility(fabVisibilityArray[i]);
                if (fabResourceArray[i] != -1) {
                    fabList.get(i).setImageResource(fabResourceArray[i]);
                    fabList.get(i).setTag(fabResourceArray[i]);
                }
                fabList.get(i).setAlpha(fabAlphaArray[i]);
            }

            FragmentManager sfm = getSupportFragmentManager();

            choosingFragment = (ChoosingFragment) sfm.getFragment(savedInstanceState, CHOOSING_FRAGMENT);
            drawingFragment = (DrawingFragment) sfm.getFragment(savedInstanceState, DRAWING_FRAGMENT);
            resultFragment = (ResultFragment) sfm.getFragment(savedInstanceState, RESULT_FRAGMENT);
        } catch (Exception e) {
            Log.e(TAG, "Error restoring instance state: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    //endregion


    //region onClick
    public void clickImageApprove(View v) {
        if (drawingFragment.processImage(this)) {
            scanMode();
        }
    }

    public void clickImageDismiss(View v) {
        showChoosingFragment();
    }

    public void clickImageBrush(View v) {
        if (drawingFragment != null) {
            if (drawingFragment.getState() == State.BRUSH) {
                // BRUSH -> VIEW (OK)
                drawingFragment.setState(State.VIEW);
                drawingFragment.approveDrawing();
                stopEditing();
                decisionMode();
            } else if (drawingFragment.getState() == State.VIEW) {
                // VIEW -> BRUSH
                startEditing(State.BRUSH);
            } else if (drawingFragment.getState() == State.SELECT) {
                // SELECT -> VIEW (CANCEL)
                stopEditing();
            }
        }
    }

    public void clickImageSelect(View v) {
        if (drawingFragment != null) {
            if (drawingFragment.getState() == State.SELECT) {
                // SELECT -> VIEW (OK)
                drawingFragment.cutSelectedArea();
                stopEditing();
                decisionMode();
            } else if (drawingFragment.getState() == State.VIEW) {
                // VIEW -> SELECT
                startEditing(State.SELECT);
            } else if (drawingFragment.getState() == State.BRUSH) {
                // BRUSH -> VIEW (CANCEL)
                stopEditing();
            }
        }
    }

    private void stopEditing() {
        if (drawingFragment == null) return;
        drawingFragment.setState(State.VIEW);
        setFabResourceAlpha(fabBrush, R.drawable.ic_brush, 1f);
        setFabResourceAlpha(fabSelect, R.drawable.ic_select_area, 1f);
        decisionMode();
    }

    private void startEditing(State state) {
        drawingFragment.setState(state);
        if (state == State.BRUSH) {
            setFabResourceAlpha(fabBrush, R.drawable.ic_check, .3f);
            setFabResourceAlpha(fabSelect, R.drawable.ic_cross, .3f);
        } else if (state == State.SELECT) {
            setFabResourceAlpha(fabSelect, R.drawable.ic_check, .3f);
            setFabResourceAlpha(fabBrush, R.drawable.ic_cross, .3f);
        }
        editingMode();
    }

    public void clickImageContinue(View v) {
        if (!drawingFragment.sendJsonForParsing(this)) {
            drawingFragment.processImage(this);
        }
    }

    public void clickImageBack(View v) {
        drawingFragment.cancelOcrResult();
        decisionMode();
    }

    private void setFabResourceAlpha(FloatingActionButton fab, int resource, float alpha) {
        fab.setImageResource(resource);
        fab.setTag(resource);
        fab.setAlpha(alpha);
    }
    //endregion


    //region Modes
    private void choosingMode() {
        displayButtons();
    }

    private void decisionMode() {
        displayButtons(fabGood, fabDismiss, fabBrush, fabSelect);
    }

    private void scanMode() {
        displayButtons();
    }

    private void editingMode() {
        displayButtons(fabBrush, fabSelect);
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

        addFragment(choosingFragment);

        choosingMode();
    }

    private void showDrawingFragment(Bitmap bitmap) {
        drawingFragment = DrawingFragment.newInstance(bitmap);

        addFragment(drawingFragment);

        decisionMode();
    }

    private void showResultFragment(JSONObject jsonObject) {
        resultFragment = ResultFragment.newInstance(jsonObject);

        addFragment(resultFragment);

        displayButtons();
    }

    private void addFragment(Fragment fragment) {
        try {
            Fragment oldFragment = getSupportFragmentManager().findFragmentById(R.id.activityFragment);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (oldFragment != null) transaction.remove(oldFragment);
            transaction.add(R.id.activityFragment, fragment);
            transaction.commit();
        } catch (Exception e) {
            Log.e(TAG, "Error while adding a new fragment: " + e.getLocalizedMessage());
        }
    }
    //endregion

    public void showRequestButtons(boolean success) {
        int resource = success? R.drawable.ic_continue: R.drawable.ic_try_again;
        fabContinue.setImageResource(resource);
        fabContinue.setTag(resource);
        displayButtons(fabContinue, fabBack);
    }
}
