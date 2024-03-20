package com.example.voiceassistant;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class QingyunkeChatBot {

    private static final String API_URL = "http://api.qingyunke.com/api.php?key=free&appid=0&msg=";

    public interface ResponseListener {
        void onResponseReceived(String response);
        void onError(String error);
    }

    public void getResponse(String message, ResponseListener listener) {
        new RetrieveResponseTask(listener).execute(message);
    }

    private static class RetrieveResponseTask extends AsyncTask<String, Void, String> {

        private ResponseListener listener;

        RetrieveResponseTask(ResponseListener listener) {
            this.listener = listener;
        }

        @Override
        protected String doInBackground(String... strings) {
            String message = strings[0];
            String apiUrl = API_URL + message;
            StringBuilder response = new StringBuilder();
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();
            } catch (IOException e) {
                Log.e("RetrieveResponseTask", "Error retrieving response", e);
                if (listener != null) {
                    listener.onError("Error retrieving response");
                }
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonResponse = new JSONObject(s);
                String content = jsonResponse.getString("content");
                if (listener != null) {
                    listener.onResponseReceived(content);
                }
            } catch (JSONException e) {
                Log.e("RetrieveResponseTask", "Error parsing JSON response", e);
                if (listener != null) {
                    listener.onError("Error parsing JSON response");
                }
            }
        }
    }
}

