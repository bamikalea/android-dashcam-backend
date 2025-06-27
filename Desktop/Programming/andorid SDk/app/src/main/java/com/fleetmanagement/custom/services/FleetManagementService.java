package com.fleetmanagement.custom.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.fleetmanagement.custom.R;
import com.fleetmanagement.custom.models.DashcamStatus;
import com.fleetmanagement.custom.receivers.DashcamEventReceiver;
import com.fleetmanagement.custom.config.ServerConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import carassist.cn.API;

public class FleetManagementService extends Service implements API.CarMotionListener {

    private static final String TAG = "FleetManagementService";
    private static final String CHANNEL_ID = "FleetManagementChannel";
    private static final int NOTIFICATION_ID = 1001;

    private API dashcamAPI;
    private DashcamStatus dashcamStatus;
    private boolean isInitialized = false;
    private final ExecutorService executorService;

    public FleetManagementService() {
        // Create background thread pool for heavy operations
        this.executorService = Executors.newFixedThreadPool(2);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "[DEBUG] Fleet Management Service onCreate - starting");
        createNotificationChannel();
        Log.i(TAG, "[DEBUG] Notification channel created");
        // Start foreground notification immediately
        startForeground(NOTIFICATION_ID, createNotification());
        Log.i(TAG, "[DEBUG] Foreground notification started");
        dashcamStatus = new DashcamStatus();
        // Set deviceId using Android ID
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        dashcamStatus.setDeviceId(androidId);
        Log.i(TAG, "[DEBUG] Device ID set: " + androidId);
        // Block, kill, and disable companion app
        try {
            carassist.cn.API api = new carassist.cn.API(getApplicationContext());
            api.setPackageNetrule("com.car.dvr", false);
            Log.i(TAG, "[DEBUG] Blocked companion app network access");
            // Try to force-stop and disable the companion app
            Runtime.getRuntime().exec("am force-stop com.car.dvr");
            Log.i(TAG, "[DEBUG] am force-stop issued for companion app");
            Runtime.getRuntime().exec("pm disable-user --user 0 com.car.dvr");
            Log.i(TAG, "[DEBUG] pm disable-user issued for companion app");
        } catch (Exception e) {
            Log.e(TAG, "[DEBUG] Failed to block/kill/disable companion app", e);
        }
        // Check and update server URL if needed (placeholder, to be replaced with
        // actual URL)
        String currentNgrokUrl = "https://android-dashcam-backend.onrender.com";
        if (!ServerConfig.BASE_URL.equals(currentNgrokUrl)) {
            Log.i(TAG, "[DEBUG] Updating SERVER_BASE_URL to: " + currentNgrokUrl);
            // Update logic here if needed
        }
        Log.i(TAG, "[DEBUG] Calling sendStatusToServer() from onCreate");
        sendStatusToServer();

        // Immediately attempt registration and status update
        Log.i(TAG, "[DEBUG] Attempting immediate registration and status update");
        sendStatusUpdateToServer();
        Log.i(TAG, "[DEBUG] Immediate registration and status update triggered");

        // Initialize dashcam API in background thread
        executorService.execute(() -> {
            initializeDashcamAPI();
        });

        // Start polling for commands every 10 seconds
        executorService.execute(() -> {
            while (true) {
                try {
                    pollForCommands();
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        // Send registration and status to server every 1 minute
        executorService.execute(() -> {
            while (true) {
                try {
                    sendStatusToServer();
                    sendStatusUpdateToServer();
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        Log.i(TAG, "[DEBUG] onCreate complete");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Fleet Management Service started");

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification());

        // Start location tracking service
        Intent locationIntent = new Intent(this, LocationTrackingService.class);
        startService(locationIntent);

        return START_STICKY; // Restart service if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Fleet Management Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Fleet management background service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Fleet Management Active")
                .setContentText("Monitoring vehicle and dashcam status")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void initializeDashcamAPI() {
        try {
            dashcamAPI = new API(this);
            dashcamAPI.setAutoSleepTime(0); // Disable auto sleep for fleet management
            dashcamAPI.registerCarMotionListener(this);

            // Enable collision detection
            dashcamAPI.enableCollision(true);
            dashcamAPI.setCollisionSensitivity(API.CollisionSensitivityNormal);

            // Set video parameters for optimal recording
            dashcamAPI.setVideoParams(API.CameraFront, 1920, 1080, 4000, 30);
            dashcamAPI.setVideoParams(API.CameraBack, 1920, 1080, 4000, 30);

            // Enable audio recording
            dashcamAPI.setDVRAudioEnable(API.CameraFront, true);
            dashcamAPI.setDVRAudioEnable(API.CameraBack, true);

            isInitialized = true;
            dashcamStatus.setStatus("Dashcam initialized");
            Log.i(TAG, "Dashcam API initialized successfully");

            // Send status to server
            sendStatusToServer();

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize dashcam API", e);
            dashcamStatus.setStatus("Dashcam initialization failed");
        }
    }

    @Override
    public void onViolentEvent(int value) {
        // Handle car motion events (acceleration, deceleration, turns)
        String eventType = "";
        switch (value) {
            case 1: // VIOLENT_SPEED_UP
                eventType = "Rapid acceleration detected";
                break;
            case 2: // VIOLENT_SPEED_DOWN
                eventType = "Hard braking detected";
                break;
            case 3: // VIOLENT_TURN_LEFT
                eventType = "Sharp left turn detected";
                break;
            case 4: // VIOLENT_TURN_RIGHT
                eventType = "Sharp right turn detected";
                break;
        }

        Log.w(TAG, "Violent event detected: " + eventType);
        dashcamStatus.setStatus(eventType);

        // Record event video
        recordEventVideo(eventType);

        // Send event to server
        sendEventToServer(value, eventType);
    }

    private void recordEventVideo(String eventType) {
        try {
            // Record 10 seconds before and after the event
            dashcamAPI.takeVideo(API.CameraBoth, 10, 10, new API.TakeCallback() {
                @Override
                public void onTakeProgress(int progressPercentage) {
                    Log.d(TAG, "Recording event video: " + progressPercentage + "%");
                }

                @Override
                public void onTakeResult(String jsonString) {
                    Log.i(TAG, "Event video recorded: " + jsonString);
                    // Upload video to server
                    uploadVideoToServer(jsonString, eventType);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to record event video", e);
        }
    }

    private void sendStatusToServer() {
        Log.i(TAG, "[DEBUG] sendStatusToServer() called");
        executorService.execute(() -> {
            try {
                String url = ServerConfig.getUrl("/api/dashcams/register");
                String json = "{\"deviceId\":\"" + dashcamStatus.getDeviceId()
                        + "\",\"model\":\"Dashcam\",\"version\":\"1.0\"}";
                Log.i(TAG, "[DEBUG] Attempting to send registration to server - URL: " + url);
                Log.i(TAG, "[DEBUG] Registration JSON: " + json);
                int response = postJson(url, json);
                Log.i(TAG, "[DEBUG] Registration response code: " + response);
            } catch (Exception e) {
                Log.e(TAG, "[DEBUG] Exception in sendStatusToServer", e);
            }
        });
    }

    private void sendStatusUpdateToServer() {
        executorService.execute(() -> {
            try {
                String url = ServerConfig.getUrl("/api/dashcams/" + dashcamStatus.getDeviceId() + "/status");
                String json = "{\"status\":\"online\",\"batteryLevel\":" + dashcamStatus.getBatteryLevel()
                        + ",\"storageAvailable\":" + dashcamStatus.getStorageAvailable() + "}";
                Log.i(TAG, "[DEBUG] Attempting to send status update to server - URL: " + url);
                Log.i(TAG, "[DEBUG] Status JSON: " + json);
                int response = postJson(url, json);
                Log.i(TAG, "[DEBUG] Status update response code: " + response);
            } catch (Exception e) {
                Log.e(TAG, "[DEBUG] Failed to send status update to server", e);
            }
        });
    }

    private void sendEventToServer(int eventType, String eventDescription) {
        executorService.execute(() -> {
            try {
                String url = ServerConfig.getUrl("/api/dashcams/" + dashcamStatus.getDeviceId() + "/events");
                String json = "{\"eventType\":\"" + eventType + "\",\"description\":\"" + eventDescription + "\"}";
                int response = postJson(url, json);
                Log.i(TAG, "Sent event to server, response: " + response);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send event to server", e);
            }
        });
    }

    private void uploadVideoToServer(String videoData, String eventType) {
        executorService.execute(() -> {
            try {
                String url = ServerConfig.getUrl("/api/dashcams/" + dashcamStatus.getDeviceId() + "/media");
                String json = "{\"eventType\":\"" + eventType + "\",\"videoData\":\"" + videoData + "\"}";
                int response = postJson(url, json);
                Log.i(TAG, "Uploaded video to server, response: " + response);
            } catch (Exception e) {
                Log.e(TAG, "Failed to upload video to server", e);
            }
        });
    }

    private void uploadPhotoToServer(String photoData) {
        executorService.execute(() -> {
            try {
                // Parse the photo data JSON
                org.json.JSONObject photoJson = new org.json.JSONObject(photoData);
                String frontImagePath = photoJson.optString("imgurl", "");
                String rearImagePath = photoJson.optString("imgurlrear", "");

                // Create media info for server
                String mediaJson = "{\"type\":\"image\",\"frontImage\":\"" + frontImagePath + "\",\"rearImage\":\""
                        + rearImagePath + "\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}";

                String url = ServerConfig.getUrl("/api/dashcams/" + dashcamStatus.getDeviceId() + "/media");
                int response = postJson(url, mediaJson);
                Log.i(TAG, "Uploaded photo to server, response: " + response);
            } catch (Exception e) {
                Log.e(TAG, "Failed to upload photo to server", e);
            }
        });
    }

    // Helper method for HTTP POST
    private int postJson(String urlString, String jsonBody) throws Exception {
        java.net.URL url = new java.net.URL(urlString);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "FleetManagementApp/1.0");
        conn.setConnectTimeout(10000); // 10 seconds
        conn.setReadTimeout(10000); // 10 seconds
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes("utf-8");
            os.write(input, 0, input.length);
            os.flush();
        }

        int code = conn.getResponseCode();
        Log.i(TAG, "HTTP Response Code: " + code);

        // Read response body for debugging
        try {
            java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (is != null) {
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String responseBody = s.hasNext() ? s.next() : "";
                Log.i(TAG, "HTTP Response Body: " + responseBody);
                is.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read response body", e);
        }

        conn.disconnect();
        return code;
    }

    // Public methods for external control
    public void takePhoto() {
        try {
            dashcamAPI.takePicture(API.CameraBoth, new API.TakeCallback() {
                @Override
                public void onTakeProgress(int progressPercentage) {
                    Log.d(TAG, "Taking photo: " + progressPercentage + "%");
                }

                @Override
                public void onTakeResult(String jsonString) {
                    Log.i(TAG, "Photo captured: " + jsonString);
                    uploadPhotoToServer(jsonString);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to take photo", e);
        }
    }

    public void startLiveStream() {
        try {
            if (dashcamAPI != null) {
                // Start RTMP streaming to server
                String rtmpUrl = ServerConfig.BASE_URL.replace("https://", "rtmp://").replace("http://", "rtmp://")
                        + "/live/"
                        + dashcamStatus.getDeviceId();
                dashcamAPI.rtmpLive(rtmpUrl, API.CameraBoth);
                dashcamStatus.setLiveStreaming(true);
                dashcamStatus.setStatus("Live streaming");
                Log.i(TAG, "Started live stream to: " + rtmpUrl);
                sendStatusToServer();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start live stream", e);
        }
    }

    public void sendEmergencyAlert() {
        try {
            // Take immediate photo and video
            takePhoto();
            recordEventVideo("Emergency Alert");

            // Play emergency sound
            dashcamAPI.playTts("Emergency alert activated", API.TYPE_REMINDER);

            Log.w(TAG, "Emergency alert sent");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send emergency alert", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Fleet Management Service destroyed");

        if (dashcamAPI != null) {
            dashcamAPI.unregisterCarMotionListener(this);
        }

        // Stop location tracking service
        Intent locationIntent = new Intent(this, LocationTrackingService.class);
        stopService(locationIntent);

        executorService.shutdown();
    }

    private void pollForCommands() {
        try {
            String url = ServerConfig.getUrl("/api/dashcams/" + dashcamStatus.getDeviceId() + "/commands");
            java.net.URL commandUrl = new java.net.URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) commandUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code == 200) {
                java.io.InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                is.close();
                handleCommandResponse(response);
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Failed to poll for commands", e);
        }
    }

    private void handleCommandResponse(String response) {
        // Expecting a JSON array of commands
        try {
            org.json.JSONArray commands = new org.json.JSONArray(response);
            for (int i = 0; i < commands.length(); i++) {
                org.json.JSONObject cmd = commands.getJSONObject(i);
                String command = cmd.optString("command");
                Log.i(TAG, "Received command: " + command);
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
                    // Add more supported commands here
                    default:
                        Log.w(TAG, "Unknown command: " + command);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse command response", e);
        }
    }

    public void stopLiveStream() {
        try {
            if (dashcamAPI != null) {
                dashcamAPI.rtmpLive(null, API.CameraBoth); // Stop live stream for both cameras
                dashcamStatus.setLiveStreaming(false);
                dashcamStatus.setStatus("Live stream stopped");
                Log.i(TAG, "Live stream stopped");
                sendStatusToServer();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop live stream", e);
        }
    }
}