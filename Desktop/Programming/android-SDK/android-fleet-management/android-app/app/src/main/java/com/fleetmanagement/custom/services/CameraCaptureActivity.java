package com.fleetmanagement.custom.services;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Camera Capture Activity using Camera2 API
 * Provides fallback camera functionality when Car SDK is not available
 */
public class CameraCaptureActivity extends Activity {
    private static final String TAG = "CameraCaptureActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private ImageReader imageReader;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    private String photoPath;
    private String commandId;
    private boolean isCapturing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get parameters from intent
        photoPath = getIntent().getStringExtra("photo_path");
        commandId = getIntent().getStringExtra("command_id");

        if (photoPath == null) {
            Log.e(TAG, "No photo path provided");
            finish();
            return;
        }

        Log.i(TAG, "CameraCaptureActivity started for: " + photoPath);

        // Create a simple layout with TextureView
        textureView = new TextureView(this);
        setContentView(textureView);

        // Check camera permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
                    REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
                sendFailureResponse("Camera permission denied");
                finish();
            }
        }
    }

    private void startCamera() {
        startBackgroundThread();

        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                }
            });
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = getBackCameraId(manager);
            if (cameraId == null) {
                Log.e(TAG, "No back camera found");
                sendFailureResponse("No back camera found");
                finish();
                return;
            }

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    cameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                    Log.e(TAG, "Camera open error: " + error);
                    sendFailureResponse("Camera open error: " + error);
                    finish();
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
            sendFailureResponse("Camera access exception: " + e.getMessage());
            finish();
        }
    }

    private String getBackCameraId(CameraManager manager) throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return null;
    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(1920, 1080);

            Surface surface = new Surface(texture);

            // Create ImageReader for photo capture
            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    if (isCapturing) {
                        Image image = reader.acquireLatestImage();
                        if (image != null) {
                            saveImage(image);
                            image.close();
                            isCapturing = false;
                        }
                    }
                }
            }, backgroundHandler);

            CaptureRequest.Builder captureRequestBuilder = cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(imageReader.getSurface());

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            try {
                                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null,
                                        backgroundHandler);

                                // Take photo after a short delay
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        takePhoto();
                                    }
                                }, 1000);

                            } catch (CameraAccessException e) {
                                Log.e(TAG, "Camera access exception in preview", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "Camera capture session configuration failed");
                            sendFailureResponse("Camera capture session configuration failed");
                            finish();
                        }
                    }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception in preview", e);
            sendFailureResponse("Camera access exception in preview: " + e.getMessage());
            finish();
        }
    }

    private void takePhoto() {
        try {
            if (cameraDevice == null)
                return;

            CaptureRequest.Builder captureBuilder = cameraDevice
                    .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            isCapturing = true;

            cameraCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                        @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.i(TAG, "Photo capture completed");
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception in photo capture", e);
            sendFailureResponse("Camera access exception in photo capture: " + e.getMessage());
            finish();
        }
    }

    private void saveImage(Image image) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);

            File photoFile = new File(photoPath);
            FileOutputStream output = new FileOutputStream(photoFile);
            output.write(bytes);
            output.close();

            Log.i(TAG, "Photo saved to: " + photoPath);

            // Send success response
            sendSuccessResponse();

        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            sendFailureResponse("Error saving image: " + e.getMessage());
        }
    }

    private void sendSuccessResponse() {
        // Send success response back to the service via broadcast
        Intent resultIntent = new Intent("CAMERA_CAPTURE_RESULT");
        resultIntent.putExtra("success", true);
        resultIntent.putExtra("photo_path", photoPath);
        resultIntent.putExtra("command_id", commandId);
        sendBroadcast(resultIntent);

        Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void sendFailureResponse(String error) {
        // Send failure response back to the service via broadcast
        Intent resultIntent = new Intent("CAMERA_CAPTURE_RESULT");
        resultIntent.putExtra("success", false);
        resultIntent.putExtra("error", error);
        resultIntent.putExtra("command_id", commandId);
        sendBroadcast(resultIntent);

        Toast.makeText(this, "Photo capture failed: " + error, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        stopBackgroundThread();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        super.onDestroy();
    }
}