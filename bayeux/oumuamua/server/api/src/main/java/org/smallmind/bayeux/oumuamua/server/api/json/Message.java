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
package org.smallmind.bayeux.oumuamua.server.api.json;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

/**
 * Bayeux protocol message represented as a JSON object, providing typed accessors for all
 * standard Bayeux fields defined by the protocol specification.
 *
 * @param <V> concrete {@link Value} implementation used within this message
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
   * Returns whether the {@code successful} field is present and set to {@code true}.
   *
   * @return {@code true} if this message represents a successful Bayeux operation
   */
  default boolean isSuccessful () {

    Value<V> value;

    return ((value = get(SUCCESSFUL)) != null) && ValueType.BOOLEAN.equals(value.getType()) && ((BooleanValue<V>)value).asBoolean();
  }

  /**
   * Returns the value of the {@code id} field.
   *
   * @return message id string, or {@code null} if the field is absent or not a string
   */
  default String getId () {

    Value<V> value;

    return (((value = get(ID)) != null) && ValueType.STRING.equals(value.getType())) ? ((StringValue<V>)value).asText() : null;
  }

  /**
   * Returns the value of the {@code clientId} field.
   *
   * @return session id string, or {@code null} if the field is absent or not a string
   */
  default String getSessionId () {

    Value<V> value;

    return (((value = get(SESSION_ID)) != null) && ValueType.STRING.equals(value.getType())) ? ((StringValue<V>)value).asText() : null;
  }

  /**
   * Returns the value of the {@code channel} field.
   *
   * @return channel path string, or {@code null} if the field is absent or not a string
   */
  default String getChannel () {

    Value<V> value;

    return (((value = get(CHANNEL)) != null) && ValueType.STRING.equals(value.getType())) ? ((StringValue<V>)value).asText() : null;
  }

  /**
   * Returns the {@code advice} object field if present, without creating it.
   *
   * @return advice object, or {@code null} if absent
   */
  default ObjectValue<V> getAdvice () {

    return getAdvice(false);
  }

  /**
   * Returns the {@code advice} object field, optionally creating an empty object if absent.
   *
   * @param createIfAbsent {@code true} to create and insert an empty object when the field is missing
   * @return advice object, or {@code null} if absent and {@code createIfAbsent} is {@code false}
   */
  default ObjectValue<V> getAdvice (boolean createIfAbsent) {

    return getOrCreate(ADVICE, createIfAbsent);
  }

  /**
   * Returns the {@code ext} object field if present, without creating it.
   *
   * @return ext object, or {@code null} if absent
   */
  default ObjectValue<V> getExt () {

    return getExt(false);
  }

  /**
   * Returns the {@code ext} object field, optionally creating an empty object if absent.
   *
   * @param createIfAbsent {@code true} to create and insert an empty object when the field is missing
   * @return ext object, or {@code null} if absent and {@code createIfAbsent} is {@code false}
   */
  default ObjectValue<V> getExt (boolean createIfAbsent) {

    return getOrCreate(EXT, createIfAbsent);
  }

  /**
   * Returns the {@code data} object field if present, without creating it.
   *
   * @return data object, or {@code null} if absent
   */
  default ObjectValue<V> getData () {

    return getData(false);
  }

  /**
   * Returns the {@code data} object field, optionally creating an empty object if absent.
   *
   * @param createIfAbsent {@code true} to create and insert an empty object when the field is missing
   * @return data object, or {@code null} if absent and {@code createIfAbsent} is {@code false}
   */
  default ObjectValue<V> getData (boolean createIfAbsent) {

    return getOrCreate(DATA, createIfAbsent);
  }

  /**
   * Serializes this message to a UTF-8 encoded byte array.
   *
   * @return JSON-encoded representation of this message
   * @throws Exception if serialization or I/O fails
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
   * Returns the named field as an {@link ObjectValue}, optionally inserting an empty object
   * if the field is absent.
   *
   * @param field          field name to retrieve or create
   * @param createIfAbsent {@code true} to create and store an empty object when the field is absent
   * @return existing or newly created object value, or {@code null} if absent and not created
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
