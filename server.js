const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const path = require('path');
const fs = require('fs-extra');
const { v4: uuidv4 } = require('uuid');
const moment = require('moment');
const winston = require('winston');
const multer = require('multer');

// Load environment variables
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

// Configure logging
const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.errors({ stack: true }),
    winston.format.json()
  ),
  defaultMeta: { service: 'fleet-management-server' },
  transports: [
    new winston.transports.File({ filename: 'logs/error.log', level: 'error' }),
    new winston.transports.File({ filename: 'logs/combined.log' }),
    new winston.transports.Console({
      format: winston.format.simple()
    })
  ]
});

// Create logs directory
fs.ensureDirSync('logs');

// Create uploads directory
fs.ensureDirSync('uploads');
fs.ensureDirSync('uploads/thumbnails');

// Configure multer for file uploads
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        const uploadDir = 'uploads/';
        fs.ensureDirSync(uploadDir);
        cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, file.fieldname + '-' + uniqueSuffix + path.extname(file.originalname));
    }
});

const upload = multer({ 
    storage: storage,
    limits: {
        fileSize: 50 * 1024 * 1024 // 50MB limit
    },
    fileFilter: function (req, file, cb) {
        // Accept images, videos, and audio files
        if (file.mimetype.startsWith('image/') || 
            file.mimetype.startsWith('video/') || 
            file.mimetype.startsWith('audio/')) {
            cb(null, true);
        } else {
            cb(new Error('Invalid file type'), false);
        }
    }
});

// Security middleware
app.use(helmet());
app.use(cors({
  origin: process.env.CORS_ORIGIN || '*',
  credentials: true
}));

// Trust proxy for ngrok
app.set('trust proxy', 1);

// Rate limiting - updated for proxy support
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000, // 15 minutes
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP, please try again later.',
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req) => {
    // Use X-Forwarded-For header if available (for ngrok)
    return req.headers['x-forwarded-for'] || req.ip;
  }
});
app.use('/api/', limiter);

// Body parsing middleware
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// CSP header middleware - must be before static and routes!
app.use((req, res, next) => {
  res.setHeader(
    "Content-Security-Policy",
    "default-src 'self'; script-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; img-src 'self' data: https://cdn.jsdelivr.net https://*.tile.openstreetmap.org"
  );
  next();
});

// Serve static files
app.use(express.static(path.join(__dirname, 'public')));

// In-memory storage for dashcam data
const dashcamData = new Map();
const commandHistory = [];
const commandQueue = [];
const eventLog = [];
const mediaFiles = {
    images: [],
    videos: [],
    audio: []
};

// Socket.IO connection handling
io.on('connection', (socket) => {
  logger.info(`Client connected: ${socket.id}`);

  // Handle dashcam registration
  socket.on('dashcam_register', (data) => {
    const { deviceId, deviceInfo } = data;
    dashcamData.set(deviceId, {
      ...deviceInfo,
      socketId: socket.id,
      lastSeen: new Date(),
      status: 'online',
      location: null,
      events: [],
      jt808Enabled: false
    });
    
    logger.info(`Dashcam registered: ${deviceId}`);
    io.emit('dashcam_status', {
      deviceId,
      status: 'online',
      timestamp: new Date()
    });
  });

  // Handle dashcam events
  socket.on('dashcam_event', (data) => {
    const { deviceId, eventType, eventData } = data;
    const dashcam = dashcamData.get(deviceId);
    
    if (dashcam) {
      dashcam.lastSeen = new Date();
      dashcam.events.push({
        type: eventType,
        data: eventData,
        timestamp: new Date()
      });
      
      eventLog.push({
        deviceId,
        eventType,
        eventData,
        timestamp: new Date()
      });
      
      logger.info(`Dashcam event: ${deviceId} - ${eventType}`);
      io.emit('dashcam_event', {
        deviceId,
        eventType,
        eventData,
        timestamp: new Date()
      });
    }
  });

  // Handle location updates
  socket.on('location_update', (data) => {
    const { deviceId, location } = data;
    const dashcam = dashcamData.get(deviceId);
    
    if (dashcam) {
      dashcam.location = location;
      dashcam.lastSeen = new Date();
      
      io.emit('location_update', {
        deviceId,
        location,
        timestamp: new Date()
      });
    }
  });

  // Handle command responses
  socket.on('command_response', (data) => {
    const { commandId, deviceId, response, success } = data;
    
    commandHistory.push({
      commandId,
      deviceId,
      response,
      success,
      timestamp: new Date()
    });
    
    logger.info(`Command response: ${commandId} - ${success ? 'SUCCESS' : 'FAILED'}`);
    io.emit('command_response', {
      commandId,
      deviceId,
      response,
      success,
      timestamp: new Date()
    });
  });

  // Handle heartbeat
  socket.on('heartbeat', (data) => {
    const { deviceId } = data;
    const dashcam = dashcamData.get(deviceId);
    
    if (dashcam) {
      dashcam.lastSeen = new Date();
      logger.debug(`Heartbeat received from: ${deviceId}`);
    }
  });

  // Handle disconnection
  socket.on('disconnect', () => {
    logger.info(`Client disconnected: ${socket.id}`);
    
    // Mark dashcam as offline
    for (const [deviceId, dashcam] of dashcamData.entries()) {
      if (dashcam.socketId === socket.id) {
        dashcam.status = 'offline';
        dashcam.socketId = null;
        
        io.emit('dashcam_status', {
          deviceId,
          status: 'offline',
          timestamp: new Date()
        });
        break;
      }
    }
  });

  // Relay 'get_location' command from dashboard to device
  socket.on('get_location', (data) => {
    const { deviceId } = data;
    const dashcam = dashcamData.get(deviceId);
    if (dashcam && dashcam.socketId) {
      io.to(dashcam.socketId).emit('get_location_request', { requestorSocketId: socket.id });
      logger.info(`Relayed 'get_location' command to device: ${deviceId}`);
    } else {
      socket.emit('location_error', { error: 'Device not connected', deviceId });
    }
  });

  // Device sends location in response to 'get_location_request'
  socket.on('location_response', (data) => {
    const { deviceId, location, requestorSocketId } = data;
    if (requestorSocketId) {
      io.to(requestorSocketId).emit('location_response', { deviceId, location });
      logger.info(`Forwarded location from device ${deviceId} to dashboard`);
    } else {
      // Fallback: broadcast to all dashboards (optional)
      io.emit('location_response', { deviceId, location });
    }
  });
});

// API Routes

// Health check
app.get('/api/status', (req, res) => {
  res.json({
    status: 'online',
    timestamp: new Date(),
    uptime: process.uptime(),
    version: '1.0.0',
    connectedDevices: dashcamData.size,
    totalEvents: eventLog.length
  });
});

// Get all dashcams
app.get('/api/dashcams', (req, res) => {
  logger.info('[DEBUG] GET /api/dashcams called');
  const dashcams = Array.from(dashcamData.entries()).map(([deviceId, data]) => ({
    deviceId,
    status: data.status,
    lastSeen: data.lastSeen,
    location: data.location,
    jt808Enabled: data.jt808Enabled || false,
    model: data.model || 'Unknown',
    version: data.version || 'Unknown'
  }));
  logger.info(`[DEBUG] Returning ${dashcams.length} dashcams`);
  res.json(dashcams);
});

// Get specific dashcam
app.get('/api/dashcams/:deviceId', (req, res) => {
  const { deviceId } = req.params;
  const dashcam = dashcamData.get(deviceId);
  
  if (!dashcam) {
    logger.warn('[DEBUG] 404: Dashcam not found for deviceId ' + deviceId);
    return res.status(404).json({ error: 'Dashcam not found' });
  }
  
  res.json({
    deviceId,
    ...dashcam,
    jt808Data: dashcam.jt808Data || []
  });
});

// Register dashcam
app.post('/api/dashcams/register', (req, res) => {
  logger.info(`[DEBUG] POST /api/dashcams/register body: ${JSON.stringify(req.body)}`);
  const { deviceId, model, version } = req.body;
  if (!deviceId) {
    logger.warn('[DEBUG] 400: Device ID is required');
    return res.status(400).json({ error: 'Device ID is required' });
  }
  
  // Register or update device
  const dashcam = {
    deviceId,
    model: model || 'Unknown',
    version: version || 'Unknown',
    status: 'online',
    lastSeen: new Date(),
    registeredAt: new Date(),
    jt808Enabled: false
  };
  
  dashcamData.set(deviceId, dashcam);
  
  logger.info(`Device registered via HTTP: ${deviceId} (${model} ${version})`);
  res.json({ success: true, message: 'Device registered successfully' });
});

// Update dashcam status
app.post('/api/dashcams/:deviceId/status', (req, res) => {
  const { deviceId } = req.params;
  const { status, batteryLevel, storageAvailable, jt808Enabled } = req.body;
  
  const dashcam = dashcamData.get(deviceId);
  if (!dashcam) {
    return res.status(404).json({ error: 'Dashcam not found' });
  }
  
  dashcam.status = status || dashcam.status;
  dashcam.lastSeen = new Date();
  dashcam.batteryLevel = batteryLevel;
  dashcam.storageAvailable = storageAvailable;
  dashcam.jt808Enabled = jt808Enabled || dashcam.jt808Enabled;
  
  logger.info(`Status update: ${deviceId} - ${status}`);
  res.json({ success: true });
});

// Send command to dashcam
app.post('/api/dashcams/:deviceId/command', (req, res) => {
  logger.info(`[DEBUG] POST /api/dashcams/${req.params.deviceId}/command body: ${JSON.stringify(req.body)}`);
  const { deviceId } = req.params;
  const { command, parameters } = req.body;
  const dashcam = dashcamData.get(deviceId);
  if (!dashcam) {
    logger.warn('[DEBUG] 404: Dashcam not found for deviceId ' + deviceId);
    return res.status(404).json({ error: 'Dashcam not found' });
  }
  // ... rest of the function ...
});

// Dummy events endpoint to prevent dashboard errors
app.get('/api/events', (req, res) => {
  res.json({ events: [] });
});

// Start server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  logger.info(`Fleet Management Server running on port ${PORT}`);
  console.log(`🚗 Fleet Management Server running on http://localhost:${PORT}`);
  console.log(`📊 Dashboard available at: http://localhost:${PORT}`);
  console.log(`🔌 API endpoints available at: http://localhost:${PORT}/api`);
  console.log(`📡 Socket.IO endpoint: http://localhost:${PORT}`);
});