# JT808 Alert System Documentation

## Overview

The JT808 Alert System is a comprehensive vehicle telematics solution that implements the Chinese national standard JT808 protocol for vehicle tracking and alert management. This system supports real-time alert monitoring, location tracking, and alert response capabilities.

## System Architecture

### Components

1. **Android Device App** (`com.fleetmanagement.custom`)

   - Implements JT808 protocol client
   - Sends location updates and alerts to server
   - Handles hardware integration (camera, GPS, sensors)

2. **Backend Server** (Node.js/Express)

   - Receives and processes JT808 messages
   - Stores alert and location data
   - Provides REST API for monitoring
   - Real-time WebSocket notifications

3. **JT808 Protocol Server** (Java/Spring Boot)
   - Full JT808 protocol implementation
   - Handles binary protocol messages
   - Supports alert file uploads

## JT808 Alert Bit Definitions

The system uses a 32-bit integer (`warnBit`) to represent different alert types:

| Bit   | Alert Type (English)                              | Alert Type (Chinese)      | Description                                       |
| ----- | ------------------------------------------------- | ------------------------- | ------------------------------------------------- |
| 0     | Emergency alarm                                   | 紧急报警                  | Emergency situation requiring immediate attention |
| 1     | Overspeed alarm                                   | 超速报警                  | Vehicle speed exceeds limit                       |
| 2     | Fatigue driving alarm                             | 疲劳驾驶报警              | Driver fatigue detected                           |
| 3     | Dangerous warning                                 | 危险预警                  | Dangerous situation warning                       |
| 4     | GNSS module failure                               | GNSS 模块发生故障         | GPS module malfunction                            |
| 5     | GNSS antenna disconnected                         | GNSS 天线未接或被剪断     | GPS antenna disconnected                          |
| 6     | GNSS antenna short circuit                        | GNSS 天线短路             | GPS antenna short circuit                         |
| 7     | Terminal main power undervoltage                  | 终端主电源欠压            | Low battery warning                               |
| 8     | Terminal main power off                           | 终端主电源掉电            | Power loss                                        |
| 9     | Terminal LCD/display failure                      | 终端 LCD 或显示屏故障     | Display malfunction                               |
| 10    | TTS module failure                                | TTS 模块故障              | Text-to-speech module failure                     |
| 11    | Camera failure                                    | 摄像头故障                | Camera malfunction                                |
| 12    | Road transport certificate IC card module failure | 道路运输证 IC 卡模块故障  | IC card reader failure                            |
| 13    | Overspeed warning                                 | 超速预警                  | Speed warning (pre-alert)                         |
| 14    | Fatigue driving warning                           | 疲劳驾驶预警              | Fatigue warning (pre-alert)                       |
| 15    | No card inserted alarm                            | 未插卡报警                | No IC card detected                               |
| 16-19 | Reserved                                          | 预留                      | Reserved for future use                           |
| 20    | Area entry/exit alarm                             | 进出区域报警              | Vehicle enters/leaves defined area                |
| 21    | Route entry/exit alarm                            | 进出路线报警              | Vehicle deviates from route                       |
| 22    | Route drive time too short/long                   | 路段行驶时间不足/过长报警 | Route time violation                              |
| 23    | Illegal ignition alarm                            | 车辆非法点火报警          | Unauthorized vehicle start                        |
| 24    | Illegal displacement alarm                        | 车辆非法位移报警          | Unauthorized vehicle movement                     |
| 25-31 | Reserved                                          | 预留                      | Reserved for future use                           |

## API Endpoints

### JT808 Location Updates

```
POST /api/dashcams/{deviceId}/jt808/location
```

**Request Body:**

```json
{
  "latitude": 40.7128,
  "longitude": -74.006,
  "altitude": 100,
  "speed": 60,
  "bearing": 90,
  "warnBit": 0,
  "statusBit": 1,
  "timestamp": "2025-06-28T20:32:30.288Z"
}
```

### JT808 Alert Reports

```
POST /api/dashcams/{deviceId}/jt808/alert
```

**Request Body:**

```json
{
  "alertType": "emergency",
  "warnBit": 1,
  "statusBit": 0,
  "latitude": 40.7128,
  "longitude": -74.006,
  "altitude": 100,
  "speed": 0,
  "description": "Emergency alert description",
  "timestamp": "2025-06-28T20:32:30.288Z"
}
```

### Get JT808 Data

```
GET /api/dashcams/{deviceId}/jt808
```

**Response:**

```json
{
  "deviceId": "13f15b0094dcc44a",
  "jt808Enabled": true,
  "data": [
    {
      "type": "location",
      "data": { ... },
      "timestamp": "2025-06-28T20:32:30.288Z"
    },
    {
      "type": "alert",
      "data": { ... },
      "timestamp": "2025-06-28T20:32:30.288Z"
    }
  ]
}
```

## Testing the Alert System

### 1. Local Testing

Run the test script to verify endpoints:

```bash
cd android-fleet-management/backend-server
npm install axios
node test-jt808-endpoints.js
```

### 2. Manual Testing with curl

**Test Emergency Alert:**

```bash
curl -X POST https://your-server.com/api/dashcams/your-device-id/jt808/alert \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "emergency",
    "warnBit": 1,
    "statusBit": 0,
    "latitude": 40.7128,
    "longitude": -74.0060,
    "description": "Test emergency alert"
  }'
```

**Test Overspeed Alert:**

```bash
curl -X POST https://your-server.com/api/dashcams/your-device-id/jt808/alert \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "overspeed",
    "warnBit": 2,
    "statusBit": 0,
    "latitude": 40.7128,
    "longitude": -74.0060,
    "speed": 120,
    "description": "Vehicle exceeding speed limit"
  }'
```

### 3. Android App Testing

1. **Enable JT808 Mode:**

   - The app automatically enables JT808 when sending location/alert data
   - Check logs for "JT808 enabled" messages

2. **Trigger Test Alerts:**

   - Use the app's test functions to simulate alerts
   - Monitor server logs for incoming alert data

3. **Monitor Real-time:**
   - Use WebSocket connection to receive real-time alerts
   - Check dashboard for alert history

## Alert Monitoring and Response

### Real-time Monitoring

The system provides real-time alert monitoring through:

1. **WebSocket Events:**

   ```javascript
   socket.on("jt808_alert", (data) => {
     console.log("Alert received:", data);
     // Handle alert response
   });
   ```

2. **REST API Queries:**

   ```bash
   # Get recent alerts
   curl https://your-server.com/api/dashcams/your-device-id/jt808
   ```

3. **Dashboard Interface:**
   - Web-based dashboard for monitoring
   - Real-time alert notifications
   - Historical alert data

### Alert Response Workflow

1. **Alert Detection:**

   - Device detects alert condition
   - Sends alert to server via JT808 protocol
   - Server logs and stores alert data

2. **Alert Processing:**

   - Server validates alert data
   - Determines alert severity
   - Triggers appropriate response

3. **Response Actions:**

   - Send notification to operators
   - Log alert for investigation
   - Trigger automated responses (if configured)
   - Update vehicle status

4. **Alert Resolution:**
   - Operator acknowledges alert
   - Investigation and resolution
   - Alert status updated to "resolved"

## Configuration

### Server Configuration

**Environment Variables:**

```bash
# Server settings
PORT=3000
LOG_LEVEL=info

# Security
CORS_ORIGIN=*
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=100

# File upload
MAX_FILE_SIZE=52428800
```

### Android App Configuration

**ServerConfig.java:**

```java
public static final String BASE_URL = "https://your-server.com/api";
public static final String DEVICE_ID = "your-device-id";
public static final int UPLOAD_TIMEOUT = 30000;
```

## Deployment

### Backend Server Deployment

1. **Render.com:**

   ```bash
   # Deploy to Render
   git push origin main
   ```

2. **Manual Deployment:**
   ```bash
   cd android-fleet-management/backend-server/render-deployment
   npm install
   npm start
   ```

### Android App Deployment

1. **Build APK:**

   ```bash
   cd android-fleet-management/android-app
   ./gradlew assembleDebug
   ```

2. **Install on Device:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Troubleshooting

### Common Issues

1. **Alert Endpoint Not Found:**

   - Ensure server is deployed with latest code
   - Check endpoint URL configuration
   - Verify device registration

2. **Alert Data Not Received:**

   - Check network connectivity
   - Verify device ID matches
   - Check server logs for errors

3. **Real-time Notifications Not Working:**
   - Verify WebSocket connection
   - Check client-side event handlers
   - Ensure server supports WebSocket

### Debug Commands

**Check Server Status:**

```bash
curl https://your-server.com/health
```

**Check Device Registration:**

```bash
curl https://your-server.com/api/dashcams
```

**Test Alert Endpoint:**

```bash
curl -X POST https://your-server.com/api/dashcams/test-device/jt808/alert \
  -H "Content-Type: application/json" \
  -d '{"alertType": "test", "warnBit": 1}'
```

## Security Considerations

1. **API Security:**

   - Use HTTPS for all communications
   - Implement rate limiting
   - Validate all input data

2. **Device Security:**

   - Secure device authentication
   - Encrypt sensitive data
   - Regular security updates

3. **Data Privacy:**
   - Comply with data protection regulations
   - Implement data retention policies
   - Secure data storage

## Performance Optimization

1. **Database Optimization:**

   - Index alert and location data
   - Implement data archiving
   - Use connection pooling

2. **Network Optimization:**

   - Compress alert data
   - Implement retry mechanisms
   - Use efficient protocols

3. **Monitoring:**
   - Monitor server performance
   - Track alert processing times
   - Monitor system resources

## Future Enhancements

1. **Advanced Alert Types:**

   - Machine learning-based alert detection
   - Predictive maintenance alerts
   - Driver behavior analysis

2. **Integration Capabilities:**

   - Third-party fleet management systems
   - Emergency response systems
   - Insurance company integration

3. **Analytics and Reporting:**
   - Advanced alert analytics
   - Custom report generation
   - Trend analysis

---

## Support

For technical support or questions about the JT808 Alert System:

1. Check the troubleshooting section above
2. Review server logs for error messages
3. Test endpoints using the provided test scripts
4. Contact the development team for assistance

**Documentation Version:** 1.0  
**Last Updated:** June 28, 2025
