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
package org.smallmind.phalanx.wire.signal;

import java.util.LinkedList;
import java.util.Map;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.phalanx.wire.WireContextManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * JAXB {@link XmlAdapter} that converts between a JSON array node and a {@link WireContext}
 * array; known context types are resolved via {@link org.smallmind.phalanx.wire.WireContextManager}
 * and unknown types are preserved as {@link ProtoWireContext} instances.
 */
public class WireContextXmlAdapter extends XmlAdapter<JsonNode, WireContext[]> {

  /**
   * Converts a JSON array node into an array of {@link WireContext} instances.
   * Each array element must be a single-entry JSON object whose key is the context type tag;
   * registered types are deserialized to their concrete class while unregistered types are
   * wrapped in a {@link ProtoWireContext}.
   *
   * @param node the JSON array node to convert, or {@code null}
   * @return a (possibly empty) array of reconstructed {@link WireContext} objects
   */
  @Override
  public WireContext[] unmarshal (JsonNode node) {

    WireContext[] contexts;
    LinkedList<WireContext> contextList = new LinkedList<>();

    if (node != null) {
      for (JsonNode elementNode : new IterableIterator<>(node.iterator())) {
        if (elementNode.size() == 1) {

          Map.Entry<String, JsonNode> topEntry = elementNode.properties().iterator().next();
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

  /**
   * Converts an array of {@link WireContext} objects into a JSON array node where each element
   * is a single-key object mapping the context type tag to its JSON representation.
   * {@link ProtoWireContext} instances are re-emitted verbatim using their original tag and raw payload.
   *
   * @param wireContexts the contexts to serialize; may be {@code null} or empty
   * @return a JSON array node, or {@code null} if {@code wireContexts} is {@code null} or empty
   */
  @Override
  public JsonNode marshal (WireContext[] wireContexts) {

    if ((wireContexts == null) || (wireContexts.length == 0)) {

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
