/**
 * T8100 - Terminal Registration Response Message
 * Equivalent to T8100.java
 */

const JTMessage = require('../jtMessage');
const { JT808, RESPONSE_CODES } = require('../../constants/jt808');

class T8100 extends JTMessage {
  constructor() {
    super();
    this.messageId = JT808.TERMINAL_REGISTER_RESPONSE;
    this.responseSerialNo = 0;
    this.resultCode = RESPONSE_CODES.SUCCESS;
    this.authCode = '';
  }

  /**
   * Get response serial number
   */
  getResponseSerialNo() {
    return this.responseSerialNo;
  }

  /**
   * Set response serial number
   */
  setResponseSerialNo(responseSerialNo) {
    this.responseSerialNo = responseSerialNo;
    return this;
  }

  /**
   * Get result code
   */
  getResultCode() {
    return this.resultCode;
  }

  /**
   * Set result code
   */
  setResultCode(resultCode) {
    this.resultCode = resultCode;
    return this;
  }

  /**
   * Get auth code
   */
  getAuthCode() {
    return this.authCode;
  }

  /**
   * Set auth code
   */
  setAuthCode(authCode) {
    this.authCode = authCode;
    return this;
  }

  /**
   * Convert to string for debugging
   */
  toString() {
    return `T8100{
      responseSerialNo=${this.responseSerialNo},
      resultCode=${this.resultCode},
      authCode='${this.authCode}'
    }`;
  }
}

// Static constants
T8100.SUCCESS = RESPONSE_CODES.SUCCESS;
T8100.TERMINAL_NOT_FOUND = RESPONSE_CODES.TERMINAL_NOT_FOUND;
T8100.TERMINAL_ALREADY_REGISTERED = RESPONSE_CODES.TERMINAL_ALREADY_REGISTERED;
T8100.AUTH_FAILED = RESPONSE_CODES.AUTH_FAILED;

module.exports = T8100; 