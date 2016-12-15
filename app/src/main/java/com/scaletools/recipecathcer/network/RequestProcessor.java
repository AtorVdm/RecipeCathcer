package com.scaletools.recipecathcer.network;

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.scaletools.recipecathcer.R;
import com.scaletools.recipecathcer.network.volley.VolleyMultipartRequest;
import com.scaletools.recipecathcer.network.volley.VolleySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ator on 04/10/16.
 */

public class RequestProcessor implements Parcelable {

    //region Constants
    private static final String TAG = "RequestProcessor";
    private static final int REQUEST_TIMEOUT = 5000;
    private static final int REQUEST_RETRIES = 2;
    public static final String OCR = "ocr";
    public static final String PARSE = "parse";
    //endregion


    //region Fields
    private List<Rect> responseBounds;
    private JSONObject jsonResult;
    private JSONObject jsonRecipe;
    //endregion


    public RequestProcessor() { }


    //region Send
    public void sendImageForRecognition(Context context, final byte[] imageBytes,
                                        @Nullable RequestQueue.RequestFinishedListener<NetworkResponse> listener) {
        String url = context.getResources().getString(R.string.api_url);

        final VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest
                (Request.Method.POST, url + OCR, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                processResponse(new String(response.data));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                handleErrorResponse(error);
            }
        }) {
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                // file name could found file base or direct access from real path
                // for now just get bitmap data from ImageView
                params.put("data", new DataPart("image.jpg", imageBytes, "image/jpeg"));
                return params;
            }
        };

        if (listener != null) {
            VolleySingleton.getInstance(context).getRequestQueue().addRequestFinishedListener(listener);
        }
        multipartRequest.setRetryPolicy(retryPolicy());
        multipartRequest.setTag(OCR);

        VolleySingleton.getInstance(context).addToRequestQueue(multipartRequest);
    }

    public boolean sendJsonForParsing(Context context,
                                      @Nullable RequestQueue.RequestFinishedListener<NetworkResponse> listener) {
        if (jsonResult == null) return false;
        String url = context.getResources().getString(R.string.api_url);

        final StringRequest stringRequest = new StringRequest
                (Request.Method.POST, url + PARSE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    // Fixing response problem with Task<string> response
                    response = response.substring(1, response.length() - 1).replace("\\\"", "\"");
                    jsonRecipe = new JSONObject(response);
                } catch (JSONException e) {
                    Log.e(TAG, "JSON PARSING ERROR: " + e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                handleErrorResponse(error);
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=ISO-8859-1";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return jsonResult.toString().getBytes("ISO-8859-1");
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "UNKNOWN ERROR: " + e.getMessage());
                    return new byte[1];
                }
            }
        };

        if (listener != null) {
            VolleySingleton.getInstance(context).getRequestQueue().addRequestFinishedListener(listener);
        }
        stringRequest.setRetryPolicy(retryPolicy());
        stringRequest.setTag(PARSE);

        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
        return true;
    }
    //endregion


    //region Helpers
    private void handleErrorResponse(VolleyError error) {
        NetworkResponse networkResponse = error.networkResponse;
        String errorMessage = "Unknown error";
        if (networkResponse == null) {
            if (error.getClass().equals(TimeoutError.class)) {
                errorMessage = "Request timeout";
            } else if (error.getClass().equals(NoConnectionError.class)) {
                errorMessage = "Failed to connect server";
            }
        } else {
            String result = new String(networkResponse.data);
            try {
                JSONObject response = new JSONObject(result);
                String status = response.getString("status");
                String message = response.getString("message");

                Log.e(TAG, "Error Status: " + status);
                Log.e(TAG, "Error Message: " + message);

                if (networkResponse.statusCode == 404) {
                    errorMessage = "Resource not found";
                } else if (networkResponse.statusCode == 401) {
                    errorMessage = message+" Please login again";
                } else if (networkResponse.statusCode == 400) {
                    errorMessage = message+ " Check your inputs";
                } else if (networkResponse.statusCode == 500) {
                    errorMessage = message+" Something is getting wrong";
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "Error" + errorMessage, error);
    }

    private void processResponse(String resultResponse) {
        List<Rect> rectangles = new ArrayList<>();
        try {
            jsonResult = new JSONObject(resultResponse);
            JSONArray lines = jsonResult
                    .getJSONArray("ParsedResults")
                    .getJSONObject(0)
                    .getJSONObject("TextOverlay")
                    .getJSONArray("Lines");

            for (int i = 0; i < lines.length(); i++) {
                JSONArray words = lines.getJSONObject(i).getJSONArray("Words");
                for (int j = 0; j < words.length(); j++) {
                    int left = words.getJSONObject(j).getInt("Left");
                    int top = words.getJSONObject(j).getInt("Top");
                    int width = words.getJSONObject(j).getInt("Width");
                    int height = words.getJSONObject(j).getInt("Height");
                    rectangles.add(new Rect(left, top, left + width, top + height));
                }
            }

            responseBounds = rectangles;
            String status = jsonResult.getString("OCRExitCode");
            String message = jsonResult.getString("ErrorMessage");

            if (status.equals("1")) {
                Log.i(TAG, "Request success!");
            } else {
                Log.e(TAG, "ERROR DURING OCR: " + message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private RetryPolicy retryPolicy() {
        return new DefaultRetryPolicy(REQUEST_TIMEOUT,
                REQUEST_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }
    //endregion


    //region Get
    public List<Rect> getResponseBounds() {
        return responseBounds;
    }

    public JSONObject getJsonRecipe() {
        return jsonRecipe;
    }
    //endregion


    //region Parcelable
    protected RequestProcessor(Parcel in) {
        responseBounds = in.createTypedArrayList(Rect.CREATOR);
        try {
            jsonResult = new JSONObject(in.readString());
            jsonRecipe = new JSONObject(in.readString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static final Creator<RequestProcessor> CREATOR = new Creator<RequestProcessor>() {
        @Override
        public RequestProcessor createFromParcel(Parcel in) {
            return new RequestProcessor(in);
        }

        @Override
        public RequestProcessor[] newArray(int size) {
            return new RequestProcessor[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(responseBounds);
        parcel.writeString(jsonResult.toString());
        parcel.writeString(jsonRecipe.toString());
    }
    //endregion
}
