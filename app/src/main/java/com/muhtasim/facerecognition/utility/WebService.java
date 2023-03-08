package com.muhtasim.facerecognition.utility;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WebService {

    private static JSONObject fallbackJsonObject = null;

    static {
        try {
            fallbackJsonObject = new JSONObject("{\"" + Constants.API_VAR_SUCCESS + "\":\"false\",\"" + Constants.API_VAR_ERROR + "\":\"" + Constants.ERR_UNKNOWN_ERROR + "\"}");
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
    }

    private final WebServiceResponseListener webServiceResponseListener;

    public WebService(WebServiceResponseListener webServiceResponseListener) {
        this.webServiceResponseListener = webServiceResponseListener;
    }

    /* Method to perform a GET request */
    public void getRequest(Context context, String url) {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            try {
                this.webServiceResponseListener.onResultResponseListener(new JSONObject(response));
            } catch (JSONException jsonException) {
                /* Log jsonException for debugging purposes and return fallback object */
                jsonException.printStackTrace();
                this.webServiceResponseListener.onResultResponseListener(fallbackJsonObject);
            }
        }, error -> {
            try {
                this.webServiceResponseListener.onResultResponseListener(new JSONObject(new String(error.networkResponse.data, StandardCharsets.UTF_8)));
            } catch (JSONException jsonException) {
                /* Log jsonException for debugging purposes and return fallback object */
                jsonException.printStackTrace();
                this.webServiceResponseListener.onResultResponseListener(fallbackJsonObject);
            }
        });
        Volley.newRequestQueue(context).add(stringRequest);
    }

    /* Method to perform a POST request */
    public void postRequest(Context context, String url, JSONObject rawData) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, rawData, this.webServiceResponseListener::onResultResponseListener, error -> {
            try {
                this.webServiceResponseListener.onResultResponseListener(new JSONObject(new String(error.networkResponse.data, StandardCharsets.UTF_8)));
            } catch (JSONException jsonException) {
                /* Log jsonException for debugging purposes and return fallback object */
                jsonException.printStackTrace();
                this.webServiceResponseListener.onResultResponseListener(fallbackJsonObject);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                return params;
            }
        };
        Volley.newRequestQueue(context).add(jsonObjectRequest);
    }

    public interface WebServiceResponseListener {
        void onResultResponseListener(JSONObject resultResponse);
    }

}
