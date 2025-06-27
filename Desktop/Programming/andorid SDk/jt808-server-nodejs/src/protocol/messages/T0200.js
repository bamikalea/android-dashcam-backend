/**
 * T0200 - Location Report Message
 * Equivalent to T0200.java
 */

const JTMessage = require('../jtMessage');
const { JT808 } = require('../../constants/jt808');

class T0200 extends JTMessage {
  constructor() {
    super();
    this.messageId = JT808.LOCATION_REPORT;
    this.alarmFlag = 0;
    this.statusFlag = 0;
    this.latitude = 0;
    this.longitude = 0;
    this.altitude = 0;
    this.speed = 0;
    this.direction = 0;
    this.deviceTime = null;
    this.attributes = new Map();
  }

  /**
   * Get alarm flag
   */
  getAlarmFlag() {
    return this.alarmFlag;
  }

  /**
   * Set alarm flag
   */
  setAlarmFlag(alarmFlag) {
    this.alarmFlag = alarmFlag;
    return this;
  }

  /**
   * Get status flag
   */
  getStatusFlag() {
    return this.statusFlag;
  }

  /**
   * Set status flag
   */
  setStatusFlag(statusFlag) {
    this.statusFlag = statusFlag;
    return this;
  }

  /**
   * Get latitude
   */
  getLatitude() {
    return this.latitude;
  }

  /**
   * Set latitude
   */
  setLatitude(latitude) {
    this.latitude = latitude;
    return this;
  }

  /**
   * Get longitude
   */
  getLongitude() {
    return this.longitude;
  }

  /**
   * Set longitude
   */
  setLongitude(longitude) {
    this.longitude = longitude;
    return this;
  }

  /**
   * Get altitude
   */
  getAltitude() {
    return this.altitude;
  }

  /**
   * Set altitude
   */
  setAltitude(altitude) {
    this.altitude = altitude;
    return this;
  }

  /**
   * Get speed
   */
  getSpeed() {
    return this.speed;
  }

  /**
   * Set speed
   */
  setSpeed(speed) {
    this.speed = speed;
    return this;
  }

  /**
   * Get direction
   */
  getDirection() {
    return this.direction;
  }

  /**
   * Set direction
   */
  setDirection(direction) {
    this.direction = direction;
    return this;
  }

  /**
   * Get device time
   */
  getDeviceTime() {
    return this.deviceTime;
  }

  /**
   * Set device time
   */
  setDeviceTime(deviceTime) {
    this.deviceTime = deviceTime;
    return this;
  }

  /**
   * Get attributes
   */
  getAttributes() {
    return this.attributes;
  }

  /**
   * Set attributes
   */
  setAttributes(attributes) {
    this.attributes = attributes;
    return this;
  }

  /**
   * Add attribute
   */
  addAttribute(key, value) {
    this.attributes.set(key, value);
    return this;
  }

  /**
   * Get attribute
   */
  getAttribute(key) {
    return this.attributes.get(key);
  }

  /**
   * Convert to string for debugging
   */
  toString() {
    return `T0200{
      deviceTime=${this.deviceTime},
      longitude=${this.longitude},
      latitude=${this.latitude},
      altitude=${this.altitude},
      speed=${this.speed},
      direction=${this.direction},
      alarmFlag=${this.alarmFlag.toString(2)},
      statusFlag=${this.statusFlag.toString(2)},
      attributes=${JSON.stringify(Object.fromEntries(this.attributes))}
    }`;
  }
}

module.exports = T0200; 