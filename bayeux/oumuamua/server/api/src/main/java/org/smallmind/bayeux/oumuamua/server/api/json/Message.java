/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.bayeux.oumuamua.server.api.json;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

/**
 * Represents a Bayeux JSON message with convenience accessors for standard fields.
 */
public interface Message<V extends Value<V>> extends ObjectValue<V> {

  String VERSION = "version";
  String MINIMUM_VERSION = "minimumVersion";
  String ID = "id";
  String SESSION_ID = "clientId";
  String CHANNEL = "channel";
  String SUCCESSFUL = "successful";
  String ERROR = "error";
  String CONNECTION_TYPE = "connectionType";
  String SUPPORTED_CONNECTION_TYPES = "supportedConnectionTypes";
  String SUBSCRIPTION = "subscription";
  String EXT = "ext";
  String ADVICE = "advice";
  String DATA = "data";

  /**
   * Determines whether the message indicates success.
   *
   * @return {@code true} when the {@code successful} field is present and true
   */
  default boolean isSuccessful () {

    Value<V> value;

    return ((value = get(SUCCESSFUL)) != null) && ValueType.BOOLEAN.equals(value.getType()) && ((BooleanValue<V>)value).asBoolean();
  }

  /**
   * Reads the message identifier.
   *
   * @return the {@code id} field or {@code null} if missing
   */
  default String getId () {

    Value<V> value;

    return (((value = get(ID)) != null) && ValueType.STRING.equals(value.getType())) ? ((StringValue<V>)value).asText() : null;
  }

  /**
   * Reads the client/session identifier.
   *
   * @return the {@code clientId} field or {@code null} if missing
   */
  default String getSessionId () {

    Value<V> value;

    return (((value = get(SESSION_ID)) != null) && ValueType.STRING.equals(value.getType())) ? ((StringValue<V>)value).asText() : null;
  }

  /**
   * Reads the channel path.
   *
   * @return the {@code channel} field or {@code null} if missing
   */
  default String getChannel () {

    Value<V> value;

    return (((value = get(CHANNEL)) != null) && ValueType.STRING.equals(value.getType())) ? ((StringValue<V>)value).asText() : null;
  }

  /**
   * Retrieves the advice object if present.
   *
   * @return advice value or {@code null}
   */
  default ObjectValue<V> getAdvice () {

    return getAdvice(false);
  }

  /**
   * Retrieves or creates the advice object.
   *
   * @param createIfAbsent whether to create the object if missing
   * @return advice value or {@code null}
   */
  default ObjectValue<V> getAdvice (boolean createIfAbsent) {

    return getOrCreate(ADVICE, createIfAbsent);
  }

  /**
   * Retrieves the extension object if present.
   *
   * @return extension value or {@code null}
   */
  default ObjectValue<V> getExt () {

    return getExt(false);
  }

  /**
   * Retrieves or creates the extension object.
   *
   * @param createIfAbsent whether to create the object if missing
   * @return extension value or {@code null}
   */
  default ObjectValue<V> getExt (boolean createIfAbsent) {

    return getOrCreate(EXT, createIfAbsent);
  }

  /**
   * Retrieves the data object if present.
   *
   * @return data value or {@code null}
   */
  default ObjectValue<V> getData () {

    return getData(false);
  }

  /**
   * Retrieves or creates the data object.
   *
   * @param createIfAbsent whether to create the object if missing
   * @return data value or {@code null}
   */
  default ObjectValue<V> getData (boolean createIfAbsent) {

    return getOrCreate(DATA, createIfAbsent);
  }

  /**
   * Encodes the message to a UTF-8 byte array.
   *
   * @return encoded bytes
   * @throws Exception if encoding fails
   */
  default byte[] encode ()
    throws Exception {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream)) {
      encode(outputStreamWriter);
    }

    return outputStream.toByteArray();
  }

  /**
   * Looks up a field as an object value, optionally creating it.
   *
   * @param field          field name
   * @param createIfAbsent whether to create the object if absent
   * @return the object value or {@code null}
   */
  private ObjectValue<V> getOrCreate (String field, boolean createIfAbsent) {

    Value<V> value;

    if (((value = get(field)) != null) && ValueType.OBJECT.equals(value.getType())) {

      return (ObjectValue<V>)value;
    } else if (createIfAbsent) {

      ObjectValue<V> createdValue;

      put(field, createdValue = getFactory().objectValue());

      return createdValue;
    } else {

      return null;
    }
  }
}
