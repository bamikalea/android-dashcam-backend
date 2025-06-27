/**
 * Base JT808 Message Class
 * Equivalent to JTMessage.java
 */

const { JT808 } = require('../constants/jt808');

class JTMessage {
  constructor() {
    this.messageId = 0;
    this.messageBodyProps = 0;
    this.phoneNumber = '';
    this.flowId = 0;
    this.messageBody = null;
    this.messageBodyLength = 0;
    this.subPackage = false;
    this.encrypt = false;
    this.reserved = false;
    this.version = 0;
    this.totalPackage = 0;
    this.packageNo = 0;
    this.verified = false;
    this.payload = null;
  }

  /**
   * Get message ID
   */
  getMessageId() {
    return this.messageId;
  }

  /**
   * Set message ID
   */
  setMessageId(messageId) {
    this.messageId = messageId;
    return this;
  }

  /**
   * Get message body properties
   */
  getMessageBodyProps() {
    return this.messageBodyProps;
  }

  /**
   * Set message body properties
   */
  setMessageBodyProps(messageBodyProps) {
    this.messageBodyProps = messageBodyProps;
    return this;
  }

  /**
   * Get phone number
   */
  getPhoneNumber() {
    return this.phoneNumber;
  }

  /**
   * Set phone number
   */
  setPhoneNumber(phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

  /**
   * Get flow ID
   */
  getFlowId() {
    return this.flowId;
  }

  /**
   * Set flow ID
   */
  setFlowId(flowId) {
    this.flowId = flowId;
    return this;
  }

  /**
   * Get message body
   */
  getMessageBody() {
    return this.messageBody;
  }

  /**
   * Set message body
   */
  setMessageBody(messageBody) {
    this.messageBody = messageBody;
    return this;
  }

  /**
   * Get message body length
   */
  getMessageBodyLength() {
    return this.messageBodyLength;
  }

  /**
   * Set message body length
   */
  setMessageBodyLength(messageBodyLength) {
    this.messageBodyLength = messageBodyLength;
    return this;
  }

  /**
   * Check if message is sub-package
   */
  isSubPackage() {
    return this.subPackage;
  }

  /**
   * Set sub-package flag
   */
  setSubPackage(subPackage) {
    this.subPackage = subPackage;
    return this;
  }

  /**
   * Check if message is encrypted
   */
  isEncrypt() {
    return this.encrypt;
  }

  /**
   * Set encrypt flag
   */
  setEncrypt(encrypt) {
    this.encrypt = encrypt;
    return this;
  }

  /**
   * Check if message is reserved
   */
  isReserved() {
    return this.reserved;
  }

  /**
   * Set reserved flag
   */
  setReserved(reserved) {
    this.reserved = reserved;
    return this;
  }

  /**
   * Get protocol version
   */
  getVersion() {
    return this.version;
  }

  /**
   * Set protocol version
   */
  setVersion(version) {
    this.version = version;
    return this;
  }

  /**
   * Get total package count
   */
  getTotalPackage() {
    return this.totalPackage;
  }

  /**
   * Set total package count
   */
  setTotalPackage(totalPackage) {
    this.totalPackage = totalPackage;
    return this;
  }

  /**
   * Get package number
   */
  getPackageNo() {
    return this.packageNo;
  }

  /**
   * Set package number
   */
  setPackageNo(packageNo) {
    this.packageNo = packageNo;
    return this;
  }

  /**
   * Check if message is verified
   */
  isVerified() {
    return this.verified;
  }

  /**
   * Set verified flag
   */
  setVerified(verified) {
    this.verified = verified;
    return this;
  }

  /**
   * Get payload
   */
  getPayload() {
    return this.payload;
  }

  /**
   * Set payload
   */
  setPayload(payload) {
    this.payload = payload;
    return this;
  }

  /**
   * Check if message needs buffer
   */
  noBuffer() {
    return false;
  }

  /**
   * Copy message properties from another message
   */
  copyBy(message) {
    this.messageId = message.getMessageId();
    this.messageBodyProps = message.getMessageBodyProps();
    this.phoneNumber = message.getPhoneNumber();
    this.flowId = message.getFlowId();
    this.version = message.getVersion();
    this.totalPackage = message.getTotalPackage();
    this.packageNo = message.getPackageNo();
    return this;
  }

  /**
   * Set message ID and return this
   */
  messageId(messageId) {
    this.messageId = messageId;
    return this;
  }

  /**
   * Build message for sending
   */
  build() {
    return this;
  }

  /**
   * Convert message to string for debugging
   */
  toString() {
    return `JTMessage{
      messageId=0x${this.messageId.toString(16).padStart(4, '0')},
      messageBodyProps=${this.messageBodyProps},
      phoneNumber='${this.phoneNumber}',
      flowId=${this.flowId},
      version=${this.version},
      subPackage=${this.subPackage},
      encrypt=${this.encrypt},
      messageBodyLength=${this.messageBodyLength}
    }`;
  }
}

module.exports = JTMessage; 