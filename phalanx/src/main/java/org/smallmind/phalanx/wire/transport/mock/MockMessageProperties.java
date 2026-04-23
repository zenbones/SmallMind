/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.phalanx.wire.transport.mock;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * Mutable metadata container for a {@link MockMessage}, providing AMQP-style properties for the mock transport.
 */
public class MockMessageProperties {

  private final HashMap<String, Object> headerMap = new HashMap<>();
  private LocalDateTime timestamp;
  private String contentType;
  private String expiration;
  private String messageId;
  private byte[] correlationId;

  /**
   * Adds or replaces a header entry by key.
   *
   * @param key   header name.
   * @param value header value.
   */
  public synchronized void setHeader (String key, Object value) {

    headerMap.put(key, value);
  }

  /**
   * Retrieves a header value by key.
   *
   * @param key header name.
   * @return header value, or {@code null} if the header is absent.
   */
  public synchronized Object getHeader (String key) {

    return headerMap.get(key);
  }

  /**
   * Returns the message creation timestamp.
   *
   * @return message timestamp, or {@code null} if not set.
   */
  public synchronized LocalDateTime getTimestamp () {

    return timestamp;
  }

  /**
   * Sets the message creation timestamp.
   *
   * @param timestamp message creation time.
   */
  public synchronized void setTimestamp (LocalDateTime timestamp) {

    this.timestamp = timestamp;
  }

  /**
   * Returns the MIME content type of the payload.
   *
   * @return content type string, or {@code null} if not set.
   */
  public synchronized String getContentType () {

    return contentType;
  }

  /**
   * Sets the MIME content type of the payload.
   *
   * @param contentType payload content type.
   */
  public synchronized void setContentType (String contentType) {

    this.contentType = contentType;
  }

  /**
   * Returns the expiration value for the message.
   *
   * @return expiration string, or {@code null} if not set.
   */
  public synchronized String getExpiration () {

    return expiration;
  }

  /**
   * Sets the expiration value for the message.
   *
   * @param expiration expiration string.
   */
  public synchronized void setExpiration (String expiration) {

    this.expiration = expiration;
  }

  /**
   * Returns the unique message identifier.
   *
   * @return message id, or {@code null} if not set.
   */
  public synchronized String getMessageId () {

    return messageId;
  }

  /**
   * Sets the unique message identifier.
   *
   * @param messageId message id.
   */
  public synchronized void setMessageId (String messageId) {

    this.messageId = messageId;
  }

  /**
   * Returns the correlation id bytes linking this message to a request.
   *
   * @return correlation id bytes, or {@code null} if not set.
   */
  public synchronized byte[] getCorrelationId () {

    return correlationId;
  }

  /**
   * Sets the correlation id bytes linking this message to a request.
   *
   * @param correlationId correlation id bytes.
   */
  public synchronized void setCorrelationId (byte[] correlationId) {

    this.correlationId = correlationId;
  }
}
