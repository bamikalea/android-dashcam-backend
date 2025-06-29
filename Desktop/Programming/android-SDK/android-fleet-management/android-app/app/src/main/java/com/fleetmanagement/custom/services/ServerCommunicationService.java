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
import android.os.Environment;
import android.os.Handler;

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
import java.net.URI;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

import com.fleetmanagement.custom.services.HardwareControlService;
import com.fleetmanagement.custom.services.LocationService;
import com.fleetmanagement.custom.services.AccMonitoringService;
import carassist.cn.API;

import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;

/**
 * Enhanced Server Communication Service with JT808 Integration
 * Handles all communication with the fleet management server over the internet
 */
public class ServerCommunicationService extends Service {

    private static final String TAG = "ServerCommunicationService";
    private static final String API_BASE_URL = ServerConfig.BASE_URL;
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
    private static String deviceId;
    private boolean isConnected = false;
    private PowerManager.WakeLock wakeLock;
    private AlarmManager alarmManager;
    private PendingIntent restartIntent;
    private ScheduledExecutorService pollingExecutor;
    private boolean isRegistered = false;

    // Hardware control service
    private HardwareControlService hardwareControlService;

    // Location service
    private LocationService locationService;

    // JT808 integration
    private boolean jt808Enabled = false;
    private Location lastKnownLocation;
    private String lastKnownStatus = "unknown";

    // Audio communication
    private AudioManager audioManager;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private AtomicBoolean isPlaying = new AtomicBoolean(false);
    private AtomicReference<String> currentAudioFile = new AtomicReference<>();
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    // Camera capture result receiver
    private BroadcastReceiver cameraCaptureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("CAMERA_CAPTURE_RESULT".equals(intent.getAction())) {
                boolean success = intent.getBooleanExtra("success", false);
                String photoPath = intent.getStringExtra("photo_path");
                String error = intent.getStringExtra("error");
                String commandId = intent.getStringExtra("command_id");

                handleCameraCaptureResult(success, photoPath, error, commandId);
            }
        }
    };

    // Required zero-argument constructor for Android services
    public ServerCommunicationService() {
        this.gson = new Gson();
        // Do not initialize deviceId here

        // Create a custom proxy selector that always returns NO_PROXY
        java.net.ProxySelector proxySelector = new java.net.ProxySelector() {
            @Override
            public List<java.net.Proxy> select(URI uri) {
                return Arrays.asList(java.net.Proxy.NO_PROXY);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                // Ignore connection failures
            }
        };

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .proxySelector(proxySelector)
                .build();
        this.executorService = Executors.newFixedThreadPool(3);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "ServerCommunicationService created");

        // Initialize device ID
        deviceId = getInitialDeviceId();

        // Clear APN proxy settings to ensure direct connections
        clearApnProxySettings();

        // Start foreground service
        startForegroundServiceWithNotification();

        // Initialize hardware control service
        initializeHardwareControl();

        // Initialize location service
        initializeLocationService();

        // Initialize audio components
        initializeAudio();

        // Register camera capture result receiver
        IntentFilter filter = new IntentFilter("CAMERA_CAPTURE_RESULT");
        registerReceiver(cameraCaptureReceiver, filter);

        // Set up restart alarm
        setupRestartAlarm();

        // Acquire wake lock
        acquireWakeLock();

        Log.i(TAG, "ServerCommunicationService initialization completed");
    }

    private String getInitialDeviceId() {
        String id = null;
        try {
            id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            Log.e(TAG, "Error getting ANDROID_ID", e);
        }
        if (id == null || id.isEmpty()) {
            id = "dashcam-" + System.currentTimeMillis();
        }
        Log.i(TAG, "Device ID initialized: " + id);
        return id;
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

        // Unregister camera capture result receiver
        try {
            unregisterReceiver(cameraCaptureReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering camera capture receiver", e);
        }

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

            // Start polling for commands
            startPollingForCommands();

            // Register with server
            registerWithServer();

            // Start connection monitoring
            startConnectionMonitoring();

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
            Log.e(TAG, "Error initializing connection", e);
        }
    }

    private void startConnectionMonitoring() {
        // Monitor connection status and auto-reconnect if needed
        pollingExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        checkConnectionStatus();
                    }
                });
            }
        }, 60, 60, TimeUnit.SECONDS); // Check every minute
    }

    private void checkConnectionStatus() {
        try {
            Log.d(TAG, "=== CHECKING CONNECTION STATUS ===");

            // Check if we're still registered with the server
            String url = ServerConfig.getDeviceStatusUrl(deviceId);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "FleetManagement-Android/1.0")
                    .addHeader("Device-ID", deviceId)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Connection status check successful");
                    isRegistered = true;
                } else {
                    Log.w(TAG, "Connection status check failed: " + response.code());
                    isRegistered = false;

                    // Try to re-register
                    Log.i(TAG, "Attempting to re-register with server...");
                    registerWithServer();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking connection status", e);
            isRegistered = false;

            // Try to re-register
            Log.i(TAG, "Attempting to re-register with server after error...");
            registerWithServer();
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
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        pollForCommands();
                    }
                });
            }
        }, 0, POLLING_INTERVAL, TimeUnit.MILLISECONDS);

        // Send status updates every 60 seconds to keep device active
        pollingExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        sendStatus(null);
                    }
                });
            }
        }, 60, 60, TimeUnit.SECONDS);

        Log.i(TAG, "Started polling for commands every " + POLLING_INTERVAL + "ms and status updates every 60s");
    }

    private void pollForCommands() {
        try {
            Log.d(TAG, "=== POLLING FOR COMMANDS ===");
            Log.d(TAG, "Device ID: " + deviceId);
            Log.d(TAG, "Server URL: " + API_BASE_URL);

            String url = ServerConfig.getCommandsUrl(deviceId);

            Log.d(TAG, "Polling URL: " + url);

            // Validate URL to prevent routing issues
            if (!url.startsWith("https://e-android-fleet-backend-render.onrender.com")) {
                Log.e(TAG, "Invalid URL detected: " + url);
                return;
            }

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "FleetManagement-Android/1.0")
                    .addHeader("Device-ID", deviceId)
                    .addHeader("Accept", "application/json")
                    .get()
                    .build();

            Log.d(TAG, "Sending GET request to: " + url);

            try (Response response = client.newCall(request).execute()) {
                Log.d(TAG, "=== COMMAND POLLING RESPONSE ===");
                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Headers: " + response.headers());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response Body: " + responseBody);

                    if (responseBody != null && !responseBody.trim().isEmpty()) {
                        Log.i(TAG, "=== COMMANDS RECEIVED ===");
                        Log.i(TAG, "Commands received from server: " + responseBody);
                        executeCommands(responseBody);
                    } else {
                        Log.d(TAG, "No commands received from server (empty response)");
                    }
                } else {
                    Log.w(TAG, "=== COMMAND POLLING ERROR ===");
                    Log.w(TAG, "Server returned error code: " + response.code());
                    String errorBody = response.body().string();
                    Log.w(TAG, "Error response: " + errorBody);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "=== COMMAND POLLING EXCEPTION ===");
            Log.e(TAG, "Error polling for commands", e);
            Log.e(TAG, "Exception details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeCommands(String commandsJson) {
        try {
            Log.i(TAG, "=== EXECUTING COMMANDS ===");
            Log.i(TAG, "Commands JSON: " + commandsJson);

            // Try to handle both {"commands": [...]} and raw array [...]
            JSONArray commandsArray = null;
            if (commandsJson.trim().startsWith("{")) {
                Log.d(TAG, "Parsing JSON object format");
                JSONObject obj = new JSONObject(commandsJson);
                if (obj.has("commands")) {
                    commandsArray = obj.getJSONArray("commands");
                    Log.d(TAG, "Found commands array with " + commandsArray.length() + " commands");
                } else {
                    Log.w(TAG, "JSON object does not contain 'commands' field");
                }
            } else if (commandsJson.trim().startsWith("[")) {
                Log.d(TAG, "Parsing JSON array format");
                commandsArray = new JSONArray(commandsJson);
                Log.d(TAG, "Found direct array with " + commandsArray.length() + " commands");
            } else {
                Log.w(TAG, "Unknown JSON format: " + commandsJson);
            }

            if (commandsArray != null && commandsArray.length() > 0) {
                Log.i(TAG, "=== PROCESSING " + commandsArray.length() + " COMMANDS ===");
                for (int i = 0; i < commandsArray.length(); i++) {
                    JSONObject commandObj = commandsArray.getJSONObject(i);
                    String command = commandObj.optString("command");
                    String commandId = commandObj.has("id") ? commandObj.getString("id")
                            : commandObj.optString("commandId", null);

                    Log.i(TAG, "=== EXECUTING COMMAND " + (i + 1) + " ===");
                    Log.i(TAG, "Command: " + command + " (ID: " + commandId + ")");
                    Log.i(TAG, "Command object: " + commandObj.toString());

                    switch (command) {
                        case "takePhoto":
                        case "capture_photo":
                            Log.i(TAG, "Executing capture_photo command");
                            capturePhoto(commandId);
                            break;
                        case "startLiveStream":
                        case "start_live_stream":
                            Log.i(TAG, "Executing start_live_stream command");
                            startLiveStream();
                            break;
                        case "stopLiveStream":
                        case "stop_live_stream":
                            Log.i(TAG, "Executing stop_live_stream command");
                            stopLiveStream();
                            break;
                        case "startRecording":
                        case "start_video_recording":
                            Log.i(TAG, "Executing start_video_recording command");
                            startRecording(commandId);
                            break;
                        case "stopRecording":
                        case "stop_video_recording":
                            Log.i(TAG, "Executing stop_video_recording command");
                            stopRecording();
                            break;
                        case "getStatus":
                        case "get_device_info":
                            Log.i(TAG, "Executing getStatus command");
                            sendStatus(commandId);
                            break;
                        case "getLocation":
                            Log.i(TAG, "Executing getLocation command");
                            getLocation();
                            break;
                        case "getHeartbeat":
                            Log.i(TAG, "Executing getHeartbeat command");
                            sendHeartbeat();
                            break;
                        case "getAccStatus":
                            Log.i(TAG, "Executing getAccStatus command");
                            getAccStatus();
                            break;
                        case "startAccMonitoring":
                            Log.i(TAG, "Executing startAccMonitoring command");
                            startAccMonitoring();
                            break;
                        case "stopAccMonitoring":
                            Log.i(TAG, "Executing stopAccMonitoring command");
                            stopAccMonitoring();
                            break;
                        case "startAudioRecording":
                        case "start_audio_recording":
                            Log.i(TAG, "Executing start_audio_recording command");
                            startAudioRecording();
                            break;
                        case "stopAudioRecording":
                        case "stop_audio_recording":
                            Log.i(TAG, "Executing stop_audio_recording command");
                            stopAudioRecording();
                            break;
                        case "playAudio":
                            Log.i(TAG, "Executing playAudio command");
                            if (commandObj.has("audioData")) {
                                playAudio(commandObj.getString("audioData"));
                            } else {
                                sendCommandResponse(command, false, "No audio data provided", commandId);
                            }
                            break;
                        case "playTTS":
                        case "tts_speak":
                            Log.i(TAG, "Executing TTS command");
                            if (commandObj.has("parameters")) {
                                JSONObject parameters = commandObj.getJSONObject("parameters");
                                if (parameters.has("message")) {
                                    playTTS(parameters.getString("message"), commandId);
                                } else if (parameters.has("text")) {
                                    playTTS(parameters.getString("text"), commandId);
                                } else {
                                    sendCommandResponse(command, false, "No text provided for TTS", commandId);
                                }
                            } else if (commandObj.has("text")) {
                                playTTS(commandObj.getString("text"), commandId);
                            } else {
                                sendCommandResponse(command, false, "No text provided for TTS", commandId);
                            }
                            break;
                        case "startTwoWayAudio":
                        case "start_two_way_audio":
                            Log.i(TAG, "Executing start_two_way_audio command");
                            startTwoWayAudio();
                            break;
                        case "stopTwoWayAudio":
                        case "stop_two_way_audio":
                            Log.i(TAG, "Executing stop_two_way_audio command");
                            stopTwoWayAudio();
                            break;
                        case "restart_app":
                            Log.i(TAG, "Executing restart_app command");
                            // Implement app restart logic if needed
                            break;
                        case "clear_apn_proxy":
                            Log.i(TAG, "Executing clear_apn_proxy command");
                            clearApnProxySettings();
                            break;
                        case "test_emergency_alert":
                        case "test_overspeed_alert":
                        case "test_fatigue_alert":
                            Log.i(TAG, "Executing alert test command: " + command);
                            // Implement alert test logic if needed
                            break;
                        default:
                            Log.w(TAG, "Unknown command: " + command);
                            sendCommandResponse(command, false, "Unknown command: " + command, commandId);
                            break;
                    }
                }
            } else {
                Log.d(TAG, "No commands to execute (empty array or null)");
                // Fallback for simple string matching
                if (commandsJson.contains("takePhoto")) {
                    Log.i(TAG, "Fallback: executing takePhoto");
                    capturePhoto(null);
                }
                if (commandsJson.contains("startLiveStream")) {
                    Log.i(TAG, "Fallback: executing startLiveStream");
                    startLiveStream();
                }
                if (commandsJson.contains("stopLiveStream")) {
                    Log.i(TAG, "Fallback: executing stopLiveStream");
                    stopLiveStream();
                }
                if (commandsJson.contains("startRecording")) {
                    Log.i(TAG, "Fallback: executing startRecording");
                    startRecording(null);
                }
                if (commandsJson.contains("stopRecording")) {
                    Log.i(TAG, "Fallback: executing stopRecording");
                    stopRecording();
                }
                if (commandsJson.contains("getStatus")) {
                    Log.i(TAG, "Fallback: executing getStatus");
                    sendStatus(null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "=== COMMAND EXECUTION ERROR ===");
            Log.e(TAG, "Error executing commands: " + e.getMessage(), e);
        }
    }

    private void registerWithServer() {
        Log.d(TAG, "=== REGISTERING WITH SERVER ===");
        Log.d(TAG, "Device ID: " + deviceId);
        Log.d(TAG, "Server URL: " + API_BASE_URL);

        // Run registration on background thread to avoid NetworkOnMainThreadException
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = ServerConfig.getRegisterUrl();

                    Log.d(TAG, "Registration URL: " + url);

                    // Create registration payload
                    JSONObject registrationData = new JSONObject();
                    registrationData.put("deviceId", deviceId);
                    registrationData.put("model", "Android Dashcam");
                    registrationData.put("version", "1.0");
                    registrationData.put("timestamp", System.currentTimeMillis());

                    String jsonPayload = registrationData.toString();
                    Log.d(TAG, "Registration payload: " + jsonPayload);

                    RequestBody requestBody = RequestBody.create(JSON, jsonPayload);

                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("User-Agent", "FleetManagement-Android/1.0")
                            .addHeader("Device-ID", deviceId)
                            .post(requestBody)
                            .build();

                    Log.d(TAG, "Sending registration request");

                    try (Response response = client.newCall(request).execute()) {
                        Log.d(TAG, "Registration response received - Code: " + response.code());
                        Log.d(TAG, "Response Headers: " + response.headers());

                        if (response.isSuccessful()) {
                            String responseBody = response.body().string();
                            Log.i(TAG, "=== REGISTRATION SUCCESS ===");
                            Log.i(TAG, "Response: " + responseBody);
                            isRegistered = true;

                            // Send initial status after successful registration
                            sendStatus(null);

                        } else {
                            String errorBody = response.body().string();
                            Log.w(TAG, "=== REGISTRATION FAILED ===");
                            Log.w(TAG, "Error code: " + response.code());
                            Log.w(TAG, "Error response: " + errorBody);
                            isRegistered = false;

                            // Schedule retry
                            scheduleRegistrationRetry();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "=== REGISTRATION ERROR ===");
                    Log.e(TAG, "Error registering with server", e);
                    Log.e(TAG, "Exception details: " + e.getMessage());
                    e.printStackTrace();
                    isRegistered = false;

                    // Schedule retry
                    scheduleRegistrationRetry();
                }
            }
        });
    }

    private void scheduleRegistrationRetry() {
        // Retry registration after 30 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isRegistered) {
                    Log.i(TAG, "Retrying registration...");
                    registerWithServer();
                }
            }
        }, 30000);
    }

    private void sendStatus(String commandId) {
        try {
            Log.d(TAG, "=== SENDING STATUS ===");
            Log.d(TAG, "Device ID: " + deviceId);
            Log.d(TAG, "Server URL: " + API_BASE_URL);

            String url = ServerConfig.getDeviceStatusUrl(deviceId);

            Log.d(TAG, "Status URL: " + url);

            // Validate URL to prevent routing issues
            if (!url.startsWith("https://e-android-fleet-backend-render.onrender.com")) {
                Log.e(TAG, "Invalid URL detected: " + url);
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            JSONObject statusData = new JSONObject();
            statusData.put("status", "online");
            statusData.put("batteryLevel", getBatteryLevel());
            statusData.put("storageAvailable", getAvailableStorage());
            statusData.put("timestamp", sdf.format(new java.util.Date()));

            String jsonPayload = statusData.toString();
            Log.d(TAG, "Status payload: " + jsonPayload);

            RequestBody requestBody = RequestBody.create(JSON, jsonPayload);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "FleetManagement-Android/1.0")
                    .addHeader("Device-ID", deviceId)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Sending status request");

            try (Response response = client.newCall(request).execute()) {
                Log.d(TAG, "Status response received - Code: " + response.code());
                Log.d(TAG, "Response Headers: " + response.headers());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.i(TAG, "=== STATUS SENT SUCCESS ===");
                    Log.i(TAG, "Response: " + responseBody);

                    // If this was called as a command, send a command response
                    if (commandId != null) {
                        sendCommandResponse("getStatus", true, "Status sent successfully", commandId);
                    }
                } else {
                    String errorBody = response.body().string();
                    Log.w(TAG, "=== STATUS SENT FAILED ===");
                    Log.w(TAG, "Error code: " + response.code());
                    Log.w(TAG, "Error response: " + errorBody);

                    // If this was called as a command, send a command response
                    if (commandId != null) {
                        sendCommandResponse("getStatus", false, "Status send failed: " + response.code(), commandId);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "=== STATUS SENT ERROR ===");
            Log.e(TAG, "Error sending status", e);
            Log.e(TAG, "Exception details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Play a beep sound to indicate photo capture
     */
    private void playCaptureBeep() {
        try {
            android.media.ToneGenerator toneGen = new android.media.ToneGenerator(
                    android.media.AudioManager.STREAM_SYSTEM, 100);
            toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 200);
            toneGen.release();
            Log.i(TAG, "Played capture beep sound");
        } catch (Exception e) {
            Log.w(TAG, "Could not play capture beep: " + e.getMessage());
        }
    }

    /**
     * Capture photo using Car SDK with fallback to Android Camera API
     */
    private void capturePhoto(String commandId) {
        Log.i(TAG, "=== STARTING PHOTO CAPTURE ===");

        // Play capture beep
        playCaptureBeep();

        // Try Car SDK first
        if (hardwareControlService != null) {
            try {
                Log.i(TAG, "Attempting photo capture with Car SDK...");
                hardwareControlService.takePhoto(carassist.cn.API.CameraBoth,
                        new com.fleetmanagement.custom.services.HardwareControlService.PhotoCallback() {
                            @Override
                            public void onProgress(int progress) {
                                Log.i(TAG, "Photo capture progress: " + progress + "%");
                            }

                            @Override
                            public void onSuccess(String result) {
                                Log.i(TAG, "=== PHOTO CAPTURE SUCCESS (Car SDK) ===");
                                Log.i(TAG, "Photo capture result: " + result);

                                try {
                                    JSONObject jsonResult = new JSONObject(result);
                                    if (jsonResult.has("imgurl")) {
                                        String photoPath = jsonResult.getString("imgurl");
                                        Log.i(TAG, "Photo saved to: " + photoPath);

                                        File photoFile = new File(photoPath);
                                        if (photoFile.exists()) {
                                            Log.i(TAG, "Photo file exists, uploading to server...");
                                            uploadPhoto(photoFile, commandId != null ? commandId : "remote_command");
                                            sendCommandResponse("takePhoto", true,
                                                    "Photo captured and uploaded successfully", commandId);
                                        } else {
                                            Log.e(TAG, "Photo file does not exist at path: " + photoPath);
                                            sendCommandResponse("takePhoto", false,
                                                    "Photo captured but file not found: " + photoPath, commandId);
                                        }
                                    } else {
                                        Log.w(TAG, "No imgurl in photo result: " + result);
                                        sendCommandResponse("takePhoto", false,
                                                "Photo captured but no file path returned", commandId);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Error parsing photo result JSON", e);
                                    sendCommandResponse("takePhoto", false,
                                            "Error parsing photo result: " + e.getMessage(), commandId);
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Car SDK photo capture failed: " + error);
                                // Fallback to Android Camera API
                                capturePhotoWithAndroidCamera(commandId);
                            }
                        });

                return;
            } catch (Exception e) {
                Log.e(TAG, "Car SDK photo capture failed: " + e.getMessage(), e);
            }
        } else {
            Log.w(TAG, "Hardware control service not available");
        }

        // Fallback to Android Camera API
        Log.i(TAG, "Falling back to Android Camera API...");
        capturePhotoWithAndroidCamera(commandId);
    }

    /**
     * Fallback photo capture using Android Camera API
     */
    private void capturePhotoWithAndroidCamera(String commandId) {
        Log.w(TAG, "=== FALLBACK PHOTO CAPTURE (Android Camera) ===");

        try {
            // Create a temporary file for the photo
            File photoDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "dashcam");
            if (!photoDir.exists()) {
                photoDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File photoFile = new File(photoDir, "photo_" + timestamp + ".jpg");

            // Use Camera2 API for photo capture
            capturePhotoWithCamera2(photoFile, commandId);

        } catch (Exception e) {
            Log.e(TAG, "Error in camera fallback: " + e.getMessage(), e);
            sendCommandResponse("takePhoto", false,
                    "Camera fallback failed: " + e.getMessage(), commandId);
        }
    }

    /**
     * Capture photo using Camera2 API
     */
    private void capturePhotoWithCamera2(File photoFile, String commandId) {
        try {
            // Create a temporary activity context for camera operations
            Intent cameraIntent = new Intent(this, CameraCaptureActivity.class);
            cameraIntent.putExtra("photo_path", photoFile.getAbsolutePath());
            cameraIntent.putExtra("command_id", commandId);
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(cameraIntent);

            Log.i(TAG, "Started Camera2 capture activity for: " + photoFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Failed to start camera capture: " + e.getMessage(), e);
            sendCommandResponse("takePhoto", false,
                    "Failed to start camera capture: " + e.getMessage(), commandId);
        }
    }

    private void startRecording(String commandId) {
        Log.i(TAG, "=== STARTING VIDEO RECORDING ===");

        try {
            // Use HardwareControlService for better integration
            if (hardwareControlService != null) {
                Log.i(TAG, "Using HardwareControlService for recording...");
                hardwareControlService.startRecording(carassist.cn.API.CameraBoth, 5, 10,
                        new com.fleetmanagement.custom.services.HardwareControlService.VideoCallback() {
                            @Override
                            public void onProgress(int progress) {
                                Log.i(TAG, "Video recording progress: " + progress + "%");
                            }

                            @Override
                            public void onSuccess(String result) {
                                Log.i(TAG, "=== VIDEO RECORDING SUCCESS ===");
                                Log.i(TAG, "Video recording result: " + result);

                                try {
                                    JSONObject jsonResult = new JSONObject(result);
                                    if (jsonResult.has("videourl")) {
                                        String videoPath = jsonResult.getString("videourl");
                                        Log.i(TAG, "Video saved to: " + videoPath);

                                        File videoFile = new File(videoPath);
                                        if (videoFile.exists()) {
                                            Log.i(TAG, "Video file exists, uploading to server...");
                                            uploadVideo(videoFile, "remote_recording");
                                            sendCommandResponse("startRecording", true,
                                                    "Video recorded and uploaded successfully", commandId);
                                        } else {
                                            Log.e(TAG, "Video file does not exist at path: " + videoPath);
                                            sendCommandResponse("startRecording", false,
                                                    "Video recorded but file not found: " + videoPath, commandId);
                                        }
                                    } else {
                                        Log.w(TAG, "No videourl in recording result: " + result);
                                        sendCommandResponse("startRecording", false,
                                                "Video recorded but no file path returned", commandId);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "Error parsing recording result JSON", e);
                                    sendCommandResponse("startRecording", false,
                                            "Error parsing recording result: " + e.getMessage(), commandId);
                                }
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "HardwareControlService recording failed: " + error);
                                // Fallback to direct Car SDK
                                startRecordingWithCarSDK(commandId);
                            }
                        });
            } else {
                Log.w(TAG, "HardwareControlService not available, using direct Car SDK...");
                startRecordingWithCarSDK(commandId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Video recording failed: " + e.getMessage(), e);
            sendCommandResponse("startRecording", false, "Recording failed: " + e.getMessage(), commandId);
        }
    }

    private void startRecordingWithCarSDK(String commandId) {
        try {
            Log.i(TAG, "Using Car SDK directly for recording...");
            carassist.cn.API carApi = new carassist.cn.API(this);

            // Start recording with 5 seconds forward and 10 seconds after
            carApi.takeVideo(carassist.cn.API.CameraBoth, 5, 10, new carassist.cn.API.TakeCallback() {
                @Override
                public void onTakeProgress(int progress) {
                    Log.i(TAG, "Video recording progress: " + progress + "%");
                }

                @Override
                public void onTakeResult(String jsonString) {
                    Log.i(TAG, "=== VIDEO RECORDING SUCCESS (Car SDK) ===");
                    Log.i(TAG, "Video recording result: " + jsonString);

                    try {
                        JSONObject jsonResult = new JSONObject(jsonString);
                        if (jsonResult.has("videourl")) {
                            String videoPath = jsonResult.getString("videourl");
                            Log.i(TAG, "Video saved to: " + videoPath);

                            File videoFile = new File(videoPath);
                            if (videoFile.exists()) {
                                Log.i(TAG, "Video file exists, uploading to server...");
                                uploadVideo(videoFile, "remote_recording");
                                sendCommandResponse("startRecording", true, "Video recorded and uploaded successfully",
                                        commandId);
                            } else {
                                Log.e(TAG, "Video file does not exist at path: " + videoPath);
                                sendCommandResponse("startRecording", false,
                                        "Video recorded but file not found: " + videoPath, commandId);
                            }
                        } else {
                            Log.w(TAG, "No videourl in recording result: " + jsonString);
                            sendCommandResponse("startRecording", false, "Video recorded but no file path returned",
                                    commandId);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing recording result JSON", e);
                        sendCommandResponse("startRecording", false,
                                "Error parsing recording result: " + e.getMessage(), commandId);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Car SDK recording failed: " + e.getMessage(), e);
            sendCommandResponse("startRecording", false, "Recording failed: " + e.getMessage(), commandId);
        }
    }

    private void stopRecording() {
        Log.i(TAG, "=== STOPPING VIDEO RECORDING ===");
        // Note: The Car SDK doesn't have a direct stop recording method
        // Recording stops automatically after the specified duration
        // We can check for recently created video files and upload them
        uploadRecentVideos();
        sendCommandResponse("stopRecording", true, "Recording stopped", null);
    }

    private void startLiveStream() {
        Log.i(TAG, "=== STARTING LIVE STREAM ===");

        try {
            // For now, we'll implement a basic live stream using Car SDK
            // In a real implementation, you would use a streaming service like RTMP
            carassist.cn.API carApi = new carassist.cn.API(this);

            // Start a continuous video capture for streaming
            carApi.takeVideo(carassist.cn.API.CameraBoth, 0, 30, new carassist.cn.API.TakeCallback() {
                @Override
                public void onTakeProgress(int progress) {
                    Log.i(TAG, "Live stream progress: " + progress + "%");
                }

                @Override
                public void onTakeResult(String jsonString) {
                    Log.i(TAG, "=== LIVE STREAM SEGMENT COMPLETED ===");
                    Log.i(TAG, "Live stream segment: " + jsonString);

                    // Upload the live stream segment
                    try {
                        JSONObject jsonResult = new JSONObject(jsonString);
                        if (jsonResult.has("videourl")) {
                            String videoPath = jsonResult.getString("videourl");
                            File videoFile = new File(videoPath);
                            if (videoFile.exists()) {
                                uploadVideo(videoFile, "live_stream");
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing live stream result", e);
                    }
                }
            });

            sendCommandResponse("startLiveStream", true, "Live stream started", null);

        } catch (Exception e) {
            Log.e(TAG, "Live stream failed: " + e.getMessage(), e);
            sendCommandResponse("startLiveStream", false, "Live stream failed: " + e.getMessage(), null);
        }
    }

    private void stopLiveStream() {
        Log.i(TAG, "=== STOPPING LIVE STREAM ===");
        // Stop the live stream and upload any remaining segments
        uploadRecentVideos();
        sendCommandResponse("stopLiveStream", true, "Live stream stopped", null);
    }

    /**
     * Upload recent video files from SD card
     */
    private void uploadRecentVideos() {
        try {
            Log.i(TAG, "=== UPLOADING RECENT VIDEOS ===");

            // Check multiple possible video directories
            String[] videoDirs = {
                    "/sdcard/Pictures/",
                    "/storage/sdcard0/DVR/",
                    "/storage/sdcard0/DCIM/",
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/"
            };

            for (String dirPath : videoDirs) {
                File dir = new File(dirPath);
                if (dir.exists() && dir.isDirectory()) {
                    Log.i(TAG, "Checking directory: " + dirPath);

                    File[] videoFiles = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".mp4") ||
                            name.toLowerCase().endsWith(".avi") ||
                            name.toLowerCase().endsWith(".mov"));

                    if (videoFiles != null) {
                        for (File videoFile : videoFiles) {
                            // Check if file was created in the last 5 minutes
                            long fileAge = System.currentTimeMillis() - videoFile.lastModified();
                            if (fileAge < 5 * 60 * 1000) { // 5 minutes
                                Log.i(TAG, "Found recent video file: " + videoFile.getPath());
                                uploadVideo(videoFile, "auto_upload");
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error uploading recent videos: " + e.getMessage(), e);
        }
    }

    // Overload for backward compatibility
    private void sendCommandResponse(String command, boolean success, String message) {
        sendCommandResponse(command, success, message, null);
    }

    private void sendCommandResponse(String command, boolean success, String message, String commandId) {
        try {
            Log.d(TAG, "=== SENDING COMMAND RESPONSE ===");
            Log.d(TAG, "Device ID: " + deviceId);
            Log.d(TAG, "Command: " + command);
            Log.d(TAG, "Command ID: " + commandId);
            Log.d(TAG, "Success: " + success);
            Log.d(TAG, "Message: " + message);
            Log.d(TAG, "Server URL: " + API_BASE_URL);

            String url = ServerConfig.getResponseUrl(deviceId);

            Log.d(TAG, "Response URL: " + url);

            // Validate URL to prevent routing issues
            if (!url.startsWith("https://e-android-fleet-backend-render.onrender.com")) {
                Log.e(TAG, "Invalid URL detected: " + url);
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            JSONObject responseData = new JSONObject();
            responseData.put("command", command);
            responseData.put("commandId", commandId);
            responseData.put("success", success);
            responseData.put("message", message);
            responseData.put("timestamp", sdf.format(new java.util.Date()));

            String jsonPayload = responseData.toString();
            Log.d(TAG, "Response payload: " + jsonPayload);

            RequestBody requestBody = RequestBody.create(JSON, jsonPayload);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "FleetManagement-Android/1.0")
                    .addHeader("Device-ID", deviceId)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Sending command response request");

            try (Response response = client.newCall(request).execute()) {
                Log.d(TAG, "Command response received - Code: " + response.code());
                Log.d(TAG, "Response Headers: " + response.headers());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.i(TAG, "=== COMMAND RESPONSE SUCCESS ===");
                    Log.i(TAG, "Response: " + responseBody);
                } else {
                    String errorBody = response.body().string();
                    Log.w(TAG, "=== COMMAND RESPONSE FAILED ===");
                    Log.w(TAG, "Error code: " + response.code());
                    Log.w(TAG, "Error response: " + errorBody);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "=== COMMAND RESPONSE ERROR ===");
            Log.e(TAG, "Error sending command response", e);
            Log.e(TAG, "Exception details: " + e.getMessage());
            e.printStackTrace();
        }
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
            if (deviceId == null || deviceId.isEmpty()) {
                Log.e(TAG, "Device ID is null or empty! Location update will not be sent.");
                return;
            }

            Log.i(TAG, "[LOG] Executing request for: Location Update");

            // Use the correct endpoint for location updates
            String url = ServerConfig.getLocationUrl(deviceId);

            Log.i(TAG, "[LOG] Request URL: " + url);

            JsonObject locationData = new JsonObject();
            locationData.addProperty("latitude", location.getLatitude());
            locationData.addProperty("longitude", location.getLongitude());
            locationData.addProperty("altitude", location.getAltitude());
            locationData.addProperty("speed", location.getSpeed());
            locationData.addProperty("accuracy", location.getAccuracy());
            locationData.addProperty("timestamp", System.currentTimeMillis());

            String json = gson.toJson(locationData);
            Log.i(TAG, "[LOG] Request body: " + json);

            RequestBody body = RequestBody.create(JSON, json);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "FleetManagement-Android/1.0")
                    .addHeader("Device-ID", deviceId)
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

            Log.i(TAG, "[LOG] Sending event to server. URL: " + ServerConfig.getEventUrl());
            Log.i(TAG, "[LOG] Event payload: " + json);
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
        Log.d(TAG, "=== UPLOAD PHOTO STARTED ===");
        Log.d(TAG, "Photo file: " + photoFile.getAbsolutePath());
        Log.d(TAG, "Event type: " + eventType);
        Log.d(TAG, "File exists: " + photoFile.exists());
        Log.d(TAG, "File size: " + photoFile.length() + " bytes");

        if (!photoFile.exists()) {
            Log.e(TAG, "Photo file does not exist: " + photoFile.getAbsolutePath());
            return;
        }

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Starting photo upload in background thread");

                String url = ServerConfig.getPhotoUrl(deviceId);

                Log.d(TAG, "Upload URL: " + url);

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("photo", photoFile.getName(),
                                RequestBody.create(MEDIA_TYPE_JPEG, photoFile))
                        .addFormDataPart("eventType", eventType)
                        .addFormDataPart("deviceId", deviceId)
                        .addFormDataPart("timestamp", String.valueOf(System.currentTimeMillis()))
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "FleetManagement-Android/1.0")
                        .addHeader("Device-ID", deviceId)
                        .post(requestBody)
                        .build();

                Log.d(TAG, "Sending photo upload request");

                try (Response response = client.newCall(request).execute()) {
                    Log.d(TAG, "Photo upload response received - Code: " + response.code());
                    Log.d(TAG, "Response Headers: " + response.headers());

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.i(TAG, "Photo upload SUCCESS - Response: " + responseBody);
                        Log.i(TAG, "Photo uploaded successfully to server");
                    } else {
                        String errorBody = response.body().string();
                        Log.e(TAG, "Photo upload FAILED - Error code: " + response.code());
                        Log.e(TAG, "Error response: " + errorBody);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error uploading photo", e);
                Log.e(TAG, "Exception details: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Upload video to server
     */
    public void uploadVideo(File videoFile, String eventType) {
        Log.i(TAG, "=== UPLOAD VIDEO STARTED ===");
        Log.i(TAG, "Video file: " + videoFile.getAbsolutePath());
        Log.i(TAG, "Event type: " + eventType);
        Log.i(TAG, "File exists: " + videoFile.exists());
        Log.i(TAG, "File size: " + videoFile.length() + " bytes");

        if (!videoFile.exists()) {
            Log.e(TAG, "Video file does not exist: " + videoFile.getAbsolutePath());
            return;
        }

        executorService.execute(() -> {
            try {
                Log.i(TAG, "Starting video upload in background thread");

                String url = ServerConfig.getVideoUrl(deviceId);

                Log.i(TAG, "Upload URL: " + url);

                // Determine media type based on file extension
                MediaType mediaType = MEDIA_TYPE_MP4;
                String fileName = videoFile.getName().toLowerCase();
                if (fileName.endsWith(".avi")) {
                    mediaType = MediaType.parse("video/avi");
                } else if (fileName.endsWith(".mov")) {
                    mediaType = MediaType.parse("video/quicktime");
                }

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("video", videoFile.getName(),
                                RequestBody.create(mediaType, videoFile))
                        .addFormDataPart("eventType", eventType)
                        .addFormDataPart("deviceId", deviceId)
                        .addFormDataPart("timestamp", String.valueOf(System.currentTimeMillis()))
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "FleetManagement-Android/1.0")
                        .addHeader("Device-ID", deviceId)
                        .post(requestBody)
                        .build();

                Log.i(TAG, "Sending video upload request");

                try (Response response = client.newCall(request).execute()) {
                    Log.i(TAG, "Video upload response received - Code: " + response.code());
                    Log.i(TAG, "Response Headers: " + response.headers());

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.i(TAG, "=== VIDEO UPLOAD SUCCESS ===");
                        Log.i(TAG, "Response: " + responseBody);
                        Log.i(TAG, "Video uploaded successfully to server");
                    } else {
                        String errorBody = response.body().string();
                        Log.e(TAG, "=== VIDEO UPLOAD FAILED ===");
                        Log.e(TAG, "Error code: " + response.code());
                        Log.e(TAG, "Error response: " + errorBody);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error uploading video", e);
                Log.e(TAG, "Exception details: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Send heartbeat to server
     */
    public void sendHeartbeat() {
        try {
            JsonObject heartbeatData = new JsonObject();
            heartbeatData.addProperty("timestamp", System.currentTimeMillis());
            heartbeatData.addProperty("battery_level", getBatteryLevel());
            heartbeatData.addProperty("storage_available", getAvailableStorage());

            String json = gson.toJson(heartbeatData);
            RequestBody body = RequestBody.create(JSON, json);

            Request request = new Request.Builder()
                    .url(ServerConfig.getHeartbeatUrl(deviceId))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "FleetManagement-Android/1.0")
                    .addHeader("Device-ID", deviceId)
                    .post(body)
                    .build();

            Log.i(TAG, "[LOG] Sending heartbeat to server. URL: " + ServerConfig.getHeartbeatUrl(deviceId));
            Log.i(TAG, "[LOG] Heartbeat payload: " + json);
            executeRequest(request, "Heartbeat");

        } catch (Exception e) {
            Log.e(TAG, "Failed to send heartbeat", e);
        }
    }

    /**
     * Execute HTTP request with retry logic
     */
    private void executeRequest(Request request, String context) {
        executorService.execute(() -> {
            try {
                Log.i(TAG, "[LOG] Executing request for: " + context);
                Log.i(TAG, "[LOG] Request URL: " + request.url());
                Log.i(TAG, "[LOG] Request method: " + request.method());
                Log.i(TAG, "[LOG] Request headers: " + request.headers());

                // Add DNS resolution logging
                String host = request.url().host();
                Log.i(TAG, "[LOG] Resolving host: " + host);

                try {
                    java.net.InetAddress[] addresses = java.net.InetAddress.getAllByName(host);
                    for (java.net.InetAddress address : addresses) {
                        Log.i(TAG, "[LOG] Resolved IP: " + address.getHostAddress());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "[LOG] DNS resolution failed for " + host + ": " + e.getMessage());
                }

                try (Response response = client.newCall(request).execute()) {
                    Log.i(TAG, "[LOG] Response received for " + context + " - Code: " + response.code());
                    Log.i(TAG, "[LOG] Response headers: " + response.headers());

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.i(TAG, "[LOG] Success response for " + context + ": " + responseBody);
                    } else {
                        String errorBody = response.body().string();
                        Log.w(TAG, "[LOG] Error response for " + context + " - Code: " + response.code());
                        Log.w(TAG, "[LOG] Error body: " + errorBody);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "[LOG] Exception in executeRequest for " + context, e);
                Log.e(TAG, "[LOG] Exception details: " + e.getMessage());
                e.printStackTrace();
            }
        });
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

    /**
     * Initialize hardware control service
     * This provides local hardware control without third-party data sharing
     */
    private void initializeHardwareControl() {
        try {
            // Start hardware control service
            Intent hardwareIntent = new Intent(this, HardwareControlService.class);
            startService(hardwareIntent);

            // Create instance for direct method calls
            hardwareControlService = new HardwareControlService();
            Log.i(TAG, "Hardware control service initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize hardware control: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize location service
     */
    private void initializeLocationService() {
        try {
            // Start location service
            Intent locationIntent = new Intent(this, LocationService.class);
            startService(locationIntent);

            // Create instance for direct method calls
            locationService = new LocationService();
            Log.i(TAG, "Location service initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize location service: " + e.getMessage(), e);
        }
    }

    private void getLocation() {
        Log.i(TAG, "Executing getLocation command");

        if (locationService != null && locationService.isLocationEnabled()) {
            Location location = locationService.getLastKnownLocation();
            if (location != null) {
                Log.i(TAG, "Location retrieved: " + location.getLatitude() + ", " + location.getLongitude());
                sendLocation(location);
                sendCommandResponse("getLocation", true,
                        "Location retrieved: " + location.getLatitude() + ", " + location.getLongitude(), null);
            } else {
                Log.w(TAG, "No location available");
                sendCommandResponse("getLocation", false, "No location available", null);
            }
        } else {
            Log.w(TAG, "Location service not available");
            sendCommandResponse("getLocation", false, "Location service not available", null);
        }
    }

    /**
     * Get current ACC status
     */
    private void getAccStatus() {
        Log.i(TAG, "Executing getAccStatus command");

        try {
            boolean accStatus = carassist.cn.API.isAccOn(this);
            String status = accStatus ? "ON" : "OFF";

            Log.i(TAG, "ACC status: " + status);
            sendCommandResponse("getAccStatus", true, "ACC Status: " + status, null);

            // Send ACC status to server
            sendAccStatusToServer(accStatus);

        } catch (Exception e) {
            Log.e(TAG, "Error getting ACC status: " + e.getMessage(), e);
            sendCommandResponse("getAccStatus", false, "Error getting ACC status: " + e.getMessage(), null);
        }
    }

    /**
     * Start ACC monitoring service
     */
    private void startAccMonitoring() {
        Log.i(TAG, "Executing startAccMonitoring command");

        try {
            // Start ACC monitoring service
            Intent accIntent = new Intent(this, AccMonitoringService.class);
            startService(accIntent);

            Log.i(TAG, "ACC monitoring service started");
            sendCommandResponse("startAccMonitoring", true, "ACC monitoring started", null);

        } catch (Exception e) {
            Log.e(TAG, "Error starting ACC monitoring: " + e.getMessage(), e);
            sendCommandResponse("startAccMonitoring", false, "Error starting ACC monitoring: " + e.getMessage(), null);
        }
    }

    /**
     * Stop ACC monitoring service
     */
    private void stopAccMonitoring() {
        Log.i(TAG, "Executing stopAccMonitoring command");

        try {
            // Stop ACC monitoring service
            Intent accIntent = new Intent(this, AccMonitoringService.class);
            stopService(accIntent);

            Log.i(TAG, "ACC monitoring service stopped");
            sendCommandResponse("stopAccMonitoring", true, "ACC monitoring stopped", null);

        } catch (Exception e) {
            Log.e(TAG, "Error stopping ACC monitoring: " + e.getMessage(), e);
            sendCommandResponse("stopAccMonitoring", false, "Error stopping ACC monitoring: " + e.getMessage(), null);
        }
    }

    /**
     * Send ACC status to server
     */
    private void sendAccStatusToServer(boolean accOn) {
        try {
            Log.d(TAG, "=== SENDING ACC STATUS ===");
            Log.d(TAG, "Device ID: " + deviceId);
            Log.d(TAG, "Server URL: " + API_BASE_URL);

            String endpoint = String.format("/dashcams/%s/acc-status", deviceId);
            String url = API_BASE_URL + endpoint;

            Log.d(TAG, "ACC Status URL: " + url);

            // Validate URL to prevent routing issues
            if (!url.startsWith("https://e-android-fleet-backend-render.onrender.com")) {
                Log.e(TAG, "Invalid URL detected: " + url);
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            JSONObject accStatusData = new JSONObject();
            accStatusData.put("deviceId", deviceId);
            accStatusData.put("accStatus", accOn ? "ON" : "OFF");
            accStatusData.put("timestamp", sdf.format(new java.util.Date()));

            String jsonPayload = accStatusData.toString();
            Log.d(TAG, "ACC status payload: " + jsonPayload);

            RequestBody requestBody = RequestBody.create(JSON, jsonPayload);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "FleetManagement-Android/1.0")
                    .addHeader("Device-ID", deviceId)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Sending ACC status request");

            try (Response response = client.newCall(request).execute()) {
                Log.d(TAG, "ACC status response received - Code: " + response.code());
                Log.d(TAG, "Response Headers: " + response.headers());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.i(TAG, "=== ACC STATUS SENT SUCCESS ===");
                    Log.i(TAG, "Response: " + responseBody);
                } else {
                    String errorBody = response.body().string();
                    Log.w(TAG, "=== ACC STATUS SENT FAILED ===");
                    Log.w(TAG, "Error code: " + response.code());
                    Log.w(TAG, "Error response: " + errorBody);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "=== ACC STATUS SENT ERROR ===");
            Log.e(TAG, "Error sending ACC status", e);
            Log.e(TAG, "Exception details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clear APN proxy settings to ensure direct connections
     * This prevents any SIM from forcing proxy usage
     */
    private void clearApnProxySettings() {
        try {
            Log.i(TAG, "=== CLEARING APN PROXY SETTINGS ===");

            // Get current APN
            android.database.Cursor cursor = getContentResolver().query(
                    android.provider.Telephony.Carriers.CONTENT_URI,
                    new String[] { "_id", "name", "apn", "proxy", "port" },
                    android.provider.Telephony.Carriers.CURRENT + "=1",
                    null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndex("_id");
                int nameColumn = cursor.getColumnIndex("name");
                int proxyColumn = cursor.getColumnIndex("proxy");
                int portColumn = cursor.getColumnIndex("port");

                if (idColumn >= 0) {
                    long apnId = cursor.getLong(idColumn);
                    String apnName = cursor.getString(nameColumn);
                    String currentProxy = cursor.getString(proxyColumn);
                    String currentPort = cursor.getString(portColumn);

                    Log.i(TAG, "Current APN: " + apnName + " (ID: " + apnId + ")");
                    Log.i(TAG, "Current proxy: " + currentProxy + ":" + currentPort);

                    // Clear proxy settings if they exist
                    if (currentProxy != null && !currentProxy.isEmpty()) {
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put("proxy", "");
                        values.put("port", "");

                        int updated = getContentResolver().update(
                                android.provider.Telephony.Carriers.CONTENT_URI,
                                values,
                                "_id=?",
                                new String[] { String.valueOf(apnId) });

                        if (updated > 0) {
                            Log.i(TAG, "=== APN PROXY CLEARED SUCCESSFULLY ===");
                            Log.i(TAG, "Removed proxy from APN: " + apnName);
                        } else {
                            Log.w(TAG, "Failed to clear APN proxy");
                        }
                    } else {
                        Log.i(TAG, "APN already has no proxy configured");
                    }
                }
                cursor.close();
            } else {
                Log.w(TAG, "No current APN found");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error clearing APN proxy settings: " + e.getMessage(), e);
        }
    }

    // ==================== AUDIO COMMUNICATION METHODS ====================

    /**
     * Initialize audio manager and permissions
     */
    private void initializeAudio() {
        try {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            Log.i(TAG, "Audio manager initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize audio manager: " + e.getMessage(), e);
        }
    }

    /**
     * Start audio recording
     */
    private void startAudioRecording() {
        Log.i(TAG, "=== STARTING AUDIO RECORDING ===");

        if (isRecording.get()) {
            Log.w(TAG, "Audio recording already in progress");
            sendCommandResponse("startAudioRecording", false, "Audio recording already in progress", null);
            return;
        }

        try {
            // Create audio file path
            String audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            String fileName = "audio_" + System.currentTimeMillis() + ".wav";
            String audioPath = audioDir + "/" + fileName;

            File audioFile = new File(audioPath);
            audioFile.getParentFile().mkdirs();

            currentAudioFile.set(audioPath);

            // Initialize MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(SAMPLE_RATE);
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setOutputFile(audioPath);

            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording.set(true);

            Log.i(TAG, "Audio recording started: " + audioPath);
            sendCommandResponse("startAudioRecording", true, "Audio recording started", null);

        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio recording: " + e.getMessage(), e);
            sendCommandResponse("startAudioRecording", false, "Failed to start recording: " + e.getMessage(), null);
        }
    }

    /**
     * Stop audio recording and upload
     */
    private void stopAudioRecording() {
        Log.i(TAG, "=== STOPPING AUDIO RECORDING ===");

        if (!isRecording.get()) {
            Log.w(TAG, "No audio recording in progress");
            sendCommandResponse("stopAudioRecording", false, "No audio recording in progress", null);
            return;
        }

        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }

            isRecording.set(false);

            String audioPath = currentAudioFile.get();
            if (audioPath != null) {
                File audioFile = new File(audioPath);
                if (audioFile.exists()) {
                    Log.i(TAG, "Audio recording completed: " + audioPath);
                    uploadAudio(audioFile, "remote_recording");
                    sendCommandResponse("stopAudioRecording", true, "Audio recording completed and uploaded", null);
                } else {
                    Log.e(TAG, "Audio file not found: " + audioPath);
                    sendCommandResponse("stopAudioRecording", false, "Audio file not found", null);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop audio recording: " + e.getMessage(), e);
            sendCommandResponse("stopAudioRecording", false, "Failed to stop recording: " + e.getMessage(), null);
        }
    }

    /**
     * Play audio from base64 data
     */
    private void playAudio(String audioData) {
        Log.i(TAG, "=== PLAYING AUDIO ===");

        if (isPlaying.get()) {
            Log.w(TAG, "Audio already playing, stopping current playback");
            stopAudioPlayback();
        }

        try {
            // Decode base64 audio data
            byte[] audioBytes = android.util.Base64.decode(audioData, android.util.Base64.DEFAULT);

            // Create temporary audio file
            String audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath();
            String fileName = "playback_" + System.currentTimeMillis() + ".wav";
            String audioPath = audioDir + "/" + fileName;

            File audioFile = new File(audioPath);
            audioFile.getParentFile().mkdirs();

            // Write audio data to file
            FileOutputStream fos = new FileOutputStream(audioFile);
            fos.write(audioBytes);
            fos.close();

            // Play audio
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioPath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            isPlaying.set(true);

            // Set completion listener
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying.set(false);
                mp.release();
                audioFile.delete(); // Clean up temporary file
                Log.i(TAG, "Audio playback completed");
            });

            Log.i(TAG, "Audio playback started");
            sendCommandResponse("playAudio", true, "Audio playback started", null);

        } catch (Exception e) {
            Log.e(TAG, "Failed to play audio: " + e.getMessage(), e);
            sendCommandResponse("playAudio", false, "Failed to play audio: " + e.getMessage(), null);
        }
    }

    /**
     * Stop audio playback
     */
    private void stopAudioPlayback() {
        if (mediaPlayer != null && isPlaying.get()) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying.set(false);
                Log.i(TAG, "Audio playback stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio playback: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Play TTS (Text-to-Speech)
     */
    private void playTTS(String text, String commandId) {
        Log.i(TAG, "=== PLAYING TTS ===");
        Log.i(TAG, "TTS Text: " + text);
        Log.i(TAG, "Command ID: " + commandId);

        try {
            // Use Car SDK TTS if available
            if (hardwareControlService != null) {
                carassist.cn.API carApi = new carassist.cn.API(this);
                carApi.playTts(text, carassist.cn.API.TYPE_NOTICE);
                Log.i(TAG, "TTS played using Car SDK");
                sendCommandResponse("playTTS", true, "TTS played successfully", commandId);
            } else {
                // For now, just log that TTS is not available
                Log.w(TAG, "TTS not available - Car SDK not initialized");
                sendCommandResponse("playTTS", false, "TTS not available - Car SDK not initialized", commandId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to play TTS: " + e.getMessage(), e);
            sendCommandResponse("playTTS", false, "Failed to play TTS: " + e.getMessage(), commandId);
        }
    }

    /**
     * Start two-way audio communication
     */
    private void startTwoWayAudio() {
        Log.i(TAG, "=== STARTING TWO-WAY AUDIO ===");

        try {
            // Initialize audio recording and playback
            initializeAudio();

            // Start continuous audio recording and streaming
            startContinuousAudioStreaming();

            Log.i(TAG, "Two-way audio started");
            sendCommandResponse("startTwoWayAudio", true, "Two-way audio communication started", null);

        } catch (Exception e) {
            Log.e(TAG, "Failed to start two-way audio: " + e.getMessage(), e);
            sendCommandResponse("startTwoWayAudio", false, "Failed to start two-way audio: " + e.getMessage(), null);
        }
    }

    /**
     * Stop two-way audio communication
     */
    private void stopTwoWayAudio() {
        Log.i(TAG, "=== STOPPING TWO-WAY AUDIO ===");

        try {
            stopContinuousAudioStreaming();
            stopAudioPlayback();

            Log.i(TAG, "Two-way audio stopped");
            sendCommandResponse("stopTwoWayAudio", true, "Two-way audio communication stopped", null);

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop two-way audio: " + e.getMessage(), e);
            sendCommandResponse("stopTwoWayAudio", false, "Failed to stop two-way audio: " + e.getMessage(), null);
        }
    }

    /**
     * Start continuous audio streaming for two-way communication
     */
    private void startContinuousAudioStreaming() {
        executorService.execute(() -> {
            try {
                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        BUFFER_SIZE);

                audioTrack = new AudioTrack.Builder()
                        .setAudioAttributes(new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build())
                        .setAudioFormat(new android.media.AudioFormat.Builder()
                                .setEncoding(AUDIO_FORMAT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(BUFFER_SIZE)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();

                audioRecord.startRecording();
                audioTrack.play();

                byte[] buffer = new byte[BUFFER_SIZE];

                while (isRecording.get()) {
                    int readSize = audioRecord.read(buffer, 0, BUFFER_SIZE);
                    if (readSize > 0) {
                        // Send audio data to server
                        sendAudioDataToServer(buffer, readSize);

                        // Play received audio (if any)
                        // This would be implemented based on server response
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error in continuous audio streaming: " + e.getMessage(), e);
            } finally {
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                }
                if (audioTrack != null) {
                    audioTrack.stop();
                    audioTrack.release();
                    audioTrack = null;
                }
            }
        });
    }

    /**
     * Stop continuous audio streaming
     */
    private void stopContinuousAudioStreaming() {
        isRecording.set(false);

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    /**
     * Send audio data to server
     */
    private void sendAudioDataToServer(byte[] audioData, int dataSize) {
        try {
            // Convert audio data to base64
            byte[] trimmedData = new byte[dataSize];
            System.arraycopy(audioData, 0, trimmedData, 0, dataSize);
            String base64Audio = android.util.Base64.encodeToString(trimmedData, android.util.Base64.DEFAULT);

            // Send to server
            String endpoint = String.format("/dashcams/%s/audio", deviceId);
            String url = API_BASE_URL + endpoint;

            JSONObject audioDataJson = new JSONObject();
            audioDataJson.put("deviceId", deviceId);
            audioDataJson.put("audioData", base64Audio);
            audioDataJson.put("timestamp", System.currentTimeMillis());

            RequestBody requestBody = RequestBody.create(JSON, audioDataJson.toString());

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "FleetManagement-Android/1.0")
                    .addHeader("Device-ID", deviceId)
                    .post(requestBody)
                    .build();

            // Send asynchronously
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Audio data sent successfully");
                    } else {
                        Log.w(TAG, "Failed to send audio data: " + response.code());
                    }
                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e(TAG, "Error sending audio data: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing audio data: " + e.getMessage(), e);
        }
    }

    /**
     * Upload audio file to server
     */
    private void uploadAudio(File audioFile, String eventType) {
        Log.i(TAG, "=== UPLOAD AUDIO STARTED ===");
        Log.i(TAG, "Audio file: " + audioFile.getAbsolutePath());
        Log.i(TAG, "Event type: " + eventType);
        Log.i(TAG, "File exists: " + audioFile.exists());
        Log.i(TAG, "File size: " + audioFile.length() + " bytes");

        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file does not exist: " + audioFile.getAbsolutePath());
            return;
        }

        executorService.execute(() -> {
            try {
                Log.i(TAG, "Starting audio upload in background thread");

                String endpoint = String.format("/dashcams/%s/audio", deviceId);
                String url = API_BASE_URL + endpoint;

                Log.i(TAG, "Upload URL: " + url);

                // Determine media type based on file extension
                MediaType mediaType = MediaType.parse("audio/wav");
                String fileName = audioFile.getName().toLowerCase();
                if (fileName.endsWith(".mp3")) {
                    mediaType = MediaType.parse("audio/mpeg");
                } else if (fileName.endsWith(".aac")) {
                    mediaType = MediaType.parse("audio/aac");
                } else if (fileName.endsWith(".m4a")) {
                    mediaType = MediaType.parse("audio/mp4");
                }

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("audio", audioFile.getName(),
                                RequestBody.create(mediaType, audioFile))
                        .addFormDataPart("eventType", eventType)
                        .addFormDataPart("deviceId", deviceId)
                        .addFormDataPart("timestamp", String.valueOf(System.currentTimeMillis()))
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "FleetManagement-Android/1.0")
                        .addHeader("Device-ID", deviceId)
                        .post(requestBody)
                        .build();

                Log.i(TAG, "Sending audio upload request");

                try (Response response = client.newCall(request).execute()) {
                    Log.i(TAG, "Audio upload response received - Code: " + response.code());
                    Log.i(TAG, "Response Headers: " + response.headers());

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.i(TAG, "=== AUDIO UPLOAD SUCCESS ===");
                        Log.i(TAG, "Response: " + responseBody);
                        Log.i(TAG, "Audio uploaded successfully to server");
                    } else {
                        String errorBody = response.body().string();
                        Log.e(TAG, "=== AUDIO UPLOAD FAILED ===");
                        Log.e(TAG, "Error code: " + response.code());
                        Log.e(TAG, "Error response: " + errorBody);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error uploading audio", e);
                Log.e(TAG, "Exception details: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Handle camera capture result from CameraCaptureActivity
     */
    public void handleCameraCaptureResult(boolean success, String photoPath, String error, String commandId) {
        if (success && photoPath != null) {
            Log.i(TAG, "=== CAMERA CAPTURE SUCCESS ===");
            Log.i(TAG, "Photo captured at: " + photoPath);

            File photoFile = new File(photoPath);
            if (photoFile.exists()) {
                Log.i(TAG, "Photo file exists, uploading to server...");
                uploadPhoto(photoFile, commandId != null ? commandId : "camera_fallback");
                sendCommandResponse("takePhoto", true,
                        "Photo captured and uploaded successfully (Camera2 fallback)", commandId);
            } else {
                Log.e(TAG, "Photo file does not exist at path: " + photoPath);
                sendCommandResponse("takePhoto", false,
                        "Photo captured but file not found: " + photoPath, commandId);
            }
        } else {
            Log.e(TAG, "=== CAMERA CAPTURE FAILED ===");
            Log.e(TAG, "Error: " + error);
            sendCommandResponse("takePhoto", false,
                    "Camera capture failed: " + error, commandId);
        }
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