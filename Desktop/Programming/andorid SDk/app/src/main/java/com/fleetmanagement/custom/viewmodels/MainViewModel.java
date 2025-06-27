package com.fleetmanagement.custom.viewmodels;

import android.app.Application;
import android.location.Location;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fleetmanagement.custom.models.DashcamStatus;
import com.fleetmanagement.custom.services.LocationTrackingService;

public class MainViewModel extends AndroidViewModel {

    private MutableLiveData<DashcamStatus> dashcamStatus;
    private MutableLiveData<Location> currentLocation;
    private MutableLiveData<Float> currentSpeed;
    private MutableLiveData<String> statusMessage;

    public MainViewModel(Application application) {
        super(application);

        dashcamStatus = new MutableLiveData<>();
        currentLocation = new MutableLiveData<>();
        currentSpeed = new MutableLiveData<>();
        statusMessage = new MutableLiveData<>();

        // Initialize with default values
        dashcamStatus.setValue(new DashcamStatus());
    }

    public LiveData<DashcamStatus> getDashcamStatus() {
        return dashcamStatus;
    }

    public LiveData<Location> getCurrentLocation() {
        return currentLocation;
    }

    public LiveData<Float> getCurrentSpeed() {
        return currentSpeed;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public void updateDashcamStatus(DashcamStatus status) {
        dashcamStatus.postValue(status);
    }

    public void updateLocation(Location location) {
        currentLocation.postValue(location);

        // Update speed if available
        if (location.hasSpeed()) {
            float speedKmh = location.getSpeed() * 3.6f; // Convert m/s to km/h
            currentSpeed.postValue(speedKmh);
        }
    }

    public void updateStatus(String status) {
        statusMessage.postValue(status);
    }

    public void startLocationTracking() {
        // This would typically start the location service
        // For now, we'll just update the status
        updateStatus("Location tracking started");
    }

    public void uploadPhotoToServer(String photoData) {
        // TODO: Implement photo upload to server
        updateStatus("Photo upload initiated");

        // Simulate upload process
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate network delay
                updateStatus("Photo uploaded successfully");
            } catch (InterruptedException e) {
                updateStatus("Photo upload failed");
            }
        }).start();
    }

    public void uploadVideoToServer(String videoData) {
        // TODO: Implement video upload to server
        updateStatus("Video upload initiated");

        // Simulate upload process
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Simulate network delay
                updateStatus("Video uploaded successfully");
            } catch (InterruptedException e) {
                updateStatus("Video upload failed");
            }
        }).start();
    }

    public void sendEmergencyAlert() {
        // TODO: Implement emergency alert to server
        updateStatus("Emergency alert sent");

        // Simulate emergency alert
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate network delay
                updateStatus("Emergency alert confirmed by server");
            } catch (InterruptedException e) {
                updateStatus("Emergency alert failed");
            }
        }).start();
    }

    public void recordViolentEvent(int eventType, String eventDescription) {
        // TODO: Implement violent event recording
        updateStatus("Violent event recorded: " + eventDescription);

        // Simulate event recording
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Simulate processing delay
                updateStatus("Event data sent to server");
            } catch (InterruptedException e) {
                updateStatus("Event recording failed");
            }
        }).start();
    }
}