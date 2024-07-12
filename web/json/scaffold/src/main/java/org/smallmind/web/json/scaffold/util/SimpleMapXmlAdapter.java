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
package org.smallmind.web.json.scaffold.util;

import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.nutsnbolts.util.IterableIterator;

public abstract class SimpleMapXmlAdapter<M extends Map<String, V>, V> extends XmlAdapter<JsonNode, M> {

  public abstract M getEmptyMap ();

  public abstract Class<V> getValueClass ();

  @Override
  public M unmarshal (JsonNode node) {

    if (node != null) {
      if (!node.isObject()) {
        throw new RuntimeException("Expecting an object");
      } else {

        M map = getEmptyMap();

        for (Map.Entry<String, JsonNode> entry : new IterableIterator<>(node.fields())) {
          map.put("null".equals(entry.getKey()) ? null : entry.getKey(), JsonCodec.convert(entry.getValue(), getValueClass()));
        }

        return map;
      }
    }

    return null;
  }

  @Override
  public JsonNode marshal (M map) throws Exception {

    if (map != null) {

      ObjectNode rootNode = JsonNodeFactory.instance.objectNode();

      for (Map.Entry<String, V> entry : map.entrySet()) {
        rootNode.set((entry.getKey() == null) ? "null" : entry.getKey(), JsonCodec.writeAsJsonNode(entry.getValue()));
      }

      return rootNode;
    }

    return null;
  }
}
