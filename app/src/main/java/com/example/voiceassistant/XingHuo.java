package com.example.voiceassistant;

import android.os.Build;
import android.support.annotation.RequiresApi;

import okhttp3.*;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class XingHuo {
    private static final String APPID = "f921cd8e";
    private static final String API_SECRET = "ZDJhZWZhOWFiZmUxMTdmNzllOTBkYzAy";
    private static final String API_KEY = "c5a224ff88254fc96c60850ae901fa68";
    private static final String GPT_URL = "wss://spark-api.xf-yun.com/v3.5/chat";
    private static final String DOMAIN = "generalv3.5";

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void main(String[] args) {
        // Example usage
        String query = "天气"; // External input
        String response = getResponse(query);
        System.out.println("Response: " + response);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getResponse(String query) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(createUrl(query))
                .build();

        // Response from the WebSocket
        final StringBuilder responseBuilder = new StringBuilder();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                String data = genParams(APPID, query, DOMAIN);
                webSocket.send(data);
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                responseBuilder.append(text); // Append WebSocket response
            }

            @Override
            public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                System.out.println("### closed ###");
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, Response response) {
                t.printStackTrace();
            }
        });

        // Wait for WebSocket response
        try {
            Thread.sleep(5000); // Adjust timeout as needed
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return responseBuilder.toString();
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String createUrl(String query) {
        String date = getRFC1123Date();
        String host = "spark-api.xf-yun.com";
        String path = "/v3.5/chat";

        String signatureOrigin = "host: " + host + "\n"
                + "date: " + date + "\n"
                + "GET " + path + " HTTP/1.1";

        String signatureSha = hmacSHA256(API_SECRET, signatureOrigin);
        String authorizationOrigin = String.format(Locale.US, "api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"", API_KEY, signatureSha);
        String authorization = base64Encode(authorizationOrigin);

        return GPT_URL + "?" + "authorization=" + authorization + "&date=" + date + "&host=" + host;
    }

    private static String hmacSHA256(String key, String data) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] signatureBytes = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String base64Encode(String input) {
        return java.util.Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String genParams(String appid, String query, String domain) {
        return "{\"header\":{\"app_id\":\"" + appid + "\",\"uid\":\"1234\"},\"parameter\":{\"chat\":{\"domain\":\"" + domain + "\",\"temperature\":0.5,\"max_tokens\":4096,\"auditing\":\"default\"}},\"payload\":{\"message\":{\"text\":[{\"role\":\"user\",\"content\":\"" + query + "\"}]}}}";
    }

    private static String getRFC1123Date() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }
}
