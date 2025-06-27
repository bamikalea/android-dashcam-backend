# Dashcam Fleet Management App

A headless Android application for fleet management using a vendor-provided dashcam SDK.

## Features

- **Background Services**: Fleet management, location tracking, and dashcam event handling
- **Dashcam Integration**: Direct control via vendor SDK (`libcarMotion.so`)
- **Server Communication**: RESTful API integration for fleet data
- **Location Tracking**: GPS-based vehicle location monitoring
- **Event Handling**: Dashcam event detection and processing

## Project Structure

```
app/
├── src/main/java/com/fleetmanagement/
│   ├── models/           # Data models
│   ├── receivers/        # Broadcast receivers
│   ├── services/         # Background services
│   └── viewmodels/       # ViewModels for UI
├── libs/
│   ├── armeabi/
│   │   └── libcarMotion.so  # Dashcam SDK library
│   ├── okhttp-3.4.1.jar     # HTTP client (SDK compatible)
│   └── okio-1.10.0.jar      # I/O library (SDK compatible)
└── res/                  # Resources
```

## Dependencies

- **Android SDK**: API 26+ (Android 8.0+)
- **Dashcam SDK**: `libcarMotion.so` (vendor-provided)
- **Network**: OkHttp 3.4.1, Retrofit 2.9.0
- **Database**: Room 2.5.1
- **Location**: Google Play Services Location 21.0.1

## Building

1. **Sync Project**: In Android Studio, go to `File > Sync Project with Gradle Files`
2. **Build APK**: `Build > Build Bundle(s) / APK(s) > Build APK(s)`
3. **Install**: Transfer APK to dashcam device and install

## Configuration

- **Server URL**: Configure in `ServerConfig.java`
- **Location Updates**: Adjust frequency in `LocationTrackingService.java`
- **Event Handling**: Customize in `DashcamEventReceiver.java`

## Installation on Dashcam

1. Enable "Install from Unknown Sources" in device settings
2. Transfer APK to device storage
3. Install using file manager or ADB
4. Grant necessary permissions (location, storage, etc.)
5. App runs automatically on boot via `BootReceiver`

## Troubleshooting

- **Build Errors**: Ensure all SDK files are in `app/libs/`
- **Runtime Errors**: Check device logs with `adb logcat`
- **Permission Issues**: Verify all required permissions in `AndroidManifest.xml`

## SDK Compatibility

This app uses the vendor-provided dashcam SDK with compatible library versions:

- OkHttp 3.4.1 (not 4.x to maintain SDK compatibility)
- Okio 1.10.0 (not 2.x to maintain SDK compatibility)
- No Kotlin stdlib conflicts (pure Java implementation)

## License

This project is proprietary software. All rights reserved.

---

**Version:** 1.0.0  
**Last Updated:** June 26, 2025  
**Compatibility:** Android 8.1+ (API 26+)
