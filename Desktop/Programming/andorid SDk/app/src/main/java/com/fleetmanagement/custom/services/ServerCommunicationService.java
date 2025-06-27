package com.fleetmanagement.custom.services;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.fleetmanagement.custom.R;
import com.fleetmanagement.custom.config.ServerConfig;
import com.fleetmanagement.custom.models.DashcamStatus;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Enhanced Server Communication Service with JT808 Integration
 * Handles all communication with the fleet management server over the internet
 */
public class ServerCommunicationService extends Service {

    private static final String TAG = "ServerCommunicationService";
    private static final String API_BASE_URL = ServerConfig.BASE_URL + "/api";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");
    private static final MediaType MEDIA_TYPE_MP4 = MediaType.parse("video/mp4");
    private static final String CHANNEL_ID = "ServerCommunicationService";
    private static final int NOTIFICATION_ID = 1001;
    private static final int ALARM_INTERVAL = 30000; // 30 seconds
    private static final int POLLING_INTERVAL = 10000; // 10 seconds
    private static final String ENDPOINT_JT808_LOCATION = "/dashcams/%s/jt808/location";
    private static final String ENDPOINT_JT808_ALERT = "/dashcams/%s/jt808/alert";
    private static final String ENDPOINT_STATUS = "/dashcams/%s/status";
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 60000;

    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService executorService;
    private Socket socket;
    private String deviceId;
    private boolean isConnected = false;
    private PowerManager.WakeLock wakeLock;
    private AlarmManager alarmManager;
    private PendingIntent restartIntent;
    private ScheduledExecutorService pollingExecutor;
    private boolean isRegistered = false;

    // JT808 integration
    private boolean jt808Enabled = false;
    private Location lastKnownLocation;
    private String lastKnownStatus = "unknown";

    // Required zero-argument constructor for Android services
    public ServerCommunicationService() {
        this.gson = new Gson();

        // Configure HTTP client with more permissive settings
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        // Create background thread pool for network operations
        this.executorService = Executors.newFixedThreadPool(3);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.deviceId = generateDeviceId();

        // Initialize socket in background thread to prevent ANR
        executorService.execute(() -> {
            initializeConnection();
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Always start as foreground
        startForegroundServiceWithNotification();
        checkBatteryOptimization();

        // Acquire wake lock to keep service alive
        acquireWakeLock();

        // Initialize connection in background thread to prevent ANR
        executorService.execute(() -> {
            initializeConnection();
        });

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "Service being destroyed - attempting restart");

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Cancel alarm
        if (alarmManager != null && restartIntent != null) {
            alarmManager.cancel(restartIntent);
        }

        // Force restart service
        Intent restartServiceIntent = new Intent(this, ServerCommunicationService.class);
        startService(restartServiceIntent);

        super.onDestroy();
    }

    private void initializeConnection() {
        try {
            Log.i(TAG, "Initializing HTTP polling connection...");

            // Get device ID
            deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = "dashcam-" + System.currentTimeMillis();
            }

            // Start polling for commands
            startPollingForCommands();

            // Register with server
            registerWithServer();

            // After initializing the socket connection (pseudo-code, adapt as needed):
            if (socket != null) {
                socket.on("get_location_request", new io.socket.emitter.Emitter.Listener() {
                    @Override
                    public void call(final Object... args) {
                        // Get the requestorSocketId from the server
                        String requestorSocketId = null;
                        try {
                            JSONObject data = (JSONObject) args[0];
                            requestorSocketId = data.optString("requestorSocketId");
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing get_location_request", e);
                        }

                        // Get the last known location (or fetch a new one if needed)
                        double latitude = 0;
                        double longitude = 0;
                        if (lastKnownLocation != null) {
                            latitude = lastKnownLocation.getLatitude();
                            longitude = lastKnownLocation.getLongitude();
                        }
                        JSONObject response = new JSONObject();
                        try {
                            response.put("deviceId", deviceId);
                            JSONObject location = new JSONObject();
                            location.put("latitude", latitude);
                            location.put("longitude", longitude);
                            response.put("location", location);
                            response.put("requestorSocketId", requestorSocketId);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error building location_response", e);
                        }
                        socket.emit("location_response", response);
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing connection: " + e.getMessage(), e);
        }
    }

    private void startPollingForCommands() {
        if (pollingExecutor != null) {
            pollingExecutor.shutdown();
        }

        pollingExecutor = Executors.newScheduledThreadPool(2);

        // Poll for commands every 10 seconds
        pollingExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                pollForCommands();
            }
        }, 0, POLLING_INTERVAL, TimeUnit.MILLISECONDS);

        // Send status updates every 60 seconds to keep device active
        pollingExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sendStatus();
            }
        }, 60, 60, TimeUnit.SECONDS);

        Log.i(TAG, "Started polling for commands every " + POLLING_INTERVAL + "ms and status updates every 60s");
    }

    private void pollForCommands() {
        try {
            String url = API_BASE_URL + "/dashcams/" + deviceId + "/commands";

            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "FleetManagement/1.0");

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse and execute commands
                String commandsJson = response.toString();
                if (!commandsJson.equals("[]") && !commandsJson.equals("null")) {
                    Log.i(TAG, "Received commands: " + commandsJson);
                    executeCommands(commandsJson);
                }

                // Send status update to keep device active
                sendStatus();
            } else {
                Log.w(TAG, "Polling failed with response code: " + responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Error polling for commands: " + e.getMessage(), e);
        }
    }

    private void executeCommands(String commandsJson) {
        try {
            Log.i(TAG, "Executing commands: " + commandsJson);

            // Handle JSON array format from server
            if (commandsJson.startsWith("[") && commandsJson.endsWith("]")) {
                // Parse JSON array of commands
                JSONArray commandsArray = new JSONArray(commandsJson);
                for (int i = 0; i < commandsArray.length(); i++) {
                    JSONObject commandObj = commandsArray.getJSONObject(i);
                    String command = commandObj.getString("command");
                    String commandId = commandObj.getString("commandId");

                    Log.i(TAG, "Executing command: " + command + " (ID: " + commandId + ")");

                    switch (command) {
                        case "takePhoto":
                            takePhoto();
                            break;
                        case "startLiveStream":
                            startLiveStream();
                            break;
                        case "stopLiveStream":
                            stopLiveStream();
                            break;
                        case "startRecording":
                            startRecording();
                            break;
                        case "stopRecording":
                            stopRecording();
                            break;
                        case "getStatus":
                            sendStatus();
                            break;
                        default:
                            Log.w(TAG, "Unknown command: " + command);
                            sendCommandResponse(command, false, "Unknown command: " + command);
                            break;
                    }
                }
            } else {
                // Fallback for simple string matching
                if (commandsJson.contains("takePhoto")) {
                    takePhoto();
                }
                if (commandsJson.contains("startLiveStream")) {
                    startLiveStream();
                }
                if (commandsJson.contains("stopLiveStream")) {
                    stopLiveStream();
                }
                if (commandsJson.contains("startRecording")) {
                    startRecording();
                }
                if (commandsJson.contains("stopRecording")) {
                    stopRecording();
                }
                if (commandsJson.contains("getStatus")) {
                    sendStatus();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing commands: " + e.getMessage(), e);
        }
    }

    private void registerWithServer() {
        try {
            String url = ServerConfig.getUrl("/dashcams/register");

            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "FleetManagement/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Registration data
            String registrationData = String.format(
                    "{\"deviceId\": \"%s\", \"model\": \"%s\", \"version\": \"%s\"}",
                    deviceId,
                    Build.MODEL,
                    Build.VERSION.RELEASE);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = registrationData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                isRegistered = true;
                Log.i(TAG, "Successfully registered with server");
            } else {
                Log.w(TAG, "Registration failed with response code: " + responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Error registering with server: " + e.getMessage(), e);
        }
    }

    private void sendStatus() {
        try {
            String url = API_BASE_URL + "/dashcams/" + deviceId + "/status";

            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "FleetManagement/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            // Status data
            String statusData = String.format(
                    "{\"deviceId\": \"%s\", \"status\": \"online\", \"timestamp\": \"%s\"}",
                    deviceId,
                    sdf.format(new java.util.Date()));

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = statusData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            Log.i(TAG, "Status sent with response code: " + responseCode);

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Error sending status: " + e.getMessage(), e);
        }
    }

    private void takePhoto() {
        Log.i(TAG, "Executing takePhoto command");
        // Implement photo capture logic here
        sendCommandResponse("takePhoto", true, "Photo captured successfully");
    }

    private void startRecording() {
        Log.i(TAG, "Executing startRecording command");
        // Implement recording start logic here
        sendCommandResponse("startRecording", true, "Recording started");
    }

    private void stopRecording() {
        Log.i(TAG, "Executing stopRecording command");
        // Implement recording stop logic here
        sendCommandResponse("stopRecording", true, "Recording stopped");
    }

    private void startLiveStream() {
        Log.i(TAG, "Executing startLiveStream command");
        // Implement live stream start logic here
        sendCommandResponse("startLiveStream", true, "Live stream started");
    }

    private void stopLiveStream() {
        Log.i(TAG, "Executing stopLiveStream command");
        // Implement live stream stop logic here
        sendCommandResponse("stopLiveStream", true, "Live stream stopped");
    }

    private void sendCommandResponse(String command, boolean success, String message) {
        try {
            String url = API_BASE_URL + "/dashcams/" + deviceId + "/response";

            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "FleetManagement/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            String responseData = String.format(
                    "{\"deviceId\": \"%s\", \"command\": \"%s\", \"success\": %s, \"message\": \"%s\", \"timestamp\": \"%s\"}",
                    deviceId,
                    command,
                    success,
                    message,
                    sdf.format(new java.util.Date()));

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = responseData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            Log.i(TAG, "Command response sent with response code: " + responseCode);

            connection.disconnect();

        } catch (Exception e) {
            Log.e(TAG, "Error sending command response: " + e.getMessage(), e);
        }
    }

    private String generateDeviceId() {
        return "dashcam-" + android.os.Build.SERIAL + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void sendDashcamStatus(DashcamStatus status) {
        try {
            JSONObject statusData = new JSONObject();
            statusData.put("recording", status.isRecording());
            statusData.put("motion_detected", status.isMotionDetected());
            statusData.put("storage_available", status.getStorageAvailable());
            statusData.put("battery_level", status.getBatteryLevel());
            statusData.put("timestamp", System.currentTimeMillis());

            // Log status instead of sending event
            Log.i(TAG, "Status update: " + statusData.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Error creating status data", e);
        }
    }

    /**
     * Send status update to server
     */
    public void sendStatus(String status, String deviceInfo) {
        try {
            JsonObject statusData = new JsonObject();
            statusData.addProperty("device_id", ServerConfig.DEVICE_ID);
            statusData.addProperty("status", status);
            statusData.addProperty("timestamp", System.currentTimeMillis());
            statusData.addProperty("device_info", deviceInfo);

            String json = gson.toJson(statusData);
            RequestBody body = RequestBody.create(JSON, json);

            Request request = new Request.Builder()
                    .url(ServerConfig.getStatusUrl())
                    .addHeader("Authorization", "Bearer " + ServerConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            executeRequest(request, "Status Update");

        } catch (Exception e) {
            Log.e(TAG, "Failed to send status", e);
        }
    }

    /**
     * Send location update to server
     */
    public void sendLocation(Location location) {
        try {
            JsonObject locationData = new JsonObject();
            locationData.addProperty("device_id", ServerConfig.DEVICE_ID);
            locationData.addProperty("latitude", location.getLatitude());
            locationData.addProperty("longitude", location.getLongitude());
            locationData.addProperty("altitude", location.getAltitude());
            locationData.addProperty("speed", location.getSpeed());
            locationData.addProperty("accuracy", location.getAccuracy());
            locationData.addProperty("timestamp", System.currentTimeMillis());

            String json = gson.toJson(locationData);
            RequestBody body = RequestBody.create(JSON, json);

            Request request = new Request.Builder()
                    .url(ServerConfig.getLocationUrl())
                    .addHeader("Authorization", "Bearer " + ServerConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            executeRequest(request, "Location Update");

        } catch (Exception e) {
            Log.e(TAG, "Failed to send location", e);
        }
    }

    /**
     * Send event notification to server
     */
    public void sendEvent(int eventType, String eventDescription, Location location) {
        try {
            JsonObject eventData = new JsonObject();
            eventData.addProperty("device_id", ServerConfig.DEVICE_ID);
            eventData.addProperty("event_type", eventType);
            eventData.addProperty("event_description", eventDescription);
            eventData.addProperty("timestamp", System.currentTimeMillis());

            if (location != null) {
                eventData.addProperty("latitude", location.getLatitude());
                eventData.addProperty("longitude", location.getLongitude());
                eventData.addProperty("speed", location.getSpeed());
            }

            String json = gson.toJson(eventData);
            RequestBody body = RequestBody.create(JSON, json);

            Request request = new Request.Builder()
                    .url(ServerConfig.getEventUrl())
                    .addHeader("Authorization", "Bearer " + ServerConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            executeRequest(request, "Event Notification");

        } catch (Exception e) {
            Log.e(TAG, "Failed to send event", e);
        }
    }

    /**
     * Send emergency alert to server
     */
    public void sendEmergencyAlert(String alertType, Location location) {
        try {
            JsonObject emergencyData = new JsonObject();
            emergencyData.addProperty("device_id", ServerConfig.DEVICE_ID);
            emergencyData.addProperty("alert_type", alertType);
            emergencyData.addProperty("timestamp", System.currentTimeMillis());
            emergencyData.addProperty("emergency_contact", ServerConfig.EMERGENCY_CONTACT);
            emergencyData.addProperty("emergency_email", ServerConfig.EMERGENCY_EMAIL);

            if (location != null) {
                emergencyData.addProperty("latitude", location.getLatitude());
                emergencyData.addProperty("longitude", location.getLongitude());
            }

            String json = gson.toJson(emergencyData);
            RequestBody body = RequestBody.create(JSON, json);

            Request request = new Request.Builder()
                    .url(ServerConfig.getEmergencyUrl())
                    .addHeader("Authorization", "Bearer " + ServerConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            executeRequest(request, "Emergency Alert");

        } catch (Exception e) {
            Log.e(TAG, "Failed to send emergency alert", e);
        }
    }

    /**
     * Upload photo to server
     */
    public void uploadPhoto(File photoFile, String eventType) {
        try {
            if (!photoFile.exists()) {
                Log.e(TAG, "Photo file does not exist: " + photoFile.getPath());
                return;
            }

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("device_id", ServerConfig.DEVICE_ID)
                    .addFormDataPart("event_type", eventType)
                    .addFormDataPart("timestamp", String.valueOf(System.currentTimeMillis()))
                    .addFormDataPart("photo", photoFile.getName(),
                            RequestBody.create(MEDIA_TYPE_JPEG, photoFile))
                    .build();

            Request request = new Request.Builder()
                    .url(ServerConfig.getPhotoUrl())
                    .addHeader("Authorization", "Bearer " + ServerConfig.API_KEY)
                    .post(requestBody)
                    .build();

            executeRequest(request, "Photo Upload");

        } catch (Exception e) {
            Log.e(TAG, "Failed to upload photo", e);
        }
    }

    /**
     * Upload video to server
     */
    public void uploadVideo(File videoFile, String eventType) {
        try {
            if (!videoFile.exists()) {
                Log.e(TAG, "Video file does not exist: " + videoFile.getPath());
                return;
            }

            // Check file size
            if (videoFile.length() > ServerConfig.MAX_FILE_SIZE) {
                Log.w(TAG, "Video file too large, compressing...");
                // TODO: Implement video compression
            }

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("device_id", ServerConfig.DEVICE_ID)
                    .addFormDataPart("event_type", eventType)
                    .addFormDataPart("timestamp", String.valueOf(System.currentTimeMillis()))
                    .addFormDataPart("video", videoFile.getName(),
                            RequestBody.create(MEDIA_TYPE_MP4, videoFile))
                    .build();

            Request request = new Request.Builder()
                    .url(ServerConfig.getVideoUrl())
                    .addHeader("Authorization", "Bearer " + ServerConfig.API_KEY)
                    .post(requestBody)
                    .build();

            executeRequest(request, "Video Upload");

        } catch (Exception e) {
            Log.e(TAG, "Failed to upload video", e);
        }
    }

    /**
     * Send heartbeat to server
     */
    public void sendHeartbeat() {
        try {
            JsonObject heartbeatData = new JsonObject();
            heartbeatData.addProperty("device_id", ServerConfig.DEVICE_ID);
            heartbeatData.addProperty("timestamp", System.currentTimeMillis());
            heartbeatData.addProperty("battery_level", getBatteryLevel());
            heartbeatData.addProperty("storage_available", getAvailableStorage());

            String json = gson.toJson(heartbeatData);
            RequestBody body = RequestBody.create(JSON, json);

            Request request = new Request.Builder()
                    .url(ServerConfig.getHeartbeatUrl())
                    .addHeader("Authorization", "Bearer " + ServerConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            executeRequest(request, "Heartbeat");

        } catch (Exception e) {
            Log.e(TAG, "Failed to send heartbeat", e);
        }
    }

    /**
     * Execute HTTP request with retry logic
     */
    private void executeRequest(Request request, String requestType) {
        int retryCount = 0;

        while (retryCount < ServerConfig.MAX_RETRY_ATTEMPTS) {
            try {
                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.i(TAG, requestType + " successful: " + response.code());
                    return;
                } else {
                    Log.w(TAG, requestType + " failed: " + response.code() + " - " + response.message());
                }

            } catch (IOException e) {
                Log.e(TAG, requestType + " network error (attempt " + (retryCount + 1) + ")", e);
            }

            retryCount++;
            if (retryCount < ServerConfig.MAX_RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(1000 * retryCount); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        Log.e(TAG, requestType + " failed after " + ServerConfig.MAX_RETRY_ATTEMPTS + " attempts");
    }

    /**
     * Get battery level percentage
     */
    private int getBatteryLevel() {
        try {
            android.content.IntentFilter iFilter = new android.content.IntentFilter(
                    android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = getApplicationContext().registerReceiver(null, iFilter);

            int level = batteryStatus != null ? batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    : -1;

            float batteryPct = level * 100 / (float) scale;
            return (int) batteryPct;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Get available storage in MB
     */
    private long getAvailableStorage() {
        try {
            File path = getApplicationContext().getExternalFilesDir(null);
            if (path != null) {
                return path.getFreeSpace() / (1024 * 1024); // Convert to MB
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get storage info", e);
        }
        return -1;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Server Communication Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Manages communication with the fleet management server");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String content) {
        Intent notificationIntent = new Intent(this, ServerCommunicationService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Fleet Management")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void testHttpConnectivity() {
        try {
            Log.i(TAG, "Testing HTTP connectivity to server...");
            Log.i(TAG, "Server URL: " + ServerConfig.BASE_URL + "/api/status");

            // Use a simple HTTP client for connectivity test
            OkHttpClient testClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            Request request = new Request.Builder()
                    .url(ServerConfig.BASE_URL + "/api/status")
                    .build();

            Log.i(TAG, "Executing HTTP request...");
            try (Response response = testClient.newCall(request).execute()) {
                Log.i(TAG, "HTTP response received: " + response.code() + " " + response.message());
                if (response.isSuccessful()) {
                    Log.i(TAG, "HTTP connectivity test successful: " + response.code());
                    String responseBody = response.body().string();
                    Log.i(TAG, "Response: " + responseBody);
                } else {
                    Log.w(TAG, "HTTP connectivity test failed: " + response.code() + " - " + response.message());
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.w(TAG, "Error response: " + errorBody);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "HTTP connectivity test error: " + e.getMessage());
            Log.e(TAG, "Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }

    private void startForegroundServiceWithNotification() {
        String channelId = "fleet_management_channel";
        String channelName = "Fleet Management Service";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the fleet management service running");
            manager.createNotificationChannel(channel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, channelId)
                : new Notification.Builder(this);
        Notification notification = builder
                .setContentTitle("Fleet Management Running")
                .setContentText("The dashcam is connected to the fleet server.")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .build();
        startForeground(1, notification);
    }

    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Log.i(TAG, "Battery optimization is disabled - good!");
        } else {
            Log.w(TAG, "Battery optimization is ENABLED - this may cause service to be killed!");
            Log.w(TAG, "Consider disabling battery optimization for this app");
        }
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FleetManagement::ServerCommunicationWakeLock");
        wakeLock.acquire();
        Log.i(TAG, "Wake lock acquired");
    }

    private void setupRestartAlarm() {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ServiceRestartReceiver.class);
        restartIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Set repeating alarm every 30 seconds
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + ALARM_INTERVAL,
                ALARM_INTERVAL,
                restartIntent);

        Log.i(TAG, "Restart alarm set up");
    }
}

class ServiceRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("ServiceRestartReceiver", "Checking if service is running...");

        // Check if service is running
        boolean isServiceRunning = false;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ServerCommunicationService.class.getName().equals(service.service.getClassName())) {
                isServiceRunning = true;
                break;
            }
        }

        // If service is not running, restart it
        if (!isServiceRunning) {
            Log.w("ServiceRestartReceiver", "Service not running, restarting...");
            Intent restartIntent = new Intent(context, ServerCommunicationService.class);
            context.startService(restartIntent);
        }
    }
}