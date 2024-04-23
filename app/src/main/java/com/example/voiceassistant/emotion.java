package com.example.voiceassistant;


import android.os.AsyncTask;

import okhttp3.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

class emotion {
    private static final String API_KEY = "9yfwBfjMjCgDPn569m5GRix0";
    private static final String SECRET_KEY = "NinXPC3SIM1Q2NnQQLVofQQCbOlKNCc8";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    public static void getResponseInBackground(String text, ResponseCallback callback) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... texts) {
                try {
                    return getResponse(texts[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String response) {
                super.onPostExecute(response);
                if (response != null) {
                    callback.onSuccess(response);
                } else {
                    callback.onError("Failed to get response");
                }
            }
        }.execute(text);
    }

    private static String getResponse(String text) throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"text\":\"" + text + "\"}");
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/rpc/2.0/nlp/v1/sentiment_classify?charset=UTF-8&access_token=" + getAccessToken())
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        return response.body().string();
    }

    private static String getAccessToken() throws IOException {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id=" + API_KEY
                + "&client_secret=" + SECRET_KEY);
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        try {
            return new JSONObject(response.body().string()).getString("access_token");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public interface ResponseCallback {
        void onSuccess(String response);
        void onError(String error);
    }}
