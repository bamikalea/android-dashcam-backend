/**
 * T0100 - Terminal Registration Message
 * Equivalent to T0100.java
 */

const JTMessage = require('../jtMessage');
const { JT808 } = require('../../constants/jt808');

class T0100 extends JTMessage {
  constructor() {
    super();
    this.messageId = JT808.TERMINAL_REGISTER;
    this.provinceId = 0;
    this.cityId = 0;
    this.manufacturerId = '';
    this.terminalModel = '';
    this.terminalId = '';
    this.plateColor = 0;
    this.plateNo = '';
  }

  /**
   * Get province ID
   */
  getProvinceId() {
    return this.provinceId;
  }

  /**
   * Set province ID
   */
  setProvinceId(provinceId) {
    this.provinceId = provinceId;
    return this;
  }

  /**
   * Get city ID
   */
  getCityId() {
    return this.cityId;
  }

  /**
   * Set city ID
   */
  setCityId(cityId) {
    this.cityId = cityId;
    return this;
  }

  /**
   * Get manufacturer ID
   */
  getManufacturerId() {
    return this.manufacturerId;
  }

  /**
   * Set manufacturer ID
   */
  setManufacturerId(manufacturerId) {
    this.manufacturerId = manufacturerId;
    return this;
  }

  /**
   * Get terminal model
   */
  getTerminalModel() {
    return this.terminalModel;
  }

  /**
   * Set terminal model
   */
  setTerminalModel(terminalModel) {
    this.terminalModel = terminalModel;
    return this;
  }

  /**
   * Get terminal ID
   */
  getTerminalId() {
    return this.terminalId;
  }

  /**
   * Set terminal ID
   */
  setTerminalId(terminalId) {
    this.terminalId = terminalId;
    return this;
  }

  /**
   * Get plate color
   */
  getPlateColor() {
    return this.plateColor;
  }

  /**
   * Set plate color
   */
  setPlateColor(plateColor) {
    this.plateColor = plateColor;
    return this;
  }

  /**
   * Get plate number
   */
  getPlateNo() {
    return this.plateNo;
  }

  /**
   * Set plate number
   */
  setPlateNo(plateNo) {
    this.plateNo = plateNo;
    return this;
  }

  /**
   * Convert to string for debugging
   */
  toString() {
    return `T0100{
      provinceId=${this.provinceId},
      cityId=${this.cityId},
      manufacturerId='${this.manufacturerId}',
      terminalModel='${this.terminalModel}',
      terminalId='${this.terminalId}',
      plateColor=${this.plateColor},
      plateNo='${this.plateNo}'
    }`;
  }
}

module.exports = T0100; 