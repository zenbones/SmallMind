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
package org.smallmind.bayeux.oumuamua.server.spi.json.jackson;

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueFactory;
import org.smallmind.bayeux.oumuamua.server.spi.json.JsonDeserializer;
import org.smallmind.nutsnbolts.lang.FormattedIOException;
import org.smallmind.web.json.scaffold.util.JsonCodec;

/**
 * Jackson-based deserializer that converts JSON payloads into Bayeux message/value structures.
 *
 * @param <V> concrete value type produced
 */
public class JaxbDeserializer<V extends Value<V>> implements JsonDeserializer<V> {

  /**
   * Deserializes a byte buffer into messages.
   *
   * @param codec codec providing factories
   * @param buffer encoded payload
   * @return array of messages
   * @throws IOException if parsing fails
   */
  @Override
  public Message<V>[] read (Codec<V> codec, byte[] buffer)
    throws IOException {

    return read(codec, JsonCodec.readAsJsonNode(buffer));
  }

  /**
   * Deserializes string data into messages.
   *
   * @param codec codec providing factories
   * @param data encoded payload
   * @return array of messages
   * @throws IOException if parsing fails
   */
  @Override
  public Message<V>[] read (Codec<V> codec, String data)
    throws IOException {

    return read(codec, JsonCodec.readAsJsonNode(data));
  }

  /**
   * Deserializes the supplied JSON node into message structures.
   *
   * @param codec codec providing factories
   * @param node parsed JSON tree
   * @return array of messages
   * @throws IOException if parsing fails or JSON is not object/array
   */
  private Message<V>[] read (Codec<V> codec, JsonNode node)
    throws IOException {

    switch (node.getNodeType()) {
      case ARRAY:

        Message<V>[] messages = new Message[node.size()];
        int index = 0;

        for (JsonNode item : node) {
          if (!JsonNodeType.OBJECT.equals(item.getNodeType())) {
            throw new IOException("All messages must represent json objects");
          } else {
            messages[index] = codec.create();

            for (Map.Entry<String, JsonNode> propertyEntry : item.properties()) {
              messages[index].put(propertyEntry.getKey(), walk(messages[index].getFactory(), propertyEntry.getValue()));
            }
          }

          index++;
        }

        return messages;
      case OBJECT:

        Message<V> message = codec.create();

        for (Map.Entry<String, JsonNode> propertyEntry : node.properties()) {
          message.put(propertyEntry.getKey(), walk(message.getFactory(), propertyEntry.getValue()));
        }

        return new Message[] {message};
      default:
        throw new IOException("Json data does not represent an object or array");
    }
  }

  /**
   * Converts an arbitrary object into a {@link Value} via JSON serialization.
   *
   * @param factory value factory
   * @param object object to convert
   * @return value representing the object
   * @throws IOException if conversion fails
   */
  @Override
  public Value<V> convert (ValueFactory<V> factory, Object object)
    throws IOException {

    return walk(factory, JsonCodec.writeAsJsonNode(object));
  }

  /**
   * Recursively walks a JSON node to construct a {@link Value} hierarchy.
   *
   * @param factory value factory
   * @param node JSON node to convert
   * @return value representation
   * @throws IOException if encountering unknown node types
   */
  private Value<V> walk (ValueFactory<V> factory, JsonNode node)
    throws IOException {

    switch (node.getNodeType()) {
      case OBJECT:

        ObjectValue<V> objectValue = factory.objectValue();

        for (Map.Entry<String, JsonNode> propertyEntry : node.properties()) {
          objectValue.put(propertyEntry.getKey(), walk(factory, propertyEntry.getValue()));
        }

        return objectValue;
      case ARRAY:

        ArrayValue<V> arrayValue = factory.arrayValue();

        for (JsonNode item : node) {
          arrayValue.add(walk(factory, item));
        }

        return arrayValue;
      case STRING:
        return factory.textValue(node.textValue());
      case NUMBER:
        switch (node.numberType()) {
          case LONG:
            return factory.numberValue(node.longValue());
          case INT:
            return factory.numberValue(node.intValue());
          case DOUBLE:
            return factory.numberValue(node.doubleValue());
          case FLOAT:
            return factory.numberValue(node.doubleValue());
          default:
            throw new FormattedIOException("Unknown number type(%s)", node.numberType().name());
        }
      case BOOLEAN:
        return factory.booleanValue(node.booleanValue());
      case NULL:
        return factory.nullValue();
      default:
        throw new FormattedIOException("Unknown node type(%s)", node.getNodeType().name());
    }
  }
}
