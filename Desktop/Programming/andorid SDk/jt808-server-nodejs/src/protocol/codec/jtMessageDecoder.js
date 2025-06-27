/**
 * JT808 Message Decoder
 * Equivalent to JTMessageDecoder.java
 */

const crypto = require('crypto');
const { JT808, PROTOCOL_VERSIONS } = require('../../constants/jt808');

class JTMessageDecoder {
  constructor() {
    this.messageRegistry = new Map();
    this.registerMessages();
  }

  /**
   * Register all message types
   */
  registerMessages() {
    // Import message classes
    const T0100 = require('../messages/T0100');
    const T0200 = require('../messages/T0200');
    const T8100 = require('../messages/T8100');

    // Register message types
    this.messageRegistry.set(JT808.TERMINAL_REGISTER, T0100);
    this.messageRegistry.set(JT808.LOCATION_REPORT, T0200);
    this.messageRegistry.set(JT808.TERMINAL_REGISTER_RESPONSE, T8100);
    // Add more message types as needed
  }

  /**
   * Decode buffer to JTMessage
   */
  decode(buffer) {
    try {
      // Unescape the buffer
      const unescapedBuffer = this.unescape(buffer);
      
      // Verify checksum
      const verified = this.verify(unescapedBuffer);
      
      // Parse message header
      const messageId = unescapedBuffer.readUInt16BE(0);
      const properties = unescapedBuffer.readUInt16BE(2);
      
      // Determine protocol version
      let version = PROTOCOL_VERSIONS.V2013; // Default to 2013
      let headerLength = 21; // Default header length
      
      if (this.isBitSet(properties, 14)) {
        // 2019 version
        version = unescapedBuffer.readUInt8(4);
        headerLength = 25;
      }
      
      const isSubPackage = this.isBitSet(properties, 13);
      if (isSubPackage) {
        headerLength += 4; // Add sub-package fields
      }
      
      // Create message instance
      const MessageClass = this.messageRegistry.get(messageId);
      const message = MessageClass ? new MessageClass() : new (require('../jtMessage'))();
      
      message.setVerified(verified);
      message.setPayload(buffer);
      message.setMessageId(messageId);
      message.setMessageBodyProps(properties);
      message.setVersion(version);
      
      // Parse header
      this.parseHeader(unescapedBuffer, message, headerLength);
      
      // Parse body if exists
      if (unescapedBuffer.length > headerLength) {
        const bodyBuffer = unescapedBuffer.slice(headerLength, -1); // Exclude checksum
        this.parseBody(bodyBuffer, message);
      }
      
      return message;
    } catch (error) {
      console.error('Error decoding message:', error);
      throw error;
    }
  }

  /**
   * Parse message header
   */
  parseHeader(buffer, message, headerLength) {
    let offset = 4; // Skip messageId and properties
    
    if (message.getVersion() > 0) {
      offset += 1; // Skip version byte
    }
    
    // Phone number (6 bytes BCD)
    const phoneBytes = buffer.slice(offset, offset + 6);
    const phoneNumber = this.bcdToString(phoneBytes);
    message.setPhoneNumber(phoneNumber);
    offset += 6;
    
    // Flow ID (2 bytes)
    const flowId = buffer.readUInt16BE(offset);
    message.setFlowId(flowId);
    offset += 2;
    
    // Sub-package info if present
    if (message.isSubPackage()) {
      const totalPackage = buffer.readUInt16BE(offset);
      const packageNo = buffer.readUInt16BE(offset + 2);
      message.setTotalPackage(totalPackage);
      message.setPackageNo(packageNo);
      offset += 4;
    }
    
    // Set flags
    const properties = message.getMessageBodyProps();
    message.setSubPackage(this.isBitSet(properties, 13));
    message.setEncrypt(this.isBitSet(properties, 10));
    message.setReserved(this.isBitSet(properties, 9));
  }

  /**
   * Parse message body
   */
  parseBody(buffer, message) {
    if (!buffer || buffer.length === 0) return;
    
    const messageId = message.getMessageId();
    
    switch (messageId) {
      case JT808.TERMINAL_REGISTER:
        this.parseT0100Body(buffer, message);
        break;
      case JT808.LOCATION_REPORT:
        this.parseT0200Body(buffer, message);
        break;
      case JT808.TERMINAL_REGISTER_RESPONSE:
        this.parseT8100Body(buffer, message);
        break;
      default:
        // Store raw body for unknown messages
        message.setMessageBody(buffer);
        break;
    }
  }

  /**
   * Parse T0100 body
   */
  parseT0100Body(buffer, message) {
    let offset = 0;
    
    // Province ID (2 bytes)
    const provinceId = buffer.readUInt16BE(offset);
    message.setProvinceId(provinceId);
    offset += 2;
    
    // City ID (2 bytes)
    const cityId = buffer.readUInt16BE(offset);
    message.setCityId(cityId);
    offset += 2;
    
    // Manufacturer ID (5 bytes)
    const manufacturerId = buffer.slice(offset, offset + 5).toString('ascii').replace(/\0/g, '');
    message.setManufacturerId(manufacturerId);
    offset += 5;
    
    // Terminal Model (20 bytes)
    const terminalModel = buffer.slice(offset, offset + 20).toString('ascii').replace(/\0/g, '');
    message.setTerminalModel(terminalModel);
    offset += 20;
    
    // Terminal ID (7 bytes)
    const terminalId = buffer.slice(offset, offset + 7).toString('ascii').replace(/\0/g, '');
    message.setTerminalId(terminalId);
    offset += 7;
    
    // Plate Color (1 byte)
    const plateColor = buffer.readUInt8(offset);
    message.setPlateColor(plateColor);
    offset += 1;
    
    // Plate Number (variable length)
    const plateNo = buffer.slice(offset).toString('ascii').replace(/\0/g, '');
    message.setPlateNo(plateNo);
  }

  /**
   * Parse T0200 body
   */
  parseT0200Body(buffer, message) {
    let offset = 0;
    
    // Alarm flag (4 bytes)
    const alarmFlag = buffer.readUInt32BE(offset);
    message.setAlarmFlag(alarmFlag);
    offset += 4;
    
    // Status flag (4 bytes)
    const statusFlag = buffer.readUInt32BE(offset);
    message.setStatusFlag(statusFlag);
    offset += 4;
    
    // Latitude (4 bytes)
    const latitude = buffer.readUInt32BE(offset);
    message.setLatitude(latitude);
    offset += 4;
    
    // Longitude (4 bytes)
    const longitude = buffer.readUInt32BE(offset);
    message.setLongitude(longitude);
    offset += 4;
    
    // Altitude (2 bytes)
    const altitude = buffer.readUInt16BE(offset);
    message.setAltitude(altitude);
    offset += 2;
    
    // Speed (2 bytes)
    const speed = buffer.readUInt16BE(offset);
    message.setSpeed(speed);
    offset += 2;
    
    // Direction (2 bytes)
    const direction = buffer.readUInt16BE(offset);
    message.setDirection(direction);
    offset += 2;
    
    // Device time (6 bytes BCD)
    const timeBytes = buffer.slice(offset, offset + 6);
    const deviceTime = this.parseBCDTime(timeBytes);
    message.setDeviceTime(deviceTime);
    offset += 6;
    
    // Parse attributes if remaining data
    if (offset < buffer.length) {
      const attributes = this.parseAttributes(buffer.slice(offset));
      message.setAttributes(attributes);
    }
  }

  /**
   * Parse T8100 body
   */
  parseT8100Body(buffer, message) {
    let offset = 0;
    
    // Response serial number (2 bytes)
    const responseSerialNo = buffer.readUInt16BE(offset);
    message.setResponseSerialNo(responseSerialNo);
    offset += 2;
    
    // Result code (1 byte)
    const resultCode = buffer.readUInt8(offset);
    message.setResultCode(resultCode);
    offset += 1;
    
    // Auth code (variable length)
    if (offset < buffer.length) {
      const authCode = buffer.slice(offset).toString('ascii').replace(/\0/g, '');
      message.setAuthCode(authCode);
    }
  }

  /**
   * Parse attributes
   */
  parseAttributes(buffer) {
    const attributes = new Map();
    let offset = 0;
    
    while (offset < buffer.length) {
      if (offset + 1 >= buffer.length) break;
      
      const id = buffer.readUInt8(offset);
      offset += 1;
      
      if (offset >= buffer.length) break;
      
      const length = buffer.readUInt8(offset);
      offset += 1;
      
      if (offset + length > buffer.length) break;
      
      const value = buffer.slice(offset, offset + length);
      attributes.set(id, value);
      offset += length;
    }
    
    return attributes;
  }

  /**
   * Verify checksum
   */
  verify(buffer) {
    if (buffer.length < 2) return false;
    
    const data = buffer.slice(0, -1); // Exclude checksum
    const expectedChecksum = buffer.readUInt8(buffer.length - 1);
    const calculatedChecksum = this.calculateBCC(data);
    
    return expectedChecksum === calculatedChecksum;
  }

  /**
   * Calculate BCC checksum
   */
  calculateBCC(buffer) {
    let checksum = 0;
    for (let i = 0; i < buffer.length; i++) {
      checksum ^= buffer.readUInt8(i);
    }
    return checksum;
  }

  /**
   * Unescape buffer
   */
  unescape(buffer) {
    const result = [];
    let i = 0;
    
    // Skip start delimiter
    if (buffer.length > 0 && buffer.readUInt8(0) === 0x7E) {
      i = 1;
    }
    
    while (i < buffer.length) {
      const byte = buffer.readUInt8(i);
      
      if (byte === 0x7D && i + 1 < buffer.length) {
        const nextByte = buffer.readUInt8(i + 1);
        if (nextByte === 0x01) {
          result.push(0x7D);
          i += 2;
        } else if (nextByte === 0x02) {
          result.push(0x7E);
          i += 2;
        } else {
          result.push(byte);
          i += 1;
        }
      } else {
        result.push(byte);
        i += 1;
      }
    }
    
    // Skip end delimiter
    if (result.length > 0 && result[result.length - 1] === 0x7E) {
      result.pop();
    }
    
    return Buffer.from(result);
  }

  /**
   * Check if bit is set
   */
  isBitSet(value, bit) {
    return (value & (1 << bit)) !== 0;
  }

  /**
   * Convert BCD to string
   */
  bcdToString(buffer) {
    let result = '';
    for (let i = 0; i < buffer.length; i++) {
      const byte = buffer.readUInt8(i);
      result += Math.floor(byte / 16).toString();
      result += (byte % 16).toString();
    }
    return result;
  }

  /**
   * Parse BCD time
   */
  parseBCDTime(buffer) {
    const timeStr = this.bcdToString(buffer);
    const year = 2000 + parseInt(timeStr.substring(0, 2));
    const month = parseInt(timeStr.substring(2, 4));
    const day = parseInt(timeStr.substring(4, 6));
    const hour = parseInt(timeStr.substring(6, 8));
    const minute = parseInt(timeStr.substring(8, 10));
    const second = parseInt(timeStr.substring(10, 12));
    
    return new Date(year, month - 1, day, hour, minute, second);
  }
}

module.exports = JTMessageDecoder; 