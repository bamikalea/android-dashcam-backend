/**
 * Session Manager
 * Equivalent to SessionManager.java
 */

const EventEmitter = require('events');

class SessionManager extends EventEmitter {
  constructor() {
    super();
    this.sessions = new Map();
    this.phoneToSession = new Map();
    this.terminalToSession = new Map();
  }

  /**
   * Create a new session
   */
  createSession(socket, remoteAddress, remotePort) {
    const sessionId = `${remoteAddress}:${remotePort}`;
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
      flowId: 0
    };

    this.sessions.set(sessionId, session);
    this.emit('sessionCreated', session);
    
    console.log(`Session created: ${sessionId}`);
    return session;
  }

  /**
   * Get session by ID
   */
  getSession(sessionId) {
    return this.sessions.get(sessionId);
  }

  /**
   * Get session by phone number
   */
  getSessionByPhone(phoneNumber) {
    return this.phoneToSession.get(phoneNumber);
  }

  /**
   * Get session by terminal ID
   */
  getSessionByTerminal(terminalId) {
    return this.terminalToSession.get(terminalId);
  }

  /**
   * Register session with device info
   */
  registerSession(session, phoneNumber, terminalId, plateNo, version) {
    session.phoneNumber = phoneNumber;
    session.terminalId = terminalId;
    session.plateNo = plateNo;
    session.version = version;
    session.isRegistered = true;
    session.lastActivity = Date.now();

    this.phoneToSession.set(phoneNumber, session);
    if (terminalId) {
      this.terminalToSession.set(terminalId, session);
    }

    this.emit('sessionRegistered', session);
    console.log(`Session registered: ${session.id}, Phone: ${phoneNumber}, Terminal: ${terminalId}`);
  }

  /**
   * Authenticate session
   */
  authenticateSession(session) {
    session.isAuthenticated = true;
    session.lastActivity = Date.now();
    this.emit('sessionAuthenticated', session);
    console.log(`Session authenticated: ${session.id}`);
  }

  /**
   * Update session activity
   */
  updateActivity(sessionId) {
    const session = this.sessions.get(sessionId);
    if (session) {
      session.lastActivity = Date.now();
    }
  }

  /**
   * Set session attribute
   */
  setAttribute(sessionId, key, value) {
    const session = this.sessions.get(sessionId);
    if (session) {
      session.attributes.set(key, value);
    }
  }

  /**
   * Get session attribute
   */
  getAttribute(sessionId, key) {
    const session = this.sessions.get(sessionId);
    return session ? session.attributes.get(key) : null;
  }

  /**
   * Remove session attribute
   */
  removeAttribute(sessionId, key) {
    const session = this.sessions.get(sessionId);
    if (session) {
      session.attributes.delete(key);
    }
  }

  /**
   * Get next flow ID for session
   */
  getNextFlowId(sessionId) {
    const session = this.sessions.get(sessionId);
    if (session) {
      session.flowId = (session.flowId + 1) % 65536;
      return session.flowId;
    }
    return 0;
  }

  /**
   * Send message to session
   */
  sendMessage(sessionId, message) {
    const session = this.sessions.get(sessionId);
    if (session && session.socket) {
      try {
        session.socket.write(message);
        session.lastActivity = Date.now();
        this.emit('messageSent', session, message);
        return true;
      } catch (error) {
        console.error(`Error sending message to session ${sessionId}:`, error);
        this.emit('messageSendError', session, error);
        return false;
      }
    }
    return false;
  }

  /**
   * Send message by phone number
   */
  sendMessageByPhone(phoneNumber, message) {
    const session = this.phoneToSession.get(phoneNumber);
    if (session) {
      return this.sendMessage(session.id, message);
    }
    return false;
  }

  /**
   * Send message by terminal ID
   */
  sendMessageByTerminal(terminalId, message) {
    const session = this.terminalToSession.get(terminalId);
    if (session) {
      return this.sendMessage(session.id, message);
    }
    return false;
  }

  /**
   * Close session
   */
  closeSession(sessionId) {
    const session = this.sessions.get(sessionId);
    if (session) {
      // Remove from maps
      if (session.phoneNumber) {
        this.phoneToSession.delete(session.phoneNumber);
      }
      if (session.terminalId) {
        this.terminalToSession.delete(session.terminalId);
      }
      
      // Close socket
      if (session.socket) {
        try {
          session.socket.destroy();
        } catch (error) {
          console.error(`Error closing socket for session ${sessionId}:`, error);
        }
      }
      
      // Remove from sessions
      this.sessions.delete(sessionId);
      
      this.emit('sessionClosed', session);
      console.log(`Session closed: ${sessionId}`);
    }
  }

  /**
   * Close all sessions
   */
  closeAllSessions() {
    const sessionIds = Array.from(this.sessions.keys());
    sessionIds.forEach(sessionId => {
      this.closeSession(sessionId);
    });
  }

  /**
   * Get all sessions
   */
  getAllSessions() {
    return Array.from(this.sessions.values());
  }

  /**
   * Get registered sessions
   */
  getRegisteredSessions() {
    return Array.from(this.sessions.values()).filter(session => session.isRegistered);
  }

  /**
   * Get authenticated sessions
   */
  getAuthenticatedSessions() {
    return Array.from(this.sessions.values()).filter(session => session.isAuthenticated);
  }

  /**
   * Get session count
   */
  getSessionCount() {
    return this.sessions.size;
  }

  /**
   * Get registered session count
   */
  getRegisteredSessionCount() {
    return this.getRegisteredSessions().length;
  }

  /**
   * Get authenticated session count
   */
  getAuthenticatedSessionCount() {
    return this.getAuthenticatedSessions().length;
  }

  /**
   * Clean up inactive sessions
   */
  cleanupInactiveSessions(timeoutMs = 300000) { // 5 minutes default
    const now = Date.now();
    const inactiveSessions = Array.from(this.sessions.values()).filter(session => {
      return (now - session.lastActivity) > timeoutMs;
    });

    inactiveSessions.forEach(session => {
      console.log(`Cleaning up inactive session: ${session.id}`);
      this.closeSession(session.id);
    });

    return inactiveSessions.length;
  }

  /**
   * Get session statistics
   */
  getStatistics() {
    return {
      totalSessions: this.getSessionCount(),
      registeredSessions: this.getRegisteredSessionCount(),
      authenticatedSessions: this.getAuthenticatedSessionCount(),
      activeSessions: this.getAuthenticatedSessions().length
    };
  }
}

module.exports = SessionManager; 