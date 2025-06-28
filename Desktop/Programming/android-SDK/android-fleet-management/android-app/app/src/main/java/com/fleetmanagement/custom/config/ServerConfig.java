package com.fleetmanagement.custom.config;

/**
 * Server configuration for fleet management system
 * Update these values with your actual server endpoints
 */
public class ServerConfig {

    // Base server URL - UPDATE THIS WITH YOUR SERVER URL
    public static final String BASE_URL = "https://e-android-fleet-backend-render.onrender.com/api";

    // Alternative: Use HTTP for development (not recommended for production)
    // public static final String BASE_URL = "http://your-fleet-server.com/api";

    // API Endpoints
    public static final String ENDPOINT_STATUS = "/status";
    public static final String ENDPOINT_LOCATION = "/location";
    public static final String ENDPOINT_EVENT = "/event";
    public static final String ENDPOINT_EMERGENCY = "/emergency";
    public static final String ENDPOINT_MEDIA = "/media";
    public static final String ENDPOINT_PHOTO = "/photo";
    public static final String ENDPOINT_VIDEO = "/video";
    public static final String ENDPOINT_HEARTBEAT = "/heartbeat";

    // Authentication
    public static final String API_KEY = "your-api-key-here";
    public static final String DEVICE_ID = "dashcam-001"; // Unique device identifier

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
     * Get location endpoint URL
     */
    public static String getLocationUrl() {
        return getUrl(ENDPOINT_LOCATION);
    }

    /**
     * Get event endpoint URL
     */
    public static String getEventUrl() {
        return getUrl(ENDPOINT_EVENT);
    }

    /**
     * Get emergency endpoint URL
     */
    public static String getEmergencyUrl() {
        return getUrl(ENDPOINT_EMERGENCY);
    }

    /**
     * Get media upload endpoint URL
     */
    public static String getMediaUrl() {
        return getUrl(ENDPOINT_MEDIA);
    }

    /**
     * Get photo upload endpoint URL
     */
    public static String getPhotoUrl() {
        return getUrl(ENDPOINT_PHOTO);
    }

    /**
     * Get video upload endpoint URL
     */
    public static String getVideoUrl() {
        return getUrl(ENDPOINT_VIDEO);
    }

    /**
     * Get heartbeat endpoint URL
     */
    public static String getHeartbeatUrl() {
        return getUrl(ENDPOINT_HEARTBEAT);
    }
}