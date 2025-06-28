# ACC Monitoring and Engine Cutoff Capabilities

## Overview

The Android Fleet Management app now includes comprehensive ACC (Accessory) monitoring capabilities that can detect when the vehicle's engine is turned on or off. This document explains the current implementation and addresses the possibility of engine cutoff functionality.

## ACC Monitoring Implementation

### What is ACC?

ACC (Accessory) refers to the vehicle's accessory power system that is activated when the ignition key is turned to the "ACC" position or when the engine is running. The ACC system powers various vehicle accessories like the radio, lights, and in this case, the dashcam device.

### Current ACC Detection Capabilities

The app implements ACC monitoring through the following components:

#### 1. AccMonitoringService

- **Location**: `app/src/main/java/com/fleetmanagement/custom/services/AccMonitoringService.java`
- **Purpose**: Monitors ACC status changes in real-time
- **Features**:
  - Detects ACC ON/OFF events via Android power broadcasts
  - Uses Car SDK's `API.isAccOn()` method for status verification
  - Sends notifications to the server about ACC status changes
  - Provides foreground service with status notification

#### 2. Server Communication Integration

- **New Commands Added**:
  - `getAccStatus` - Get current ACC status
  - `startAccMonitoring` - Start continuous ACC monitoring
  - `stopAccMonitoring` - Stop ACC monitoring
- **Server Endpoints**:
  - `POST /dashcams/{deviceId}/acc-status` - Send ACC status updates

#### 3. Broadcast Receiver

The service registers for these Android system broadcasts:

- `Intent.ACTION_POWER_CONNECTED` - When ACC power is connected
- `Intent.ACTION_POWER_DISCONNECTED` - When ACC power is disconnected
- `Intent.ACTION_BATTERY_CHANGED` - When battery status changes

### ACC Status Detection Method

```java
// From Car SDK API.java
public static boolean isAccOn(Context context) {
    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    Intent batteryStatus = context.registerReceiver(null, ifilter);
    if (batteryStatus != null) {
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        return isCharging;
    }
    return false;
}
```

This method detects ACC status by monitoring the battery charging state, which indicates when the vehicle's electrical system is active.

## ACC Notifications

### Device-Side Notifications

The app provides real-time notifications for ACC status changes:

1. **System Notifications**: Foreground service shows current ACC status
2. **Log Messages**: Detailed logging of all ACC events
3. **Server Communication**: Automatic status updates sent to the fleet management server

### Server-Side Integration

ACC status changes are automatically sent to the server with the following data:

```json
{
  "deviceId": "dashcam-123456",
  "accStatus": "ON|OFF",
  "timestamp": "2024-01-15T10:30:45.123Z"
}
```

## Engine Cutoff Implementation

### Current Status: NOT IMPLEMENTED

**Important**: Engine cutoff functionality is **NOT implemented** in the current version due to:

1. **Safety Regulations**: Engine control systems require automotive-grade safety certifications
2. **Legal Restrictions**: Direct engine control may violate vehicle safety regulations
3. **Hardware Requirements**: Requires specialized vehicle integration hardware
4. **Technical Complexity**: Needs integration with vehicle CAN bus or OBD-II systems

### Theoretical Implementation Requirements

If engine cutoff were to be implemented, it would require:

#### 1. Hardware Integration

- **CAN Bus Interface**: Direct connection to vehicle's Controller Area Network
- **OBD-II Integration**: On-Board Diagnostics interface for engine communication
- **Automotive-Grade Hardware**: Certified components meeting vehicle safety standards

#### 2. Software Requirements

- **Vehicle-Specific Protocols**: Different protocols for different vehicle manufacturers
- **Safety System Integration**: Integration with existing vehicle safety systems
- **Certification**: Automotive software safety certification (ISO 26262)

#### 3. Legal Compliance

- **Safety Standards**: Compliance with automotive safety standards
- **Regulatory Approval**: Government approval for engine control systems
- **Liability Insurance**: Specialized insurance for engine control functionality

### Theoretical Implementation Approach

If the above requirements were met, the implementation might look like:

```java
// THEORETICAL - NOT IMPLEMENTED
private void implementEngineCutoff() {
    // This would require:
    // 1. CAN bus integration
    // 2. OBD-II communication
    // 3. Vehicle-specific protocols
    // 4. Safety system integration (legally restricted)

    Log.w(TAG, "Engine cutoff NOT implemented - requires specialized hardware and legal compliance");
}
```

## Current ACC-Based Capabilities

### What CAN Be Done with ACC Detection

1. **Engine Start Detection**: Detect when the engine is started
2. **Engine Stop Detection**: Detect when the engine is stopped
3. **Power Management**: Optimize device power consumption based on ACC status
4. **Data Collection**: Log engine start/stop events for fleet analytics
5. **Remote Monitoring**: Allow fleet managers to monitor vehicle usage

### ACC-Based Safety Features

1. **Automatic Shutdown**: Device can automatically enter sleep mode when ACC is off
2. **Wake-on-ACC**: Device can wake up when ACC is turned on
3. **Battery Protection**: Prevent device from draining vehicle battery when engine is off
4. **Data Sync**: Sync data when ACC is available

## Usage Examples

### Starting ACC Monitoring

```bash
# Send command to start ACC monitoring
curl -X POST "https://your-server.com/api/dashcams/device123/commands" \
  -H "Content-Type: application/json" \
  -d '{"command": "startAccMonitoring"}'
```

### Getting ACC Status

```bash
# Get current ACC status
curl -X POST "https://your-server.com/api/dashcams/device123/commands" \
  -H "Content-Type: application/json" \
  -d '{"command": "getAccStatus"}'
```

### Server Response

```json
{
  "deviceId": "device123",
  "command": "getAccStatus",
  "success": true,
  "message": "ACC Status: ON",
  "timestamp": "2024-01-15T10:30:45.123Z"
}
```

## Security Considerations

### ACC Monitoring Security

1. **Local Processing**: ACC status is processed locally on the device
2. **Encrypted Communication**: All server communication is encrypted
3. **Access Control**: Server endpoints require proper authentication
4. **Data Privacy**: ACC status data is only shared with authorized fleet management systems

### Engine Cutoff Security (Theoretical)

If engine cutoff were implemented, it would require:

1. **Multi-Factor Authentication**: Multiple authentication methods required
2. **Geofencing**: Engine cutoff only allowed in specific areas
3. **Speed Restrictions**: Engine cutoff only when vehicle is stationary
4. **Emergency Override**: Always allow driver to override cutoff
5. **Audit Logging**: Complete logging of all cutoff events

## Conclusion

The current implementation provides comprehensive ACC monitoring capabilities that can:

✅ **Detect engine start/stop events**
✅ **Send real-time notifications to the server**
✅ **Provide fleet management insights**
✅ **Optimize device power management**

❌ **Engine cutoff is NOT implemented** due to safety, legal, and technical requirements

The ACC monitoring system provides valuable fleet management capabilities while maintaining safety and compliance with automotive regulations. Engine cutoff functionality would require significant additional development, certification, and regulatory approval.

## Future Considerations

If engine cutoff functionality is required in the future, consider:

1. **Partner with Automotive Suppliers**: Work with companies that provide certified engine control systems
2. **Regulatory Compliance**: Ensure compliance with all automotive safety regulations
3. **Safety Certification**: Obtain necessary safety certifications for engine control
4. **Legal Review**: Consult with legal experts on liability and compliance issues
5. **Insurance**: Obtain specialized insurance for engine control functionality
