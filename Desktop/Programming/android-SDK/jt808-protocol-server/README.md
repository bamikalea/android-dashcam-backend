# JT808 Protocol Server & Dashboard

This folder contains everything related to the JT808 protocol server and its web dashboard for managing dashcam devices.

## Folder Structure

### `jt808-server/`

Contains the complete JT808 protocol server implementation:

- **Java Spring Boot Server**: Full JT808/JT1078 protocol implementation
- **Protocol Support**: JT808-2011, JT808-2013, JT808-2019, JT1078-2016
- **Device Management**: Real-time device connection management
- **Message Handling**: Complete JT808 message encoding/decoding
- **Database**: H2 database for device and session management
- **File Storage**: Media file and alarm file storage

**Key Components:**

- `jtt808-server/src/main/java/org/yzh/web/controller/` - REST API controllers
- `jtt808-server/src/main/java/org/yzh/web/endpoint/` - JT808 protocol endpoints
- `jtt808-server/src/main/java/org/yzh/protocol/` - Protocol implementation
- `jtt808-server/src/main/resources/` - Configuration files

**REST API Endpoints:**

- `GET /device/all` - All online device sessions
- `GET /device/option` - All device information
- `POST /device/raw` - Send raw JT808 messages
- `POST /device/{messageId}` - Send specific JT808 commands
- `GET /device/sse` - Server-sent events for real-time updates

### `dashboard/`

Contains the React web dashboard:

- **React Application**: Modern web interface for device management
- **Real-time Monitoring**: Live device status and location tracking
- **Command Interface**: Send JT808 commands to devices
- **Device Management**: View and manage connected devices
- **Responsive Design**: Works on desktop and mobile

**Key Features:**

- Real-time device status monitoring
- JT808 command sending interface
- Device location mapping
- Message history viewing
- Responsive web design

## Communication Protocol

The JT808 server handles:

- **Protocol**: JT808/JT1078 binary protocol over TCP/UDP
- **Port**: 7100 (configurable)
- **Authentication**: Device registration and authentication
- **Message Types**: Location reports, multimedia uploads, commands, etc.
- **Real-time**: WebSocket and Server-Sent Events for live updates

## Setup Instructions

### 1. JT808 Server Setup

```bash
cd jt808-server
# Build the project
./gradlew build

# Run the server
java -jar jtt808-server/build/libs/jtt808-server.jar
```

### 2. Dashboard Setup

```bash
cd dashboard
# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

## Configuration

### Server Configuration

Update `jt808-server/jtt808-server/src/main/resources/application.yml`:

```yaml
server:
  port: 8100

jt-server:
  jt808:
    port:
      tcp: 7100
      udp: 7100
    media-file:
      path: ./data/media_file
    alarm-file:
      port: 7200
```

### Dashboard Configuration

Update `dashboard/src/config.js`:

```javascript
const config = {
  backendUrl: "http://localhost:8100",
  // Add other configuration options
};
```

## Features

### JT808 Server

- ✅ Complete JT808/JT1078 protocol support
- ✅ Device registration and authentication
- ✅ Real-time location tracking
- ✅ Multimedia file handling
- ✅ Command sending and response handling
- ✅ Session management
- ✅ REST API for web integration
- ✅ WebSocket/SSE for real-time updates

### Dashboard

- ✅ Real-time device monitoring
- ✅ Interactive command interface
- ✅ Device location mapping
- ✅ Message history and logs
- ✅ Responsive web design
- ✅ Device status indicators

## Deployment

### Server Deployment

- **Local**: Run with `java -jar`
- **Cloud**: Deploy to VPS with public IP (requires port 7100 access)
- **Docker**: Use provided Dockerfile
- **Render**: Limited to HTTP(S) only (no raw TCP/UDP)

### Dashboard Deployment

- **Development**: `npm start`
- **Production**: `npm run build` and serve static files
- **Cloud**: Deploy to any static hosting (Netlify, Vercel, etc.)

## Important Notes

⚠️ **Deployment Limitations:**

- **Render/Heroku**: Only supports HTTP(S), cannot expose port 7100 for raw JT808 protocol
- **VPS Required**: For full JT808 protocol support, deploy to VPS with public IP
- **Port Requirements**: JT808 devices need direct TCP/UDP access to port 7100

## Protocol Support

- **JT808-2011**: Basic location and command protocol
- **JT808-2013**: Enhanced with multimedia support
- **JT808-2019**: Latest protocol version
- **JT1078-2016**: Video surveillance protocol
- **JSATL12**: Alarm file upload protocol

## File Structure

```
jt808-protocol-server/
├── jt808-server/           # Java JT808 server
│   ├── jtt808-server/      # Main server application
│   ├── jtt808-protocol/    # Protocol implementation
│   ├── commons/           # Common utilities
│   └── 协议文档/          # Protocol documentation
├── dashboard/              # React web dashboard
│   ├── src/               # React source code
│   ├── public/            # Static assets
│   └── package.json       # Dependencies
├── jt808.xml             # Device configuration template
├── automate_dashcam.sh   # Device setup script
└── README.md             # This file
```
