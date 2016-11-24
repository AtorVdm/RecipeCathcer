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
 * A simple {@link Fragment} subclass.
 * Use the {@link ResultFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ResultFragment extends Fragment {
    private TextView textView;
    private String resultText = "Error!";
    public ResultFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ResultFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ResultFragment newInstance() {
        ResultFragment fragment = new ResultFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        textView = (TextView) view.findViewById(R.id.resultText);
        textView.setText(resultText);
    }

    public void showResult(JSONObject resultJson) {
        if (resultJson != null) {
            if (textView != null)
                textView.setText(resultJson.toString());
            else
                resultText = resultJson.toString();
        }
    }
}
