# Fleet Management System Setup Guide

This guide will help you set up the complete fleet management system with a Node.js server, web dashboard, and Android app for dashcam control.

## System Overview

The fleet management system consists of:

1. **Node.js Server** - Real-time communication hub
2. **Web Dashboard** - Control interface for monitoring and commanding dashcams
3. **Android App** - Installed on dashcam devices for SDK integration
4. **Socket.IO** - Real-time bidirectional communication

## Prerequisites

- Node.js 16+ installed
- Android Studio (for app development)
- Dashcam device with USB debugging enabled
- Network connectivity between server and dashcams

## Step 1: Server Setup

### 1.1 Install Server Dependencies

```bash
cd server
npm install
```

### 1.2 Configure Server

The server will automatically create a `.env` file with default settings. You can modify it:

```env
# Server Configuration
PORT=3000
NODE_ENV=development

# Logging
LOG_LEVEL=info

# Security
CORS_ORIGIN=*

# Rate Limiting
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=100
```

### 1.3 Start the Server

```bash
# Using the startup script
./start.sh

# Or manually
npm start
```

The server will be available at:

- **Dashboard**: http://localhost:3000
- **API**: http://localhost:3000/api
- **Status**: http://localhost:3000/api/status

## Step 2: Android App Setup

### 2.1 Update Server URL

In `app/src/main/java/com/fleetmanagement/services/ServerCommunicationService.java`, update the server URL:

```java
private static final String SERVER_URL = "http://YOUR_SERVER_IP:3000";
```

Replace `YOUR_SERVER_IP` with your server's IP address.

### 2.2 Build and Install App

```bash
# Build the APK
./gradlew assembleDebug

# Install on connected dashcam
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2.3 Verify Installation

```bash
# Check if app is installed
adb shell pm list packages | grep fleetmanagement

# Check app logs
adb logcat | grep -E "(fleetmanagement|ServerCommunication)"
```

## Step 3: Testing the System

### 3.1 Start the Server

```bash
cd server
./start.sh
```

### 3.2 Access the Dashboard

Open http://localhost:3000 in your browser. You should see:

- Server status indicator
- Connected dashcams list
- Control panel
- Real-time logs
- Event history

### 3.3 Test Dashcam Connection

1. **Connect your dashcam** via USB
2. **Enable USB debugging** on the dashcam
3. **Install the fleet management app** if not already installed
4. **Check the dashboard** - the dashcam should appear in the connected devices list

### 3.4 Test Commands

From the dashboard:

1. Select your dashcam from the dropdown
2. Click any command button (e.g., "Take Photo")
3. Check the real-time logs for command execution
4. Verify the command response

## Step 4: Integration with Dashcam SDK

### 4.1 Update Command Implementations

In `ServerCommunicationService.java`, replace the placeholder implementations with actual SDK calls:

```java
private boolean takePhoto() {
    try {
        // Replace with actual SDK call
        // carMotion.takePhoto();

        JSONObject photoData = new JSONObject();
        photoData.put("timestamp", System.currentTimeMillis());
        photoData.put("file_path", "actual_file_path");

        sendEvent("photo_captured", photoData);
        return true;
    } catch (Exception e) {
        Log.e(TAG, "Error taking photo", e);
        return false;
    }
}
```

### 4.2 Add Event Reporting

In your dashcam event handlers, add server communication:

```java
// In DashcamEventReceiver or similar
private void onMotionDetected() {
    try {
        JSONObject eventData = new JSONObject();
        eventData.put("confidence", 0.95);
        eventData.put("timestamp", System.currentTimeMillis());

        serverCommunicationService.sendEvent("motion_detected", eventData);
    } catch (JSONException e) {
        Log.e(TAG, "Error sending motion event", e);
    }
}
```

## Step 5: Production Deployment

### 5.1 Server Deployment

For production, consider:

- Using a process manager like PM2
- Setting up HTTPS with SSL certificates
- Configuring a reverse proxy (nginx)
- Setting up database persistence
- Implementing authentication

### 5.2 Android App Deployment

For production:

- Sign the APK with your release key
- Configure proper server URLs
- Add error handling and retry logic
- Implement proper security measures

## API Reference

### REST Endpoints

#### GET /api/dashcams

Get all connected dashcams

#### POST /api/dashcams/:deviceId/command

Send command to specific dashcam

```json
{
  "command": "take_photo",
  "parameters": {}
}
```

#### GET /api/events

Get event history with optional filters

#### GET /api/commands

Get command history

#### GET /api/status

Get server status

### Socket.IO Events

#### Client to Server

- `dashcam_register` - Register dashcam
- `dashcam_event` - Report event
- `location_update` - Update location
- `command_response` - Respond to command

#### Server to Client

- `command` - Receive command from server

## Troubleshooting

### Common Issues

1. **Dashcam not connecting**

   - Check network connectivity
   - Verify server IP address
   - Check firewall settings
   - Ensure USB debugging is enabled

2. **Commands not working**

   - Verify dashcam is online in dashboard
   - Check app logs for errors
   - Ensure SDK integration is complete

3. **Server connection issues**
   - Check server is running
   - Verify port 3000 is accessible
   - Check CORS settings

### Log Locations

- **Server logs**: `server/logs/`
- **Android logs**: `adb logcat | grep fleetmanagement`
- **Dashboard logs**: Browser developer console

### Debug Commands

```bash
# Check server status
curl http://localhost:3000/api/status

# Check connected dashcams
curl http://localhost:3000/api/dashcams

# Monitor server logs
tail -f server/logs/combined.log

# Monitor Android app logs
adb logcat | grep -E "(fleetmanagement|ServerCommunication|carassist)"
```

## Security Considerations

1. **Network Security**

   - Use HTTPS in production
   - Implement proper authentication
   - Configure firewall rules

2. **Device Security**

   - Secure the Android app
   - Implement command validation
   - Add rate limiting

3. **Data Protection**
   - Encrypt sensitive data
   - Implement proper logging
   - Regular security audits

## Support

For issues or questions:

1. Check the troubleshooting section
2. Review server and app logs
3. Verify network connectivity
4. Test with a simple command first

## Next Steps

Once the basic system is working:

1. Add more sophisticated dashcam controls
2. Implement video streaming
3. Add analytics and reporting
4. Scale to multiple dashcams
5. Add mobile app for field use
