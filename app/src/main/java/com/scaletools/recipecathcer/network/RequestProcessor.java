package com.scaletools.recipecathcer.network;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.scaletools.recipecathcer.volley.*;

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

public class RequestProcessor {
    private static final String TAG = "RequestProcessor";
    private static final String URL = "http://192.168.0.104/RecipeParser/api/recipe/";
    private static final String URL_OCR = URL + "ocr";
    private static final String URL_PARSE = URL + "parse";

    private List<Rect> responseBounds;
    private JSONObject jsonResult;
    private JSONObject jsonRecipe;

    public void requestImageTextHighlights(final Context context, byte[] imageBytes) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);
        Log.d(TAG, "Clicked...");

// Request a string response from the provided URL.

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_OCR,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d(TAG, "Response is: " + response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error is: " + error.getLocalizedMessage());
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void sendImageForRecognition(Context context, final byte[] imageBytes, @Nullable RequestQueue.RequestFinishedListener<NetworkResponse> listener) {
        final VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, URL_OCR, new Response.Listener<NetworkResponse>() {
            @Override
            public void onResponse(NetworkResponse response) {
                List<Rect> rectangles = new ArrayList<>();
                String resultResponse = new String(response.data);

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
                        // tell everybody you have succed upload image and post strings
                        Log.i("INFO", "Request success!");
                    } else {
                        Log.i("ERROR", message);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
                params.put("data", new DataPart("test.jpg", imageBytes, "image/jpeg"));
                return params;
            }
        };

        if (listener != null) {
            VolleySingleton.getInstance(context).getRequestQueue().addRequestFinishedListener(listener);
        }

        VolleySingleton.getInstance(context).addToRequestQueue(multipartRequest);
    }

    public List<Rect> getResponseBounds() {
        return responseBounds;
    }

    public boolean sendJsonForParsing(Context context, @Nullable RequestQueue.RequestFinishedListener<NetworkResponse> listener) {
        if (jsonResult == null) return false;

        final StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_PARSE, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    // Fixing response problem with Task<string> response
                    response = response.substring(1, response.length() - 1).replace("\\\"", "\"");
                    jsonRecipe = new JSONObject(response);
                } catch (JSONException e) {
                    Log.e("JSON ERROR", e.getMessage());
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
                    Log.e("UNKNOWN ERROR", e.getMessage());
                    return new byte[1];
                }
            }
        };

        if (listener != null) {
            VolleySingleton.getInstance(context).getRequestQueue().addRequestFinishedListener(listener);
        }

        VolleySingleton.getInstance(context).addToRequestQueue(stringRequest);
        return true;
    }

    public JSONObject getJsonRecipe() {
        return jsonRecipe;
    }

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

                Log.e("Error Status", status);
                Log.e("Error Message", message);

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
        Log.i("Error", errorMessage);
        error.printStackTrace();
    }

}
