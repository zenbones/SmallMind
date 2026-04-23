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
package org.smallmind.phalanx.wire.transport;

/**
 * Enumeration of well-known message-header property keys shared across all wire transport implementations.
 *
 * <p>Each constant carries a stable string key suitable for use in transport message headers or metadata maps.</p>
 */
public enum WireProperty {

  /**
   * Identifies the originating caller of a wire message.
   */
  CALLER_ID("callerId"),

  /**
   * Declares the content-type (e.g. MIME type) of the message payload.
   */
  CONTENT_TYPE("contentType"),

  /**
   * Carries a timestamp or logical clock value associated with the message.
   */
  CLOCK("clock"),

  /**
   * Identifies the service group to which the message is addressed.
   */
  SERVICE_GROUP("serviceGroup"),

  /**
   * Carries the specific service instance identifier targeted by the message.
   */
  INSTANCE_ID("instanceId");

  private final String key;

  /**
   * Constructs the constant and binds it to its string property key.
   *
   * @param key the transport-level property name for this constant
   */
  WireProperty (String key) {

    this.key = key;
  }

  /**
   * Returns the string property key for use in message headers or metadata.
   *
   * @return the property key string
   */
  public String getKey () {

    return key;
  }
}
