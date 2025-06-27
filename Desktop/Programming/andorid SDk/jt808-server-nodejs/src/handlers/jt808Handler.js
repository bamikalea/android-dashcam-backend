/**
 * JT808 Message Handler
 * Equivalent to JT808Endpoint.java
 */

const { JT808, RESPONSE_CODES } = require('../constants/jt808');
const T8100 = require('../protocol/messages/T8100');
const JTMessageEncoder = require('../protocol/codec/jtMessageEncoder');

class JT808Handler {
  constructor(sessionManager, messageEncoder) {
    this.sessionManager = sessionManager;
    this.messageEncoder = messageEncoder || new JTMessageEncoder();
  }

  /**
   * Handle incoming message
   */
  handleMessage(message, session) {
    const messageId = message.getMessageId();
    
    try {
      switch (messageId) {
        case JT808.TERMINAL_GENERAL_RESPONSE:
          return this.handleT0001(message, session);
          
        case JT808.TERMINAL_HEARTBEAT:
          return this.handleT0002(message, session);
          
        case JT808.TERMINAL_LOGOUT:
          return this.handleT0003(message, session);
          
        case JT808.QUERY_SERVER_TIME:
          return this.handleT0004(message, session);
          
        case JT808.TERMINAL_RESEND_PACKET_REQUEST:
          return this.handleT8003(message, session);
          
        case JT808.TERMINAL_REGISTER:
          return this.handleT0100(message, session);
          
        case JT808.TERMINAL_AUTH:
          return this.handleT0102(message, session);
          
        case JT808.QUERY_TERMINAL_PARAM_RESPONSE:
          return this.handleT0104(message, session);
          
        case JT808.QUERY_TERMINAL_ATTRIBUTE_RESPONSE:
          return this.handleT0107(message, session);
          
        case JT808.TERMINAL_UPGRADE_RESULT_NOTIFY:
          return this.handleT0108(message, session);
          
        case JT808.LOCATION_REPORT:
          return this.handleT0200(message, session);
          
        case JT808.LOCATION_QUERY_RESPONSE:
          return this.handleT0201_0500(message, session);
          
        case JT808.EVENT_REPORT:
          return this.handleT0301(message, session);
          
        case JT808.QUESTION_RESPONSE:
          return this.handleT0302(message, session);
          
        case JT808.INFO_SUBSCRIPTION_CANCEL:
          return this.handleT0303(message, session);
          
        case JT808.VEHICLE_CONTROL_RESPONSE:
          return this.handleT0201_0500(message, session);
          
        case JT808.QUERY_AREA_LINE_DATA_RESPONSE:
          return this.handleT0608(message, session);
          
        case JT808.DRIVING_RECORD_UPLOAD:
          return this.handleT0700(message, session);
          
        case JT808.ELECTRONIC_WAYBILL_REPORT:
          return this.handleT0701(message, session);
          
        case JT808.DRIVER_IDENTITY_COLLECTION_REPORT:
          return this.handleT0702(message, session);
          
        case JT808.LOCATION_BATCH_UPLOAD:
          return this.handleT0704(message, session);
          
        case JT808.CAN_BUS_DATA_UPLOAD:
          return this.handleT0705(message, session);
          
        case JT808.MULTIMEDIA_EVENT_UPLOAD:
          return this.handleT0800(message, session);
          
        case JT808.MULTIMEDIA_DATA_UPLOAD:
          return this.handleT0801(message, session);
          
        case JT808.STORAGE_MULTIMEDIA_SEARCH_RESPONSE:
          return this.handleT0802(message, session);
          
        case JT808.CAMERA_SHOOT_RESPONSE:
          return this.handleT0805(message, session);
          
        case JT808.DATA_TRANSPARENT_UPLOAD:
          return this.handleT0900(message, session);
          
        case JT808.DATA_COMPRESSION_UPLOAD:
          return this.handleT0901(message, session);
          
        case JT808.TERMINAL_RSA_PUBLIC_KEY:
          return this.handleT0A00(message, session);
          
        default:
          console.log(`Unknown message type: 0x${messageId.toString(16).padStart(4, '0')}`);
          return null;
      }
    } catch (error) {
      console.error(`Error handling message 0x${messageId.toString(16).padStart(4, '0')}:`, error);
      return null;
    }
  }

  /**
   * Handle terminal general response (T0001)
   */
  handleT0001(message, session) {
    console.log('Received terminal general response');
    // Handle response if needed
    return null;
  }

  /**
   * Handle terminal heartbeat (T0002)
   */
  handleT0002(message, session) {
    console.log('Received terminal heartbeat');
    this.sessionManager.updateActivity(session.id);
    return null;
  }

  /**
   * Handle terminal logout (T0003)
   */
  handleT0003(message, session) {
    console.log('Received terminal logout');
    this.sessionManager.closeSession(session.id);
    return null;
  }

  /**
   * Handle query server time (T0004)
   */
  handleT0004(message, session) {
    console.log('Received query server time');
    // Create T8004 response
    const response = {
      messageId: JT808.QUERY_SERVER_TIME_RESPONSE,
      serverTime: new Date()
    };
    return response;
  }

  /**
   * Handle terminal resend packet request (T8003)
   */
  handleT8003(message, session) {
    console.log('Received terminal resend packet request');
    // Handle resend request
    return null;
  }

  /**
   * Handle terminal register (T0100)
   */
  handleT0100(message, session) {
    console.log('Received terminal register');
    
    // Register session
    const phoneNumber = message.getPhoneNumber();
    const terminalId = message.getTerminalId();
    const plateNo = message.getPlateNo();
    const version = message.getVersion();
    
    this.sessionManager.registerSession(session, phoneNumber, terminalId, plateNo, version);
    
    // Create T8100 response
    const response = new T8100();
    response.setResponseSerialNo(message.getFlowId());
    response.setResultCode(RESPONSE_CODES.SUCCESS);
    response.setAuthCode(`${terminalId},${plateNo}`);
    
    // Set response properties
    response.setPhoneNumber(phoneNumber);
    response.setFlowId(this.sessionManager.getNextFlowId(session.id));
    
    return response;
  }

  /**
   * Handle terminal auth (T0102)
   */
  handleT0102(message, session) {
    console.log('Received terminal auth');
    
    const phoneNumber = message.getPhoneNumber();
    const authCode = message.getAuthCode();
    
    // Parse auth code (terminalId,plateNo)
    const parts = authCode.split(',');
    const terminalId = parts[0];
    const plateNo = parts.length > 1 ? parts[1] : '';
    
    // Register session
    this.sessionManager.registerSession(session, phoneNumber, terminalId, plateNo, message.getVersion());
    this.sessionManager.authenticateSession(session);
    
    // Create T0001 response
    const response = {
      messageId: JT808.PLATFORM_GENERAL_RESPONSE,
      responseSerialNo: message.getFlowId(),
      responseMessageId: message.getMessageId(),
      resultCode: RESPONSE_CODES.SUCCESS
    };
    
    return response;
  }

  /**
   * Handle query terminal param response (T0104)
   */
  handleT0104(message, session) {
    console.log('Received query terminal param response');
    // Handle response if needed
    return null;
  }

  /**
   * Handle query terminal attribute response (T0107)
   */
  handleT0107(message, session) {
    console.log('Received query terminal attribute response');
    // Handle response if needed
    return null;
  }

  /**
   * Handle terminal upgrade result notify (T0108)
   */
  handleT0108(message, session) {
    console.log('Received terminal upgrade result notify');
    // Handle upgrade result
    return null;
  }

  /**
   * Handle location report (T0200)
   */
  handleT0200(message, session) {
    console.log('Received location report');
    
    // Update session activity
    this.sessionManager.updateActivity(session.id);
    
    // Store location data (implement database storage)
    const locationData = {
      sessionId: session.id,
      phoneNumber: session.phoneNumber,
      terminalId: session.terminalId,
      plateNo: session.plateNo,
      latitude: message.getLatitude(),
      longitude: message.getLongitude(),
      altitude: message.getAltitude(),
      speed: message.getSpeed(),
      direction: message.getDirection(),
      alarmFlag: message.getAlarmFlag(),
      statusFlag: message.getStatusFlag(),
      deviceTime: message.getDeviceTime(),
      serverTime: new Date(),
      attributes: Object.fromEntries(message.getAttributes())
    };
    
    // Emit location event for external processing
    this.sessionManager.emit('locationReport', locationData);
    
    return null;
  }

  /**
   * Handle location query response / vehicle control response (T0201/0500)
   */
  handleT0201_0500(message, session) {
    console.log('Received location query response / vehicle control response');
    // Handle response if needed
    return null;
  }

  /**
   * Handle event report (T0301)
   */
  handleT0301(message, session) {
    console.log('Received event report');
    // Handle event report
    return null;
  }

  /**
   * Handle question response (T0302)
   */
  handleT0302(message, session) {
    console.log('Received question response');
    // Handle question response
    return null;
  }

  /**
   * Handle info subscription cancel (T0303)
   */
  handleT0303(message, session) {
    console.log('Received info subscription cancel');
    // Handle subscription cancel
    return null;
  }

  /**
   * Handle query area line data response (T0608)
   */
  handleT0608(message, session) {
    console.log('Received query area line data response');
    // Handle response if needed
    return null;
  }

  /**
   * Handle driving record upload (T0700)
   */
  handleT0700(message, session) {
    console.log('Received driving record upload');
    // Handle driving record
    return null;
  }

  /**
   * Handle electronic waybill report (T0701)
   */
  handleT0701(message, session) {
    console.log('Received electronic waybill report');
    // Handle waybill report
    return null;
  }

  /**
   * Handle driver identity collection report (T0702)
   */
  handleT0702(message, session) {
    console.log('Received driver identity collection report');
    // Handle driver identity
    return null;
  }

  /**
   * Handle location batch upload (T0704)
   */
  handleT0704(message, session) {
    console.log('Received location batch upload');
    // Handle batch location data
    return null;
  }

  /**
   * Handle CAN bus data upload (T0705)
   */
  handleT0705(message, session) {
    console.log('Received CAN bus data upload');
    // Handle CAN bus data
    return null;
  }

  /**
   * Handle multimedia event upload (T0800)
   */
  handleT0800(message, session) {
    console.log('Received multimedia event upload');
    // Handle multimedia event
    return null;
  }

  /**
   * Handle multimedia data upload (T0801)
   */
  handleT0801(message, session) {
    console.log('Received multimedia data upload');
    // Handle multimedia data
    return null;
  }

  /**
   * Handle storage multimedia search response (T0802)
   */
  handleT0802(message, session) {
    console.log('Received storage multimedia search response');
    // Handle response if needed
    return null;
  }

  /**
   * Handle camera shoot response (T0805)
   */
  handleT0805(message, session) {
    console.log('Received camera shoot response');
    // Handle response if needed
    return null;
  }

  /**
   * Handle data transparent upload (T0900)
   */
  handleT0900(message, session) {
    console.log('Received data transparent upload');
    // Handle transparent data
    return null;
  }

  /**
   * Handle data compression upload (T0901)
   */
  handleT0901(message, session) {
    console.log('Received data compression upload');
    // Handle compressed data
    return null;
  }

  /**
   * Handle terminal RSA public key (T0A00)
   */
  handleT0A00(message, session) {
    console.log('Received terminal RSA public key');
    // Handle RSA public key
    return null;
  }
}

module.exports = JT808Handler; 