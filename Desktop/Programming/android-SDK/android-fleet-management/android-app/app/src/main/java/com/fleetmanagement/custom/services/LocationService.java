package com.fleetmanagement.custom.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Location Service for GPS tracking
 * Provides location updates to the fleet management system
 */
public class LocationService extends Service implements LocationListener {

    private static final String TAG = "LocationService";
    private LocationManager locationManager;
    private Location lastKnownLocation;
    private boolean isLocationEnabled = false;

    // Location storage service
    private LocationStorageService locationStorage;

    // Location update intervals (in milliseconds)
    private static final long MIN_TIME_BETWEEN_UPDATES = 5000; // 5 seconds
    private static final float MIN_DISTANCE_BETWEEN_UPDATES = 10; // 10 meters

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "LocationService created");
        initializeLocationManager();
        initializeLocationStorage();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "LocationService started");
        startLocationUpdates();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Initialize location manager
     */
    private void initializeLocationManager() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                Log.i(TAG, "Location manager initialized, GPS enabled: " + isLocationEnabled);
            } else {
                Log.e(TAG, "Location manager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing location manager: " + e.getMessage(), e);
        }
    }

    /**
     * Initialize location storage service
     */
    private void initializeLocationStorage() {
        try {
            locationStorage = new LocationStorageService(this);
            Log.i(TAG, "Location storage service initialized");

            // Clean old data periodically
            locationStorage.cleanOldLocations();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing location storage: " + e.getMessage(), e);
        }
    }

    /**
     * Start location updates
     */
    private void startLocationUpdates() {
        if (locationManager == null || !isLocationEnabled) {
            Log.w(TAG, "Location manager not available or location disabled");
            return;
        }

        // Log provider status before starting updates
        logProviderStatus();

        try {
            // Request GPS location updates
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_BETWEEN_UPDATES,
                        this);
                Log.i(TAG, "GPS location updates started");
            }

            // Request network location updates as backup
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BETWEEN_UPDATES,
                        MIN_DISTANCE_BETWEEN_UPDATES,
                        this);
                Log.i(TAG, "Network location updates started");
            }

            // Get last known location
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (gpsLocation != null) {
                lastKnownLocation = gpsLocation;
                Log.i(TAG, "Last known GPS location: " + gpsLocation.getLatitude() + ", " + gpsLocation.getLongitude());
            } else if (networkLocation != null) {
                lastKnownLocation = networkLocation;
                Log.i(TAG, "Last known network location: " + networkLocation.getLatitude() + ", "
                        + networkLocation.getLongitude());
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception requesting location updates: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates: " + e.getMessage(), e);
        }
    }

    /**
     * Stop location updates
     */
    private void stopLocationUpdates() {
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
                Log.i(TAG, "Location updates stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping location updates: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Get last known location from database
     */
    public Location getLastKnownLocation() {
        // First try memory cache
        if (lastKnownLocation != null) {
            return lastKnownLocation;
        }

        // Fallback to database
        if (locationStorage != null) {
            Location dbLocation = locationStorage.getLastKnownLocation();
            if (dbLocation != null) {
                lastKnownLocation = dbLocation;
                return dbLocation;
            }
        }

        return null;
    }

    /**
     * Check if location is enabled
     */
    public boolean isLocationEnabled() {
        return isLocationEnabled;
    }

    /**
     * Force location update
     */
    public void requestLocationUpdate() {
        if (locationManager != null && isLocationEnabled) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        onLocationChanged(location);
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception requesting location: " + e.getMessage(), e);
            }
        }
    }

    // LocationListener callbacks
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Calculate time since last update
        long currentTime = System.currentTimeMillis();
        long timeSinceLastUpdate = 0;
        if (lastKnownLocation != null) {
            timeSinceLastUpdate = currentTime - lastKnownLocation.getTime();
        }

        // Enhanced logging to identify frequent update source
        Log.i(TAG, String.format(
                "Location update from %s: lat=%.6f, lon=%.6f, accuracy=%.1fm, speed=%.2fm/s, timeSinceLast=%.1fs",
                location.getProvider(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getSpeed(),
                timeSinceLastUpdate / 1000.0));

        // Log if updates are coming too frequently
        if (timeSinceLastUpdate > 0 && timeSinceLastUpdate < MIN_TIME_BETWEEN_UPDATES) {
            Log.w(TAG,
                    String.format("FREQUENT UPDATE WARNING: %s provider sent update after %.1fs (expected min %.1fs)",
                            location.getProvider(),
                            timeSinceLastUpdate / 1000.0,
                            MIN_TIME_BETWEEN_UPDATES / 1000.0));
        }

        lastKnownLocation = location;

        // Save to local database
        if (locationStorage != null) {
            long locationId = locationStorage.saveLocation(location);
            Log.d(TAG, "Location saved to database with ID: " + locationId);
        }

        // Send location to server via ServerCommunicationService
        try {
            ServerCommunicationService serverService = new ServerCommunicationService();
            serverService.sendLocation(location);
        } catch (Exception e) {
            Log.e(TAG, "Error sending location to server: " + e.getMessage(), e);
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.i(TAG, "Location provider enabled: " + provider);
        isLocationEnabled = true;
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.i(TAG, "Location provider disabled: " + provider);
        isLocationEnabled = false;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Location provider status changed: " + provider + " = " + status);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        Log.i(TAG, "LocationService destroyed");
    }

    /**
     * Get location history from database
     */
    public List<Location> getLocationHistory(int limit) {
        if (locationStorage != null) {
            return locationStorage.getLocationHistory(limit);
        }
        return new ArrayList<>();
    }

    /**
     * Get unsynced locations for offline sync
     */
    public List<Location> getUnsyncedLocations() {
        if (locationStorage != null) {
            return locationStorage.getUnsyncedLocations();
        }
        return new ArrayList<>();
    }

    /**
     * Mark location as synced
     */
    public boolean markLocationAsSynced(long locationId) {
        if (locationStorage != null) {
            return locationStorage.markLocationAsSynced(locationId);
        }
        return false;
    }

    /**
     * Log status of all location providers
     */
    public void logProviderStatus() {
        if (locationManager == null) {
            Log.w(TAG, "Location manager is null");
            return;
        }

        Log.i(TAG, "=== Location Provider Status ===");

        // Check GPS provider
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.i(TAG, "GPS Provider: " + (gpsEnabled ? "ENABLED" : "DISABLED"));

        // Check Network provider
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Log.i(TAG, "Network Provider: " + (networkEnabled ? "ENABLED" : "DISABLED"));

        // Check Passive provider
        boolean passiveEnabled = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
        Log.i(TAG, "Passive Provider: " + (passiveEnabled ? "ENABLED" : "DISABLED"));

        // Log current settings
        Log.i(TAG, "Min time between updates: " + MIN_TIME_BETWEEN_UPDATES + "ms ("
                + (MIN_TIME_BETWEEN_UPDATES / 1000.0) + "s)");
        Log.i(TAG, "Min distance between updates: " + MIN_DISTANCE_BETWEEN_UPDATES + "m");
        Log.i(TAG, "=================================");
    }

    /**
     * Get database statistics
     */
    public String getDatabaseStats() {
        if (locationStorage != null) {
            return locationStorage.getDatabaseStats();
        }
        return "Storage not available";
    }
}