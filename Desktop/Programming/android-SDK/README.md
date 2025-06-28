# Android Dashcam & Fleet Management System

This project has been reorganized into two main components for better clarity and separation of concerns.

## 📁 Project Structure

```
andorid SDk/
├── android-fleet-management/     # Custom Android app + REST backend
│   ├── android-app/             # Custom fleet management APK
│   ├── backend-server/          # RESTful HTTP(S) backend (TODO)
│   └── README.md               # Fleet management documentation
│
├── jt808-protocol-server/       # JT808 protocol server + dashboard
│   ├── jt808-server/           # Java JT808 protocol server
│   ├── dashboard/              # React web dashboard
│   └── README.md               # JT808 protocol documentation
│
└── README.md                   # This file
```

## 🚀 Quick Start

### Option 1: Custom Android Fleet Management

```bash
cd android-fleet-management
# See android-fleet-management/README.md for detailed instructions
```

### Option 2: JT808 Protocol Server & Dashboard

```bash
cd jt808-protocol-server
# See jt808-protocol-server/README.md for detailed instructions
```

## 📋 System Overview

### 1. Android Fleet Management System

- **Purpose**: Custom Android app for fleet management with HTTP(S) communication
- **Communication**: REST APIs over HTTP(S)
- **Deployment**: Compatible with Render, Heroku, AWS, etc.
- **Features**: Location tracking, dashcam integration, cloud storage

### 2. JT808 Protocol Server & Dashboard

- **Purpose**: Full JT808/JT1078 protocol implementation for hardware dashcams
- **Communication**: Raw TCP/UDP protocol (port 7100)
- **Deployment**: Requires VPS with public IP access
- **Features**: Protocol compliance, device management, web dashboard

## 🔄 Communication Comparison

| Aspect               | Android Fleet Management | JT808 Protocol Server |
| -------------------- | ------------------------ | --------------------- |
| **Protocol**         | HTTP(S) REST APIs        | Raw TCP/UDP (JT808)   |
| **Port**             | 80/443 (HTTP/HTTPS)      | 7100 (TCP/UDP)        |
| **Deployment**       | Any HTTP host            | VPS with public IP    |
| **Devices**          | Android phones/tablets   | Hardware dashcams     |
| **Real-time**        | WebSocket/SSE            | Native protocol       |
| **Cloud Compatible** | ✅ Yes                   | ❌ Limited            |

## 🎯 Use Cases

### Choose Android Fleet Management if:

- You want to use Android devices as dashcams
- You prefer HTTP(S) communication
- You want cloud deployment (Render, Heroku, etc.)
- You need REST API integration
- You want easier setup and maintenance

### Choose JT808 Protocol Server if:

- You have hardware JT808 dashcam devices
- You need full protocol compliance
- You have access to a VPS with public IP
- You need advanced device management
- You want protocol-level control

## 🛠️ Development

### Prerequisites

- **Android Fleet Management**: Android Studio, Java 8+, Node.js
- **JT808 Protocol Server**: Java 11+, Maven/Gradle, Node.js

### Building

```bash
# Android Fleet Management
cd android-fleet-management/android-app
./gradlew build

# JT808 Protocol Server
cd jt808-protocol-server/jt808-server
./gradlew build

# Dashboard
cd jt808-protocol-server/dashboard
npm install && npm run build
```

## 📚 Documentation

- [Android Fleet Management Guide](android-fleet-management/README.md)
- [JT808 Protocol Server Guide](jt808-protocol-server/README.md)
- [Fleet Management Setup](android-fleet-management/FLEET_MANAGEMENT_SETUP.md)

## 🔧 Configuration

### Android Fleet Management

Update `android-fleet-management/android-app/app/src/main/java/com/fleetmanagement/custom/config/ServerConfig.java`:

```java
public static final String BASE_URL = "https://your-backend-server.com";
```

### JT808 Protocol Server

Update `jt808-protocol-server/jt808-server/jtt808-server/src/main/resources/application.yml`:

```yaml
jt-server:
  jt808:
    port:
      tcp: 7100
      udp: 7100
```

## 🚨 Important Notes

1. **Deployment Limitations**: JT808 protocol requires VPS with public IP access
2. **Protocol Compatibility**: Hardware dashcams use JT808, Android apps use HTTP(S)
3. **Cloud Hosting**: Only Android fleet management works with Render/Heroku
4. **Port Requirements**: JT808 needs port 7100, HTTP(S) uses standard ports

## 🤝 Contributing

Each system is self-contained. Choose the appropriate folder based on your needs:

- For Android app development: `android-fleet-management/`
- For JT808 protocol work: `jt808-protocol-server/`

## 📄 License

This project contains multiple components. See individual README files for specific licensing information.

---

**Version:** 1.0.0  
**Last Updated:** June 26, 2025  
**Compatibility:** Android 8.1+ (API 26+)
