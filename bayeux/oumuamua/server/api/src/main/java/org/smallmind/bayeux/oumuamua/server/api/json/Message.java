/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

  default String getId () {

    Value<V> value;

    return (((value = get(ID)) != null) && ValueType.STRING.equals(value.getType())) ? ((StringValue<V>)value).asText() : null;
  }

  default String getSessionId () {

    Value<V> value;

    return (((value = get(SESSION_ID)) != null) && ValueType.STRING.equals(value.getType())) ? ((StringValue<V>)value).asText() : null;
  }

  default String getChannel () {

    Value<V> value;

    return (((value = get(CHANNEL)) != null) && ValueType.STRING.equals(value.getType())) ? ((StringValue<V>)value).asText() : null;
  }

  default ObjectValue<V> getAdvice () {

    return getAdvice(false);
  }

  default ObjectValue<V> getAdvice (boolean createIfAbsent) {

    return getOrCreate(ADVICE, createIfAbsent);
  }

  default ObjectValue<V> getExt () {

    return getExt(false);
  }

  default ObjectValue<V> getExt (boolean createIfAbsent) {

    return getOrCreate(EXT, createIfAbsent);
  }

  default ObjectValue<V> getData () {

    return getData(false);
  }

  default ObjectValue<V> getData (boolean createIfAbsent) {

    return getOrCreate(DATA, createIfAbsent);
  }

  default byte[] encode ()
    throws Exception {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream)) {
      encode(outputStreamWriter);
    }

    return outputStream.toByteArray();
  }

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
