package com.fleetmanagement.custom.models;

import android.location.Location;

public class DashcamStatus {
    private String status;
    private int batteryLevel;
    private Location location;
    private float speed;
    private boolean isRecording;
    private boolean isLiveStreaming;
    private boolean isMotionDetected;
    private long storageAvailable;
    private long lastUpdateTime;
    private String deviceId = "";

    public DashcamStatus() {
        this.status = "Unknown";
        this.batteryLevel = 0;
        this.speed = 0.0f;
        this.isRecording = false;
        this.isLiveStreaming = false;
        this.isMotionDetected = false;
        this.storageAvailable = 0;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public DashcamStatus(String status, int batteryLevel, Location location, float speed) {
        this.status = status;
        this.batteryLevel = batteryLevel;
        this.location = location;
        this.speed = speed;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public boolean isLiveStreaming() {
        return isLiveStreaming;
    }

    public void setLiveStreaming(boolean liveStreaming) {
        isLiveStreaming = liveStreaming;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public boolean isMotionDetected() {
        return isMotionDetected;
    }

    public void setMotionDetected(boolean motionDetected) {
        isMotionDetected = motionDetected;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public long getStorageAvailable() {
        return storageAvailable;
    }

    public void setStorageAvailable(long storageAvailable) {
        this.storageAvailable = storageAvailable;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return "DashcamStatus{" +
                "deviceId='" + deviceId + '\'' +
                ", status='" + status + '\'' +
                ", batteryLevel=" + batteryLevel +
                ", speed=" + speed +
                ", isRecording=" + isRecording +
                ", isLiveStreaming=" + isLiveStreaming +
                ", isMotionDetected=" + isMotionDetected +
                ", storageAvailable=" + storageAvailable +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
    }
}