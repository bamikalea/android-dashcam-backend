package com.fleetmanagement.custom.services;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fleetmanagement.custom.config.ServerConfig;

public class HttpTestTask implements Runnable {
    private static final String TAG = "HttpTestTask";

    @Override
    public void run() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        Request request = new Request.Builder()
                .url(ServerConfig.getStatusUrl())
                .addHeader("User-Agent", "FleetManagement/1.0")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                Log.i(TAG, "HTTP TEST SUCCESS: " + response.code() + " - " + response.body().string());
            } else {
                Log.w(TAG, "HTTP TEST FAIL: " + response.code() + " - " + response.message());
            }
        } catch (IOException e) {
            Log.e(TAG, "HTTP TEST ERROR: " + e.getMessage());
        }
    }
}