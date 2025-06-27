package com.fleetmanagement.custom.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fleetmanagement.custom.services.FleetManagementService;

import carassist.cn.CarIntents;

public class DashcamEventReceiver extends BroadcastReceiver {

    private static final String TAG = "DashcamEventReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Dashcam event received: " + action);

        switch (action) {
            case CarIntents.ACTION_MONITOR_NOTIFY:
                handleMonitorNotify(context, intent);
                break;

            case CarIntents.ACTION_RECORD_FILE:
                handleRecordFile(context, intent);
                break;

            case CarIntents.ACTION_DELETE_FILE:
                handleDeleteFile(context, intent);
                break;

            case CarIntents.ACTION_CAMERA_LIVING_CALLBACK:
                handleCameraLivingCallback(context, intent);
                break;

            case CarIntents.ACTION_RECORDING_STORAGE_SLOW:
                handleRecordingStorageSlow(context, intent);
                break;

            case CarIntents.ACTION_CAMERA_SNAPSHOT_CALLBACK:
                handleCameraSnapshotCallback(context, intent);
                break;

            case CarIntents.ACTION_CAPTURE_CUSTOM_VIDEO:
                handleCaptureCustomVideo(context, intent);
                break;

            case CarIntents.ACTION_CAPTURE_FILE_INFO:
                handleCaptureFileInfo(context, intent);
                break;

            default:
                Log.w(TAG, "Unknown dashcam event: " + action);
                break;
        }
    }

    private void handleMonitorNotify(Context context, Intent intent) {
        // Handle monitoring notifications
        String data = intent.getStringExtra("data");
        Log.i(TAG, "Monitor notification: " + data);

        // Forward to fleet management service
        Intent serviceIntent = new Intent(context, FleetManagementService.class);
        serviceIntent.setAction("MONITOR_NOTIFY");
        serviceIntent.putExtra("data", data);
        context.startService(serviceIntent);
    }

    private void handleRecordFile(Context context, Intent intent) {
        // Handle recording file events
        String filePath = intent.getStringExtra("file_path");
        String fileType = intent.getStringExtra("file_type");
        Log.i(TAG, "Record file: " + fileType + " - " + filePath);

        // Forward to fleet management service
        Intent serviceIntent = new Intent(context, FleetManagementService.class);
        serviceIntent.setAction("RECORD_FILE");
        serviceIntent.putExtra("file_path", filePath);
        serviceIntent.putExtra("file_type", fileType);
        context.startService(serviceIntent);
    }

    private void handleDeleteFile(Context context, Intent intent) {
        // Handle file deletion events
        String filePath = intent.getStringExtra("file_path");
        Log.i(TAG, "Delete file: " + filePath);

        // Forward to fleet management service
        Intent serviceIntent = new Intent(context, FleetManagementService.class);
        serviceIntent.setAction("DELETE_FILE");
        serviceIntent.putExtra("file_path", filePath);
        context.startService(serviceIntent);
    }

    private void handleCameraLivingCallback(Context context, Intent intent) {
        // Handle live streaming callbacks
        String data = intent.getStringExtra("data");
        Log.i(TAG, "Camera living callback: " + data);

        // Forward to fleet management service
        Intent serviceIntent = new Intent(context, FleetManagementService.class);
        serviceIntent.setAction("CAMERA_LIVING_CALLBACK");
        serviceIntent.putExtra("data", data);
        context.startService(serviceIntent);
    }

    private void handleRecordingStorageSlow(Context context, Intent intent) {
        // Handle storage slow warnings
        Log.w(TAG, "Recording storage is slow");

        // Forward to fleet management service
        Intent serviceIntent = new Intent(context, FleetManagementService.class);
        serviceIntent.setAction("RECORDING_STORAGE_SLOW");
        context.startService(serviceIntent);
    }

    private void handleCameraSnapshotCallback(Context context, Intent intent) {
        // Handle snapshot callbacks
        String data = intent.getStringExtra("data");
        Log.i(TAG, "Camera snapshot callback: " + data);

        // Forward to fleet management service
        Intent serviceIntent = new Intent(context, FleetManagementService.class);
        serviceIntent.setAction("CAMERA_SNAPSHOT_CALLBACK");
        serviceIntent.putExtra("data", data);
        context.startService(serviceIntent);
    }

    private void handleCaptureCustomVideo(Context context, Intent intent) {
        // Handle custom video capture events
        String data = intent.getStringExtra("data");
        Log.i(TAG, "Capture custom video: " + data);

        // Forward to fleet management service
        Intent serviceIntent = new Intent(context, FleetManagementService.class);
        serviceIntent.setAction("CAPTURE_CUSTOM_VIDEO");
        serviceIntent.putExtra("data", data);
        context.startService(serviceIntent);
    }

    private void handleCaptureFileInfo(Context context, Intent intent) {
        // Handle file info capture events
        String data = intent.getStringExtra("data");
        Log.i(TAG, "Capture file info: " + data);

        // Forward to fleet management service
        Intent serviceIntent = new Intent(context, FleetManagementService.class);
        serviceIntent.setAction("CAPTURE_FILE_INFO");
        serviceIntent.putExtra("data", data);
        context.startService(serviceIntent);
    }
}