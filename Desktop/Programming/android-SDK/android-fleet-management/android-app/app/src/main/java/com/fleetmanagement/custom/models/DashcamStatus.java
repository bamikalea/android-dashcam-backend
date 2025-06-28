package com.fleetmanagement.custom.models;

public class DashcamStatus {
    public String status;
    public String timestamp;
    public String deviceId;
    private boolean recording;
    private boolean motionDetected;
    private float storageAvailable;
    private int batteryLevel;

    public DashcamStatus() {}
    public DashcamStatus(String status, String timestamp, String deviceId, boolean recording, boolean motionDetected, float storageAvailable, int batteryLevel) {
        this.status = status;
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.recording = recording;
        this.motionDetected = motionDetected;
        this.storageAvailable = storageAvailable;
        this.batteryLevel = batteryLevel;
    }

    public boolean isRecording() {
        return recording;
    }
    public boolean isMotionDetected() {
        return motionDetected;
    }
    public float getStorageAvailable() {
        return storageAvailable;
    }
    public int getBatteryLevel() {
        return batteryLevel;
    }
} 