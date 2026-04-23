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
package org.smallmind.bayeux.oumuamua.server.spi.json.jackson;

import java.io.IOException;
import java.util.Map;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueFactory;
import org.smallmind.bayeux.oumuamua.server.spi.json.JsonDeserializer;
import org.smallmind.nutsnbolts.lang.FormattedIOException;
import org.smallmind.web.json.scaffold.util.JsonCodec;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;

/**
 * Jackson-backed {@link JsonDeserializer} that parses JSON byte buffers and strings into Bayeux
 * {@link Message} arrays and recursively converts {@link JsonNode} trees into the {@link Value} hierarchy.
 *
 * @param <V> the concrete {@link Value} subtype produced during deserialization
 */
public class JaxbDeserializer<V extends Value<V>> implements JsonDeserializer<V> {

  /**
   * Parses a byte buffer into Bayeux messages by first converting it to a {@link JsonNode} tree.
   *
   * @param codec  codec supplying the message factory used to construct each message
   * @param buffer JSON-encoded payload bytes
   * @return array of decoded messages
   * @throws IOException if the bytes cannot be parsed or do not represent an object or array
   */
  @Override
  public Message<V>[] read (Codec<V> codec, byte[] buffer)
    throws IOException {

    return read(codec, JsonCodec.readAsJsonNode(buffer));
  }

  /**
   * Parses a JSON string into Bayeux messages by first converting it to a {@link JsonNode} tree.
   *
   * @param codec codec supplying the message factory used to construct each message
   * @param data  JSON-encoded string
   * @return array of decoded messages
   * @throws IOException if the string cannot be parsed or does not represent an object or array
   */
  @Override
  public Message<V>[] read (Codec<V> codec, String data)
    throws IOException {

    return read(codec, JsonCodec.readAsJsonNode(data));
  }

  /**
   * Constructs messages from an already-parsed {@link JsonNode}, handling both a top-level object
   * (single message) and a top-level array (multiple messages).
   *
   * @param codec codec used to create each message via {@link Codec#create()}
   * @param node  parsed Jackson node representing the incoming payload
   * @return array of one or more messages populated from the node's fields
   * @throws IOException if any array element is not an object, or if the root node type is unsupported
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
   * Converts {@code object} to a {@link Value} by serializing it to a {@link JsonNode} tree first,
   * then walking the tree with {@link #walk}.
   *
   * @param factory factory used to instantiate value nodes
   * @param object  arbitrary object to convert; must be Jackson-serializable
   * @return value tree representing {@code object}
   * @throws IOException if Jackson cannot serialize the object or the resulting node has an unsupported type
   */
  @Override
  public Value<V> convert (ValueFactory<V> factory, Object object)
    throws IOException {

    return walk(factory, JsonCodec.writeAsJsonNode(object));
  }

  /**
   * Recursively converts a {@link JsonNode} into a {@link Value} node using the appropriate
   * {@code factory} method for each JSON type (object, array, string, number, boolean, null).
   *
   * @param factory factory used to create each value node
   * @param node    Jackson node to convert
   * @return the equivalent {@link Value} representation
   * @throws IOException if {@code node} has an unknown or unsupported type, or an unsupported numeric subtype
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
        return switch (node.numberType()) {
          case LONG -> factory.numberValue(node.longValue());
          case INT -> factory.numberValue(node.intValue());
          case DOUBLE -> factory.numberValue(node.doubleValue());
          case FLOAT -> factory.numberValue(node.doubleValue());
          default -> throw new FormattedIOException("Unknown number type(%s)", node.numberType().name());
        };
      case BOOLEAN:
        return factory.booleanValue(node.booleanValue());
      case NULL:
        return factory.nullValue();
      default:
        throw new FormattedIOException("Unknown node type(%s)", node.getNodeType().name());
    }
  }
}
