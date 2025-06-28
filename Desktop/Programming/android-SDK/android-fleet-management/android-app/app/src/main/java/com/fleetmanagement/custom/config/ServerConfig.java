package com.fleetmanagement.custom.config;

/**
 * Server configuration for fleet management system
 * Update these values with your actual server endpoints
 */
public class ServerConfig {

    // Base server URL - UPDATE THIS WITH YOUR SERVER URL
    public static final String BASE_URL = "https://e-android-fleet-backend-render.onrender.com";

    // Alternative: Use HTTP for development (not recommended for production)
    // public static final String BASE_URL = "http://your-fleet-server.com/api";

    // API Endpoints - Updated to match server structure
    public static final String ENDPOINT_STATUS = "/api/status";
    public static final String ENDPOINT_REGISTER = "/api/dashcams/register";
    public static final String ENDPOINT_DEVICE_STATUS = "/api/dashcams/%s/status";
    public static final String ENDPOINT_LOCATION = "/api/dashcams/%s/location";
    public static final String ENDPOINT_EVENTS = "/api/dashcams/%s/events";
    public static final String ENDPOINT_COMMANDS = "/api/dashcams/%s/commands";
    public static final String ENDPOINT_RESPONSE = "/api/dashcams/%s/response";
    public static final String ENDPOINT_HEARTBEAT = "/api/dashcams/%s/heartbeat";
    public static final String ENDPOINT_MEDIA = "/api/dashcams/%s/media";
    public static final String ENDPOINT_PHOTO = "/api/dashcams/%s/photo";
    public static final String ENDPOINT_VIDEO = "/api/dashcams/%s/video";

    // Authentication
    public static final String API_KEY = "your-api-key-here";
    public static final String DEVICE_ID = "13f15b0094dcc44a"; // Unique device identifier

    // Upload settings
    public static final int UPLOAD_TIMEOUT = 30000; // 30 seconds
    public static final int MAX_RETRY_ATTEMPTS = 3;

    // Heartbeat interval (milliseconds)
    public static final long HEARTBEAT_INTERVAL = 60000; // 1 minute

    // Media upload settings
    public static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    public static final String UPLOAD_QUALITY = "medium"; // low, medium, high

    // Server communication settings
    public static final boolean ENABLE_SSL_PINNING = true;
    public static final boolean ENABLE_COMPRESSION = true;
    public static final boolean ENABLE_CACHING = false; // Disable for real-time data

    // Emergency settings
    public static final String EMERGENCY_CONTACT = "+1234567890";
    public static final String EMERGENCY_EMAIL = "emergency@yourcompany.com";

    /**
     * Get full URL for an endpoint
     */
    public static String getUrl(String endpoint) {
        return BASE_URL + endpoint;
    }

    /**
     * Get status endpoint URL
     */
    public static String getStatusUrl() {
        return getUrl(ENDPOINT_STATUS);
    }

    /**
     * Get registration endpoint URL
     */
    public static String getRegisterUrl() {
        return getUrl(ENDPOINT_REGISTER);
    }

    /**
     * Get device status endpoint URL
     */
    public static String getDeviceStatusUrl(String deviceId) {
        return getUrl(String.format(ENDPOINT_DEVICE_STATUS, deviceId));
    }

    /**
     * Get location endpoint URL
     */
    public static String getLocationUrl(String deviceId) {
        return getUrl(String.format(ENDPOINT_LOCATION, deviceId));
    }

    /**
     * Get events endpoint URL
     */
    public static String getEventsUrl(String deviceId) {
        return getUrl(String.format(ENDPOINT_EVENTS, deviceId));
    }

    /**
     * Get commands endpoint URL
     */
    public static String getCommandsUrl(String deviceId) {
        return getUrl(String.format(ENDPOINT_COMMANDS, deviceId));
    }

    /**
     * Get response endpoint URL
     */
    public static String getResponseUrl(String deviceId) {
        return getUrl(String.format(ENDPOINT_RESPONSE, deviceId));
    }

    /**
     * Get heartbeat endpoint URL
     */
    public static String getHeartbeatUrl(String deviceId) {
        return getUrl(String.format(ENDPOINT_HEARTBEAT, deviceId));
    }

    /**
     * Get media upload endpoint URL
     */
    public static String getMediaUrl(String deviceId) {
        return getUrl(String.format(ENDPOINT_MEDIA, deviceId));
    }

    /**
     * Get photo upload endpoint URL
     */
    public static String getPhotoUrl(String deviceId) {
        return getUrl(String.format(ENDPOINT_PHOTO, deviceId));
    }

    /**
     * Get video upload endpoint URL
     */
    public static String getVideoUrl(String deviceId) {
        return getUrl(String.format(ENDPOINT_VIDEO, deviceId));
    }
}