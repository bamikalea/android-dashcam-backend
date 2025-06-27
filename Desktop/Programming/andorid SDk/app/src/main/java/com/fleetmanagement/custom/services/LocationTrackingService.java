package com.fleetmanagement.custom.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.fleetmanagement.custom.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "LocationTrackingChannel";
    private static final int NOTIFICATION_ID = 1002;
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_INTERVAL = 5000; // 5 seconds

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Location Tracking Service created");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationCallback();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Location Tracking Service started");

        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Start location updates
        startLocationUpdates();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Location tracking for fleet management");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracking Active")
                .setContentText("Monitoring vehicle location")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    handleLocationUpdate(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,
                    UPDATE_INTERVAL)
                    .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                    .build();

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.i(TAG, "Location updates started");

        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start location updates", e);
        }
    }

    private void handleLocationUpdate(Location location) {
        if (location != null) {
            lastLocation = location;

            Log.d(TAG, String.format("Location: %.6f, %.6f, Speed: %.2f m/s, Accuracy: %.1f m",
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getSpeed(),
                    location.getAccuracy()));

            // Send location to server
            sendLocationToServer(location);

            // Check for speed violations
            checkSpeedViolation(location);
        }
    }

    private void sendLocationToServer(Location location) {
        // TODO: Implement server communication
        // This would send location data to your fleet management server
        Log.d(TAG, "Sending location to server: " + location.getLatitude() + ", " + location.getLongitude());
    }

    private void checkSpeedViolation(Location location) {
        float speedMps = location.getSpeed();
        float speedKph = speedMps * 3.6f; // Convert m/s to km/h

        // Example speed limit check (adjust as needed)
        if (speedKph > 120) { // 120 km/h limit
            Log.w(TAG, "Speed violation detected: " + speedKph + " km/h");

            // Trigger dashcam recording
            Intent fleetIntent = new Intent(this, FleetManagementService.class);
            fleetIntent.setAction("SPEED_VIOLATION");
            fleetIntent.putExtra("speed", speedKph);
            startService(fleetIntent);
        }
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Location Tracking Service destroyed");

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}