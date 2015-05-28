package org.smallmind.throng.wire.mock;

import java.util.Date;
import java.util.HashMap;

public class MockMessageProperties {

  private HashMap<String, Object> headerMap = new HashMap<>();
  private Date timestamp;
  private String contentType;
  private String expiration;
  private String messageId;
  private byte[] correlationId;

  public synchronized void setHeader (String key, Object value) {

    headerMap.put(key, value);
  }

  public synchronized Object getHeader (String key) {

    return headerMap.get(key);
  }

  public synchronized Date getTimestamp () {

    return timestamp;
  }

  public synchronized void setTimestamp (Date timestamp) {

    this.timestamp = timestamp;
  }

  public synchronized String getContentType () {

    return contentType;
  }

  public synchronized void setContentType (String contentType) {

    this.contentType = contentType;
  }

  public synchronized String getExpiration () {

    return expiration;
  }

  public synchronized void setExpiration (String expiration) {

    this.expiration = expiration;
  }

  public synchronized String getMessageId () {

    return messageId;
  }

  public synchronized void setMessageId (String messageId) {

    this.messageId = messageId;
  }

  public synchronized byte[] getCorrelationId () {

    return correlationId;
  }

  public synchronized void setCorrelationId (byte[] correlationId) {

    this.correlationId = correlationId;
  }
}