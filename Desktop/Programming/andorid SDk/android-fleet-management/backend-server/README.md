# Fleet Management Server

A Node.js server for real-time communication with dashcam fleet management apps.

## Features

- **Real-time Communication**: Socket.IO for instant dashcam updates
- **REST API**: HTTP endpoints for dashcam control and data retrieval
- **Web Dashboard**: Modern, responsive frontend for monitoring and control
- **Event Logging**: Comprehensive logging of all dashcam events
- **Command History**: Track all commands sent to dashcams
- **Location Tracking**: Real-time GPS location updates
- **Security**: Rate limiting, CORS, and helmet security headers

## Quick Start

### Prerequisites

- Node.js 16+
- npm or yarn

### Installation

1. **Install dependencies:**

   ```bash
   cd server
   npm install
   ```

2. **Start the server:**

   ```bash
   npm start
   ```

3. **Access the dashboard:**
   Open http://localhost:3000 in your browser

### Development Mode

```bash
npm run dev
```

## API Endpoints

### Dashcam Management

#### GET /api/dashcams

Get all connected dashcams

```json
[
  {
    "deviceId": "01453982072143",
    "status": "online",
    "lastSeen": "2025-06-26T23:30:00.000Z",
    "location": {
      "latitude": 37.7749,
      "longitude": -122.4194
    }
  }
]
```

#### GET /api/dashcams/:deviceId

Get specific dashcam details

#### POST /api/dashcams/:deviceId/command

Send command to dashcam

```json
{
  "command": "take_photo",
  "parameters": {}
}
```

### Events & Commands

#### GET /api/events

Get event history

- Query params: `deviceId`, `eventType`, `limit`

#### GET /api/commands

Get command history

- Query params: `deviceId`, `limit`

#### GET /api/status

Get server status

```json
{
  "server": "running",
  "uptime": 3600,
  "dashcams": {
    "total": 5,
    "online": 3,
    "offline": 2
  }
}
```

## Socket.IO Events

### Client to Server

#### dashcam_register

Register a new dashcam

```javascript
socket.emit("dashcam_register", {
  deviceId: "01453982072143",
  deviceInfo: {
    model: "Dashcam Pro",
    version: "1.0.0",
  },
});
```

#### dashcam_event

Report dashcam event

```javascript
socket.emit("dashcam_event", {
  deviceId: "01453982072143",
  eventType: "motion_detected",
  eventData: {
    confidence: 0.95,
    timestamp: "2025-06-26T23:30:00.000Z",
  },
});
```

#### location_update

Update dashcam location

```javascript
socket.emit("location_update", {
  deviceId: "01453982072143",
  location: {
    latitude: 37.7749,
    longitude: -122.4194,
    accuracy: 10,
  },
});
```

#### command_response

Respond to server command

```javascript
socket.emit("command_response", {
  commandId: "uuid-here",
  deviceId: "01453982072143",
  response: "Photo captured successfully",
  success: true,
});
```

### Server to Client

#### command

Receive command from server

```javascript
socket.on("command", (data) => {
  console.log("Received command:", data.command);
  // Execute command and send response
});
```

## Available Commands

| Command           | Description               | Parameters |
| ----------------- | ------------------------- | ---------- |
| `take_photo`      | Capture a photo           | None       |
| `start_recording` | Start video recording     | None       |
| `stop_recording`  | Stop video recording      | None       |
| `get_location`    | Get current GPS location  | None       |
| `reboot`          | Reboot the device         | None       |
| `factory_reset`   | Reset to factory settings | None       |

## Dashboard Features

- **Real-time Monitoring**: Live status of all connected dashcams
- **Command Control**: Send commands to individual dashcams
- **Event History**: View all recorded events with timestamps
- **Location Tracking**: Real-time GPS coordinates
- **Statistics**: Overview of system health and activity

## Configuration

Create a `.env` file in the server directory:

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

## Integration with Android App

The Android fleet management app should:

1. **Connect to server** using Socket.IO
2. **Register device** on startup
3. **Listen for commands** and execute them
4. **Send events** when dashcam events occur
5. **Update location** periodically
6. **Respond to commands** with success/failure status

## Security Considerations

- Rate limiting prevents abuse
- CORS configured for web access
- Helmet security headers enabled
- Input validation on all endpoints
- Logging for audit trails

## Troubleshooting

### Common Issues

1. **Dashcam not connecting**

   - Check network connectivity
   - Verify server is running
   - Check firewall settings

2. **Commands not working**

   - Ensure dashcam is online
   - Check command format
   - Verify device ID

3. **Events not showing**
   - Check event format
   - Verify device registration
   - Check browser console for errors

### Logs

Server logs are stored in:

- `logs/combined.log` - All logs
- `logs/error.log` - Error logs only

## License

MIT License
