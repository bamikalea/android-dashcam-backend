/**
 * JT808 Message Encoder
 * Equivalent to JTMessageEncoder.java
 */

const { JT808 } = require('../../constants/jt808');

class JTMessageEncoder {
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
   * Encode JTMessage to buffer
   */
  encode(message) {
    try {
      // Create header buffer
      const headerBuffer = this.encodeHeader(message);
      
      // Create body buffer
      const bodyBuffer = this.encodeBody(message);
      
      // Combine header and body
      const messageBuffer = Buffer.concat([headerBuffer, bodyBuffer]);
      
      // Calculate and add checksum
      const checksum = this.calculateBCC(messageBuffer);
      const bufferWithChecksum = Buffer.concat([messageBuffer, Buffer.from([checksum])]);
      
      // Escape and add delimiters
      const escapedBuffer = this.escape(bufferWithChecksum);
      const finalBuffer = Buffer.concat([Buffer.from([0x7E]), escapedBuffer, Buffer.from([0x7E])]);
      
      return finalBuffer;
    } catch (error) {
      console.error('Error encoding message:', error);
      throw error;
    }
  }

  /**
   * Encode message header
   */
  encodeHeader(message) {
    const messageId = message.getMessageId();
    const phoneNumber = message.getPhoneNumber();
    const flowId = message.getFlowId();
    const version = message.getVersion();
    const isSubPackage = message.isSubPackage();
    const isEncrypt = message.isEncrypt();
    const isReserved = message.isReserved();
    
    // Calculate body length
    const bodyLength = message.getMessageBodyLength() || 0;
    
    // Build properties
    let properties = bodyLength & 0x3FF; // 10 bits for body length
    if (isSubPackage) properties |= (1 << 13);
    if (isEncrypt) properties |= (1 << 10);
    if (isReserved) properties |= (1 << 9);
    if (version > 0) properties |= (1 << 14); // 2019 version flag
    
    // Create header buffer
    let headerSize = 21; // Base header size
    if (version > 0) headerSize += 1; // Version byte
    if (isSubPackage) headerSize += 4; // Sub-package fields
    
    const headerBuffer = Buffer.alloc(headerSize);
    let offset = 0;
    
    // Message ID (2 bytes)
    headerBuffer.writeUInt16BE(messageId, offset);
    offset += 2;
    
    // Properties (2 bytes)
    headerBuffer.writeUInt16BE(properties, offset);
    offset += 2;
    
    // Version (1 byte) - only for 2019
    if (version > 0) {
      headerBuffer.writeUInt8(version, offset);
      offset += 1;
    }
    
    // Phone number (6 bytes BCD)
    const phoneBuffer = this.stringToBCD(phoneNumber, 6);
    phoneBuffer.copy(headerBuffer, offset);
    offset += 6;
    
    // Flow ID (2 bytes)
    headerBuffer.writeUInt16BE(flowId, offset);
    offset += 2;
    
    // Sub-package info if present
    if (isSubPackage) {
      const totalPackage = message.getTotalPackage();
      const packageNo = message.getPackageNo();
      headerBuffer.writeUInt16BE(totalPackage, offset);
      headerBuffer.writeUInt16BE(packageNo, offset + 2);
      offset += 4;
    }
    
    return headerBuffer;
  }

  /**
   * Encode message body
   */
  encodeBody(message) {
    const messageId = message.getMessageId();
    
    switch (messageId) {
      case JT808.TERMINAL_REGISTER:
        return this.encodeT0100Body(message);
      case JT808.LOCATION_REPORT:
        return this.encodeT0200Body(message);
      case JT808.TERMINAL_REGISTER_RESPONSE:
        return this.encodeT8100Body(message);
      default:
        // Return raw body if exists
        return message.getMessageBody() || Buffer.alloc(0);
    }
  }

  /**
   * Encode T0100 body
   */
  encodeT0100Body(message) {
    const provinceId = message.getProvinceId();
    const cityId = message.getCityId();
    const manufacturerId = message.getManufacturerId();
    const terminalModel = message.getTerminalModel();
    const terminalId = message.getTerminalId();
    const plateColor = message.getPlateColor();
    const plateNo = message.getPlateNo();
    
    // Calculate total size
    const size = 2 + 2 + 5 + 20 + 7 + 1 + plateNo.length;
    const buffer = Buffer.alloc(size);
    let offset = 0;
    
    // Province ID (2 bytes)
    buffer.writeUInt16BE(provinceId, offset);
    offset += 2;
    
    // City ID (2 bytes)
    buffer.writeUInt16BE(cityId, offset);
    offset += 2;
    
    // Manufacturer ID (5 bytes)
    const manufacturerBuffer = Buffer.alloc(5);
    Buffer.from(manufacturerId, 'ascii').copy(manufacturerBuffer);
    manufacturerBuffer.copy(buffer, offset);
    offset += 5;
    
    // Terminal Model (20 bytes)
    const modelBuffer = Buffer.alloc(20);
    Buffer.from(terminalModel, 'ascii').copy(modelBuffer);
    modelBuffer.copy(buffer, offset);
    offset += 20;
    
    // Terminal ID (7 bytes)
    const terminalBuffer = Buffer.alloc(7);
    Buffer.from(terminalId, 'ascii').copy(terminalBuffer);
    terminalBuffer.copy(buffer, offset);
    offset += 7;
    
    // Plate Color (1 byte)
    buffer.writeUInt8(plateColor, offset);
    offset += 1;
    
    // Plate Number (variable length)
    Buffer.from(plateNo, 'ascii').copy(buffer, offset);
    
    return buffer;
  }

  /**
   * Encode T0200 body
   */
  encodeT0200Body(message) {
    const alarmFlag = message.getAlarmFlag();
    const statusFlag = message.getStatusFlag();
    const latitude = message.getLatitude();
    const longitude = message.getLongitude();
    const altitude = message.getAltitude();
    const speed = message.getSpeed();
    const direction = message.getDirection();
    const deviceTime = message.getDeviceTime();
    const attributes = message.getAttributes();
    
    // Calculate base size
    let size = 4 + 4 + 4 + 4 + 2 + 2 + 2 + 6; // Fixed fields
    if (attributes && attributes.size > 0) {
      // Add attributes size
      for (const [id, value] of attributes) {
        size += 1 + 1 + value.length; // id + length + value
      }
    }
    
    const buffer = Buffer.alloc(size);
    let offset = 0;
    
    // Alarm flag (4 bytes)
    buffer.writeUInt32BE(alarmFlag, offset);
    offset += 4;
    
    // Status flag (4 bytes)
    buffer.writeUInt32BE(statusFlag, offset);
    offset += 4;
    
    // Latitude (4 bytes)
    buffer.writeUInt32BE(latitude, offset);
    offset += 4;
    
    // Longitude (4 bytes)
    buffer.writeUInt32BE(longitude, offset);
    offset += 4;
    
    // Altitude (2 bytes)
    buffer.writeUInt16BE(altitude, offset);
    offset += 2;
    
    // Speed (2 bytes)
    buffer.writeUInt16BE(speed, offset);
    offset += 2;
    
    // Direction (2 bytes)
    buffer.writeUInt16BE(direction, offset);
    offset += 2;
    
    // Device time (6 bytes BCD)
    const timeBuffer = this.dateToBCD(deviceTime);
    timeBuffer.copy(buffer, offset);
    offset += 6;
    
    // Attributes
    if (attributes && attributes.size > 0) {
      for (const [id, value] of attributes) {
        buffer.writeUInt8(id, offset);
        offset += 1;
        buffer.writeUInt8(value.length, offset);
        offset += 1;
        if (Buffer.isBuffer(value)) {
          value.copy(buffer, offset);
        } else {
          Buffer.from(value).copy(buffer, offset);
        }
        offset += value.length;
      }
    }
    
    return buffer;
  }

  /**
   * Encode T8100 body
   */
  encodeT8100Body(message) {
    const responseSerialNo = message.getResponseSerialNo();
    const resultCode = message.getResultCode();
    const authCode = message.getAuthCode();
    
    const size = 2 + 1 + authCode.length;
    const buffer = Buffer.alloc(size);
    let offset = 0;
    
    // Response serial number (2 bytes)
    buffer.writeUInt16BE(responseSerialNo, offset);
    offset += 2;
    
    // Result code (1 byte)
    buffer.writeUInt8(resultCode, offset);
    offset += 1;
    
    // Auth code (variable length)
    Buffer.from(authCode, 'ascii').copy(buffer, offset);
    
    return buffer;
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
   * Escape buffer
   */
  escape(buffer) {
    const result = [];
    
    for (let i = 0; i < buffer.length; i++) {
      const byte = buffer.readUInt8(i);
      
      if (byte === 0x7E) {
        result.push(0x7D, 0x02);
      } else if (byte === 0x7D) {
        result.push(0x7D, 0x01);
      } else {
        result.push(byte);
      }
    }
    
    return Buffer.from(result);
  }

  /**
   * Convert string to BCD
   */
  stringToBCD(str, length) {
    const buffer = Buffer.alloc(length);
    const paddedStr = str.padStart(length * 2, '0');
    
    for (let i = 0; i < length; i++) {
      const high = parseInt(paddedStr[i * 2], 10);
      const low = parseInt(paddedStr[i * 2 + 1], 10);
      buffer.writeUInt8((high << 4) | low, i);
    }
    
    return buffer;
  }

  /**
   * Convert date to BCD
   */
  dateToBCD(date) {
    const year = date.getFullYear() % 100;
    const month = date.getMonth() + 1;
    const day = date.getDate();
    const hour = date.getHours();
    const minute = date.getMinutes();
    const second = date.getSeconds();
    
    const timeStr = `${year.toString().padStart(2, '0')}${month.toString().padStart(2, '0')}${day.toString().padStart(2, '0')}${hour.toString().padStart(2, '0')}${minute.toString().padStart(2, '0')}${second.toString().padStart(2, '0')}`;
    
    return this.stringToBCD(timeStr, 6);
  }
}

module.exports = JTMessageEncoder; 