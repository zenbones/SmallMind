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

import java.util.Date;
import java.util.HashMap;

/**
 * Stores metadata for a {@link MockMessage}, mimicking AMQP-like properties.
 */
public class MockMessageProperties {

  private final HashMap<String, Object> headerMap = new HashMap<>();
  private Date timestamp;
  private String contentType;
  private String expiration;
  private String messageId;
  private byte[] correlationId;

  /**
   * Adds or replaces a header value.
   *
   * @param key   header name.
   * @param value header value.
   */
  public synchronized void setHeader (String key, Object value) {

    headerMap.put(key, value);
  }

  /**
   * Retrieves the value for the specified header.
   *
   * @param key header name.
   * @return header value or {@code null} if missing.
   */
  public synchronized Object getHeader (String key) {

    return headerMap.get(key);
  }

  /**
   * @return message timestamp.
   */
  public synchronized Date getTimestamp () {

    return timestamp;
  }

  /**
   * @param timestamp timestamp to assign.
   */
  public synchronized void setTimestamp (Date timestamp) {

    this.timestamp = timestamp;
  }

  /**
   * @return content type associated with the payload.
   */
  public synchronized String getContentType () {

    return contentType;
  }

  /**
   * @param contentType payload content type.
   */
  public synchronized void setContentType (String contentType) {

    this.contentType = contentType;
  }

  /**
   * @return expiration value.
   */
  public synchronized String getExpiration () {

    return expiration;
  }

  /**
   * @param expiration expiration value to set.
   */
  public synchronized void setExpiration (String expiration) {

    this.expiration = expiration;
  }

  /**
   * @return message id.
   */
  public synchronized String getMessageId () {

    return messageId;
  }

  /**
   * @param messageId message id to set.
   */
  public synchronized void setMessageId (String messageId) {

    this.messageId = messageId;
  }

  /**
   * @return correlation id as bytes.
   */
  public synchronized byte[] getCorrelationId () {

    return correlationId;
  }

  /**
   * @param correlationId correlation id bytes.
   */
  public synchronized void setCorrelationId (byte[] correlationId) {

    this.correlationId = correlationId;
  }
}
