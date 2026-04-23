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
package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.io.IOException;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueFactory;

/**
 * SPI contract for parsing inbound JSON payloads into Bayeux {@link Message} arrays and for
 * converting arbitrary objects into the {@link Value} type hierarchy used by a given codec.
 *
 * @param <V> the concrete {@link Value} subtype produced by implementations of this interface
 */
public interface JsonDeserializer<V extends Value<V>> {

  /**
   * Parses a raw byte payload into an array of Bayeux messages using the codec's factory.
   *
   * @param codec  codec that supplies the message factory and value creation context
   * @param buffer UTF-8 (or codec-appropriate) encoded JSON payload
   * @return one or more decoded messages; never {@code null} but may be empty
   * @throws IOException if the bytes cannot be parsed or do not represent valid message JSON
   */
  Message<V>[] read (Codec<V> codec, byte[] buffer)
    throws IOException;

  /**
   * Parses a JSON string payload into an array of Bayeux messages using the codec's factory.
   *
   * @param codec codec that supplies the message factory and value creation context
   * @param data  JSON string encoding one object or an array of objects
   * @return one or more decoded messages; never {@code null} but may be empty
   * @throws IOException if the string cannot be parsed or does not represent valid message JSON
   */
  Message<V>[] read (Codec<V> codec, String data)
    throws IOException;

  /**
   * Converts {@code object} into an equivalent {@link Value} using {@code factory} for construction.
   *
   * @param factory factory used to instantiate value nodes during conversion
   * @param object  arbitrary object to convert (typically a POJO or collection)
   * @return value tree representing {@code object}
   * @throws IOException if the object cannot be serialized or contains unsupported types
   */
  Value<V> convert (ValueFactory<V> factory, Object object)
    throws IOException;
}
