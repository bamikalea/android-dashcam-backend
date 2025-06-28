# Android Fleet Management System

This folder contains everything related to the custom Android fleet management application and its corresponding backend server.

## Folder Structure

### `android-app/`

Contains the complete Android application source code:

- **Custom Fleet Management APK**: A custom Android application for fleet management
- **Location Tracking**: Real-time GPS location tracking
- **Dashcam Integration**: Integration with dashcam devices
- **Server Communication**: HTTP(S) REST API communication with backend
- **Alibaba OSS Integration**: Cloud storage integration for media files

**Key Components:**

- `app/src/main/java/com/fleetmanagement/custom/` - Main fleet management app
- `app/src/main/java/com/alibaba/sdk/android/oss/` - Alibaba OSS SDK
- `app/src/main/java/carassist/cn/` - Car assistance utilities

### `backend-server/`

**TODO: Add your RESTful backend server here**

- This folder is reserved for the HTTP(S) REST API backend server
- Should implement endpoints matching the APK's ServerConfig.java:
  - `/status`
  - `/location`
  - `/event`
  - `/emergency`
  - `/media`
  - `/photo`
  - `/video`
  - `/heartbeat`

## Communication Protocol

The Android app communicates with the backend using:

- **Protocol**: HTTP(S) REST APIs
- **Base URL**: Configurable in `ServerConfig.java`
- **Authentication**: API key based
- **Data Format**: JSON
- **Real-time**: WebSocket support for live updates

## Setup Instructions

1. **Android App Setup:**

   ```bash
   cd android-app
   ./gradlew build
   ```

2. **Backend Server Setup:**
   ```bash
   cd backend-server
   # Add your backend server setup instructions here
   ```

## Configuration

Update `android-app/app/src/main/java/com/fleetmanagement/custom/config/ServerConfig.java`:

```java
public static final String BASE_URL = "https://your-backend-server.com";
public static final String API_KEY = "your-api-key-here";
public static final String DEVICE_ID = "your-device-id";
```

## Features

- ✅ Real-time location tracking
- ✅ Dashcam status monitoring
- ✅ Media file upload to cloud storage
- ✅ Emergency event handling
- ✅ Heartbeat monitoring
- ✅ HTTP(S) REST API communication
- ✅ Compatible with Render/Heroku deployment

## Deployment

- **Android App**: Build APK and install on Android devices
- **Backend Server**: Deploy to any HTTP(S) capable host (Render, Heroku, AWS, etc.)

## Notes

- This system uses HTTP(S) communication, not raw TCP/UDP
- Compatible with cloud hosting platforms that only support HTTP(S)
- No special port requirements for the backend server
