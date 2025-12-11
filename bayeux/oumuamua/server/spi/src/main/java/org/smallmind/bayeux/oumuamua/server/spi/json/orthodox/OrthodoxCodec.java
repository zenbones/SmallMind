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
 * Codec implementation using the orthodox value model and an injected JSON deserializer.
 */
public class OrthodoxCodec implements Codec<OrthodoxValue> {

  private static final OrthodoxValueFactory FACTORY = new OrthodoxValueFactory();

  private final JsonDeserializer<OrthodoxValue> deserializer;

  /**
   * Creates the codec using the provided deserializer.
   *
   * @param deserializer JSON deserializer to use
   */
  public OrthodoxCodec (JsonDeserializer<OrthodoxValue> deserializer) {

    this.deserializer = deserializer;
  }

  /**
   * @return a new empty message
   */
  @Override
  public Message<OrthodoxValue> create () {

    return new OrthodoxMessage(this, FACTORY);
  }

  /**
   * Parses messages from a byte buffer.
   *
   * @param buffer encoded payload
   * @return decoded messages
   * @throws IOException if parsing fails
   */
  @Override
  public Message<OrthodoxValue>[] from (byte[] buffer)
    throws IOException {

    return deserializer.read(this, buffer);
  }

  /**
   * Parses messages from string data.
   *
   * @param data encoded payload
   * @return decoded messages
   * @throws IOException if parsing fails
   */
  @Override
  public Message<OrthodoxValue>[] from (String data)
    throws IOException {

    return deserializer.read(this, data);
  }

  /**
   * Converts an arbitrary object to a value using the shared factory.
   *
   * @param object object to convert
   * @return converted value
   * @throws IOException if conversion fails
   */
  @Override
  public Value<OrthodoxValue> convert (Object object)
    throws IOException {

    return deserializer.convert(FACTORY, object);
  }
}
