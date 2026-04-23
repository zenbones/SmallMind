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
package org.smallmind.bayeux.oumuamua.server.spi.json.orthodox;

import java.io.IOException;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.json.JsonDeserializer;

/**
 * {@link Codec} implementation for the orthodox value model, delegating JSON parsing and object
 * conversion to an injected {@link JsonDeserializer} and using a shared {@link OrthodoxValueFactory}.
 */
public class OrthodoxCodec implements Codec<OrthodoxValue> {

  private static final OrthodoxValueFactory FACTORY = new OrthodoxValueFactory();

  private final JsonDeserializer<OrthodoxValue> deserializer;

  /**
   * Constructs the codec with the given deserializer for all inbound JSON parsing.
   *
   * @param deserializer the {@link JsonDeserializer} used to decode byte and string payloads
   */
  public OrthodoxCodec (JsonDeserializer<OrthodoxValue> deserializer) {

    this.deserializer = deserializer;
  }

  /**
   * Allocates and returns a new, empty {@link OrthodoxMessage} backed by the shared factory.
   *
   * @return fresh empty message ready to be populated
   */
  @Override
  public Message<OrthodoxValue> create () {

    return new OrthodoxMessage(this, FACTORY);
  }

  /**
   * Parses one or more messages from a raw byte payload via the injected deserializer.
   *
   * @param buffer JSON-encoded payload bytes
   * @return array of decoded messages
   * @throws IOException if the payload cannot be parsed
   */
  @Override
  public Message<OrthodoxValue>[] from (byte[] buffer)
    throws IOException {

    return deserializer.read(this, buffer);
  }

  /**
   * Parses one or more messages from a JSON string payload via the injected deserializer.
   *
   * @param data JSON-encoded string
   * @return array of decoded messages
   * @throws IOException if the string cannot be parsed
   */
  @Override
  public Message<OrthodoxValue>[] from (String data)
    throws IOException {

    return deserializer.read(this, data);
  }

  /**
   * Converts {@code object} to an {@link OrthodoxValue} tree using the shared factory and the
   * injected deserializer.
   *
   * @param object arbitrary object to convert; must be serializable by the configured deserializer
   * @return value tree representing {@code object}
   * @throws IOException if conversion fails
   */
  @Override
  public Value<OrthodoxValue> convert (Object object)
    throws IOException {

    return deserializer.convert(FACTORY, object);
  }
}
