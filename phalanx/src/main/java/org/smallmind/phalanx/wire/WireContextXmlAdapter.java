/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.phalanx.wire;

import java.util.LinkedList;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.web.jersey.util.JsonCodec;

public class WireContextXmlAdapter extends XmlAdapter<JsonNode, WireContext[]> {

  @Override
  public WireContext[] unmarshal (JsonNode node) {

    WireContext[] contexts;
    LinkedList<WireContext> contextList = new LinkedList<>();

    if (node != null) {
      for (JsonNode elementNode : new IterableIterator<>(node.elements())) {
        if (elementNode.size() == 1) {

          Map.Entry<String, JsonNode> topEntry = elementNode.fields().next();
          Class<? extends WireContext> contextClass;

          if ((contextClass = WireContextManager.getContextClass(topEntry.getKey())) != null) {
            contextList.add(JsonCodec.convert(topEntry.getValue(), contextClass));
          } else {
            contextList.add(new ProtoWireContext(topEntry.getKey(), topEntry.getValue()));
          }
        }
      }
    }

    contexts = new WireContext[contextList.size()];
    contextList.toArray(contexts);

    return contexts;
  }

  @Override
  public JsonNode marshal (WireContext[] wireContexts)
    throws JsonProcessingException {

    if (wireContexts == null) {

      return null;
    }

    ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(wireContexts.length);

    for (WireContext wireContext : wireContexts) {
      if (wireContext instanceof ProtoWireContext) {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        objectNode.set(((ProtoWireContext)wireContext).getSkin(), JsonCodec.writeAsJsonNode(((ProtoWireContext)wireContext).getGuts()));
        arrayNode.add(objectNode);
      } else {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        XmlRootElement xmlRootElementAnnotation = wireContext.getClass().getAnnotation(XmlRootElement.class);

        objectNode.set((xmlRootElementAnnotation == null) ? wireContext.getClass().getSimpleName() : xmlRootElementAnnotation.name(), JsonCodec.writeAsJsonNode(wireContext));
        arrayNode.add(objectNode);
      }
    }

    return arrayNode;
  }
}