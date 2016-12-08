package com.scaletools.recipecathcer.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.scaletools.recipecathcer.R;

import org.json.JSONObject;

/**
 * Sample response fragment with full screen TextView, should be replaced with a proper result fragment
 */
public class ResultFragment extends Fragment {
    private static final String ARG_JSON = "json";

    public ResultFragment() {}

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @param jsonObject a recipe json
     * @return A new instance of fragment ResultFragment.
     */
    public static ResultFragment newInstance(JSONObject jsonObject) {
        ResultFragment fragment = new ResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_JSON, jsonObject.toString());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // DO NOT REMOVE! Inflates the layout for this fragment
        return inflater.inflate(R.layout.fragment_result, container, false);
    }


    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        TextView textView = (TextView) view.findViewById(R.id.resultText);

        if (getArguments() != null) {
            String resultText = getArguments().getString(ARG_JSON);
            if (resultText != null) {
                textView.setText(resultText);
            } else {
                textView.setText("ERROR");
            }
        }
    }
}
