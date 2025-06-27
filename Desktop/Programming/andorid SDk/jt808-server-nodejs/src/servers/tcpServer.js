/**
 * TCP Server for JT808 Protocol
 * Equivalent to JTConfig.java TCP server
 */

const net = require('net');
const EventEmitter = require('events');
const JTMessageDecoder = require('../protocol/codec/jtMessageDecoder');
const JT808Handler = require('../handlers/jt808Handler');

class TCPServer extends EventEmitter {
  constructor(config = {}) {
    super();
    this.config = {
      port: 7100,
      host: '0.0.0.0',
      maxConnections: 1000,
      idleTimeout: 180000, // 3 minutes
      ...config
    };
    
    this.server = null;
    this.connections = new Map();
    this.messageDecoder = new JTMessageDecoder();
    this.handler = null;
    this.isRunning = false;
  }

  /**
   * Set message handler
   */
  setHandler(handler) {
    this.handler = handler;
  }

  /**
   * Start TCP server
   */
  start() {
    if (this.isRunning) {
      console.log('TCP server is already running');
      return;
    }

    this.server = net.createServer((socket) => {
      this.handleConnection(socket);
    });

    this.server.on('error', (error) => {
      console.error('TCP server error:', error);
      this.emit('error', error);
    });

    this.server.on('close', () => {
      console.log('TCP server closed');
      this.isRunning = false;
      this.emit('close');
    });

    this.server.listen(this.config.port, this.config.host, () => {
      console.log(`TCP server listening on ${this.config.host}:${this.config.port}`);
      this.isRunning = true;
      this.emit('listening');
    });

    // Set connection limit
    this.server.maxConnections = this.config.maxConnections;
  }

  /**
   * Stop TCP server
   */
  stop() {
    if (!this.isRunning) {
      console.log('TCP server is not running');
      return;
    }

    // Close all connections
    this.connections.forEach((connection, sessionId) => {
      this.closeConnection(sessionId);
    });

    // Close server
    if (this.server) {
      this.server.close(() => {
        console.log('TCP server stopped');
        this.isRunning = false;
        this.emit('stopped');
      });
    }
  }

  /**
   * Handle new connection
   */
  handleConnection(socket) {
    const remoteAddress = socket.remoteAddress;
    const remotePort = socket.remotePort;
    const sessionId = `${remoteAddress}:${remotePort}`;

    console.log(`New TCP connection from ${sessionId}`);

    // Create session
    const session = {
      id: sessionId,
      socket: socket,
      remoteAddress: remoteAddress,
      remotePort: remotePort,
      phoneNumber: null,
      terminalId: null,
      plateNo: null,
      version: 0,
      attributes: new Map(),
      lastActivity: Date.now(),
      isRegistered: false,
      isAuthenticated: false,
      flowId: 0,
      buffer: Buffer.alloc(0)
    };

    // Store connection
    this.connections.set(sessionId, session);

    // Set socket options
    socket.setKeepAlive(true, 60000); // 1 minute
    socket.setNoDelay(true);

    // Handle data
    socket.on('data', (data) => {
      this.handleData(session, data);
    });

    // Handle close
    socket.on('close', () => {
      this.handleClose(sessionId);
    });

    // Handle error
    socket.on('error', (error) => {
      console.error(`Socket error for ${sessionId}:`, error);
      this.handleClose(sessionId);
    });

    // Handle timeout
    socket.on('timeout', () => {
      console.log(`Socket timeout for ${sessionId}`);
      this.handleClose(sessionId);
    });

    this.emit('connection', session);
  }

  /**
   * Handle incoming data
   */
  handleData(session, data) {
    try {
      // Update activity
      session.lastActivity = Date.now();

      // Append to buffer
      session.buffer = Buffer.concat([session.buffer, data]);

      // Process complete messages
      this.processMessages(session);
    } catch (error) {
      console.error(`Error handling data for ${session.id}:`, error);
      this.handleClose(session.id);
    }
  }

  /**
   * Process complete messages from buffer
   */
  processMessages(session) {
    let startIndex = 0;
    let endIndex = 0;

    while (true) {
      // Find start delimiter
      startIndex = session.buffer.indexOf(0x7E, startIndex);
      if (startIndex === -1) {
        // No start delimiter found, keep remaining data
        session.buffer = session.buffer.slice(endIndex);
        break;
      }

      // Find end delimiter
      endIndex = session.buffer.indexOf(0x7E, startIndex + 1);
      if (endIndex === -1) {
        // No end delimiter found, keep remaining data
        session.buffer = session.buffer.slice(startIndex);
        break;
      }

      // Extract complete message
      const messageBuffer = session.buffer.slice(startIndex, endIndex + 1);
      
      try {
        // Decode message
        const message = this.messageDecoder.decode(messageBuffer);
        
        if (message && message.isVerified()) {
          // Handle message
          this.handleMessage(session, message);
        } else {
          console.warn(`Invalid message from ${session.id}`);
        }
      } catch (error) {
        console.error(`Error decoding message from ${session.id}:`, error);
      }

      // Move to next message
      startIndex = endIndex + 1;
      endIndex = startIndex;
    }
  }

  /**
   * Handle decoded message
   */
  handleMessage(session, message) {
    try {
      if (this.handler) {
        const response = this.handler.handleMessage(message, session);
        
        if (response) {
          // Encode and send response
          const responseBuffer = this.messageDecoder.messageEncoder.encode(response);
          this.sendMessage(session.id, responseBuffer);
        }
      }

      this.emit('message', session, message);
    } catch (error) {
      console.error(`Error handling message for ${session.id}:`, error);
    }
  }

  /**
   * Send message to session
   */
  sendMessage(sessionId, message) {
    const session = this.connections.get(sessionId);
    if (session && session.socket) {
      try {
        session.socket.write(message);
        session.lastActivity = Date.now();
        this.emit('messageSent', session, message);
        return true;
      } catch (error) {
        console.error(`Error sending message to ${sessionId}:`, error);
        this.handleClose(sessionId);
        return false;
      }
    }
    return false;
  }

  /**
   * Send message by phone number
   */
  sendMessageByPhone(phoneNumber, message) {
    for (const [sessionId, session] of this.connections) {
      if (session.phoneNumber === phoneNumber) {
        return this.sendMessage(sessionId, message);
      }
    }
    return false;
  }

  /**
   * Send message by terminal ID
   */
  sendMessageByTerminal(terminalId, message) {
    for (const [sessionId, session] of this.connections) {
      if (session.terminalId === terminalId) {
        return this.sendMessage(sessionId, message);
      }
    }
    return false;
  }

  /**
   * Handle connection close
   */
  handleClose(sessionId) {
    const session = this.connections.get(sessionId);
    if (session) {
      console.log(`TCP connection closed: ${sessionId}`);
      
      // Close socket
      if (session.socket) {
        try {
          session.socket.destroy();
        } catch (error) {
          console.error(`Error destroying socket for ${sessionId}:`, error);
        }
      }

      // Remove from connections
      this.connections.delete(sessionId);
      
      this.emit('disconnection', session);
    }
  }

  /**
   * Close specific connection
   */
  closeConnection(sessionId) {
    const session = this.connections.get(sessionId);
    if (session && session.socket) {
      session.socket.destroy();
    }
  }

  /**
   * Close all connections
   */
  closeAllConnections() {
    this.connections.forEach((session, sessionId) => {
      this.closeConnection(sessionId);
    });
  }

  /**
   * Get connection count
   */
  getConnectionCount() {
    return this.connections.size;
  }

  /**
   * Get all connections
   */
  getAllConnections() {
    return Array.from(this.connections.values());
  }

  /**
   * Get registered connections
   */
  getRegisteredConnections() {
    return Array.from(this.connections.values()).filter(session => session.isRegistered);
  }

  /**
   * Get authenticated connections
   */
  getAuthenticatedConnections() {
    return Array.from(this.connections.values()).filter(session => session.isAuthenticated);
  }

  /**
   * Clean up inactive connections
   */
  cleanupInactiveConnections(timeoutMs = 300000) { // 5 minutes default
    const now = Date.now();
    const inactiveSessions = Array.from(this.connections.values()).filter(session => {
      return (now - session.lastActivity) > timeoutMs;
    });

    inactiveSessions.forEach(session => {
      console.log(`Cleaning up inactive connection: ${session.id}`);
      this.closeConnection(session.id);
    });

    return inactiveSessions.length;
  }

  /**
   * Get server statistics
   */
  getStatistics() {
    return {
      isRunning: this.isRunning,
      port: this.config.port,
      host: this.config.host,
      totalConnections: this.getConnectionCount(),
      registeredConnections: this.getRegisteredConnections().length,
      authenticatedConnections: this.getAuthenticatedConnections().length
    };
  }
}

module.exports = TCPServer; 