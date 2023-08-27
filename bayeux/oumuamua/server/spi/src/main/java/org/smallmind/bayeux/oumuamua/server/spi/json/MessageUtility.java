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
package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Codec;
import org.smallmind.bayeux.oumuamua.common.api.json.Message;
import org.smallmind.bayeux.oumuamua.common.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;
import org.smallmind.bayeux.oumuamua.server.spi.MetaProcessingException;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class MessageUtility {

  public static <V extends Value<V>> Message<V> convert (Codec<V> codec, ObjectNode node)
    throws MetaProcessingException {

    Message<V> message = codec.create();

    for (Map.Entry<String, JsonNode> fieldEntry : new IterableIterator<>(node.fields())) {
      message.put(fieldEntry.getKey(), convert(fieldEntry.getValue(), message.getFactory()));
    }

    return message;
  }

  private static <V extends Value<V>> Value<V> convert (JsonNode node, ValueFactory<V> factory)
    throws MetaProcessingException {

    switch (node.getNodeType()) {
      case OBJECT:

        ObjectValue<V> objectValue = factory.objectValue();

        for (Map.Entry<String, JsonNode> fieldEntry : new IterableIterator<>(node.fields())) {
          objectValue.put(fieldEntry.getKey(), convert(fieldEntry.getValue(), factory));
        }

        return objectValue;
      case ARRAY:

        ArrayValue<V> arrayValue = factory.arrayValue();

        for (JsonNode item : node) {
          arrayValue.add(convert(item, factory));
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
            throw new MetaProcessingException("Unknown number type(%s)", node.numberType().name());
        }
      case BOOLEAN:
        return factory.booleanValue(node.booleanValue());
      case NULL:
        return factory.nullValue();
      default:
        throw new MetaProcessingException("Unknown node type(%s)", node.getNodeType().name());
    }
  }
}
