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
package org.smallmind.bayeux.oumuamua.server.spi.json.jackson;

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Codec;
import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;
import org.smallmind.bayeux.oumuamua.server.spi.json.JsonDeserializer;
import org.smallmind.nutsnbolts.lang.FormattedIOException;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class JaxbDeserializer<V extends Value<V>> implements JsonDeserializer<V> {

  @Override
  public Message<V>[] read (Codec<V> codec, byte[] buffer)
    throws IOException {

    return read(codec, JsonCodec.readAsJsonNode(buffer));
  }

  @Override
  public Message<V>[] read (Codec<V> codec, String data)
    throws IOException {

    return read(codec, JsonCodec.readAsJsonNode(data));
  }

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

            for (Map.Entry<String, JsonNode> fieldEntry : new IterableIterator<>(node.fields())) {
              messages[index].put(fieldEntry.getKey(), walk(messages[index].getFactory(), fieldEntry.getValue()));
            }

            index++;
          }
        }

        return messages;
      case OBJECT:

        Message<V> message = codec.create();

        for (Map.Entry<String, JsonNode> fieldEntry : new IterableIterator<>(node.fields())) {
          message.put(fieldEntry.getKey(), walk(message.getFactory(), fieldEntry.getValue()));
        }

        return new Message[] {message};
      default:
        throw new IOException("Json data does not represent an object or array");
    }
  }

  @Override
  public Value<V> convert (ValueFactory<V> factory, Object object)
    throws IOException {

    return walk(factory, JsonCodec.writeAsJsonNode(object));
  }

  private Value<V> walk (ValueFactory<V> factory, JsonNode node)
    throws IOException {

    switch (node.getNodeType()) {
      case OBJECT:

        ObjectValue<V> objectValue = factory.objectValue();

        for (Map.Entry<String, JsonNode> fieldEntry : new IterableIterator<>(node.fields())) {
          objectValue.put(fieldEntry.getKey(), walk(factory, fieldEntry.getValue()));
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
