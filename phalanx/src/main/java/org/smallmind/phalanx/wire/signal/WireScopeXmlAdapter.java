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
import org.smallmind.phalanx.wire.WireScopeManager;
import org.smallmind.web.json.scaffold.util.JsonCodec;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * JAXB {@link XmlAdapter} that converts between a JSON array node and a {@link WireScope} array. Each array
 * element is a single-key object whose key is the scope's tag; known tags are resolved to a class through
 * {@link WireScopeManager} and unknown tags are preserved as {@link ProtoWireScope} instances. This is the
 * scope counterpart of {@link WireContextXmlAdapter}.
 * <p>
 * Because {@link #unmarshal} consults {@link WireScopeManager} for each element, it must run on a thread with a
 * {@code PerApplicationScope} bound whenever the node carries at least one single-property element (the only shape that is looked up); on an unbound thread it then
 * throws {@code MissingPerApplicationScopeException}. A {@code null} node or an empty array decodes without any
 * binding, and {@link #marshal} needs none.
 */
public class WireScopeXmlAdapter extends XmlAdapter<JsonNode, WireScope[]> {

  /**
   * Converts a JSON array node into an array of {@link WireScope} instances. Each element must be a JSON object
   * with exactly one property, whose name is the scope tag; elements with any other shape are skipped. A tag with a
   * class registered in {@link WireScopeManager} is converted to that class through {@code JsonCodec}; any other tag
   * becomes a {@link ProtoWireScope} holding the tag and the raw value.
   *
   * @param node the JSON array node to convert, or {@code null}
   * @return a (possibly empty) array of reconstructed scopes in array order; never {@code null}
   * @throws org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException if {@code node} carries at least one single-property element and no per-application scope is bound on the current thread; a {@code null} node or an empty array needs no binding
   */
  @Override
  public WireScope[] unmarshal (JsonNode node) {

    WireScope[] scopes;
    LinkedList<WireScope> scopeList = new LinkedList<>();

    if (node != null) {
      for (JsonNode elementNode : new IterableIterator<>(node.iterator())) {
        if (elementNode.size() == 1) {

          Map.Entry<String, JsonNode> topEntry = elementNode.properties().iterator().next();
          Class<? extends WireScope> scopeClass;

          if ((scopeClass = WireScopeManager.getScopeClass(topEntry.getKey())) != null) {
            scopeList.add(JsonCodec.instance().convert(topEntry.getValue(), scopeClass));
          } else {
            scopeList.add(new ProtoWireScope(topEntry.getKey(), topEntry.getValue()));
          }
        }
      }
    }

    scopes = new WireScope[scopeList.size()];
    scopeList.toArray(scopes);

    return scopes;
  }

  /**
   * Converts an array of {@link WireScope} objects into a JSON array node where each element is a single-key
   * object mapping the scope's tag to its JSON representation. The tag is the class's {@code @XmlRootElement}
   * name when present and its simple class name otherwise. {@link ProtoWireScope} instances are re-emitted
   * verbatim using their original tag and raw payload.
   *
   * @param wireScopes the scopes to serialize; may be {@code null} or empty
   * @return a JSON array node, or {@code null} if {@code wireScopes} is {@code null} or empty
   */
  @Override
  public JsonNode marshal (WireScope[] wireScopes) {

    if ((wireScopes == null) || (wireScopes.length == 0)) {

      return null;
    }

    ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(wireScopes.length);

    for (WireScope wireScope : wireScopes) {
      if (wireScope instanceof ProtoWireScope) {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        objectNode.set(((ProtoWireScope)wireScope).getSkin(), JsonCodec.instance().writeAsJsonNode(((ProtoWireScope)wireScope).getGuts()));
        arrayNode.add(objectNode);
      } else {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

        XmlRootElement xmlRootElementAnnotation = wireScope.getClass().getAnnotation(XmlRootElement.class);

        objectNode.set((xmlRootElementAnnotation == null) ? wireScope.getClass().getSimpleName() : xmlRootElementAnnotation.name(), JsonCodec.instance().writeAsJsonNode(wireScope));
        arrayNode.add(objectNode);
      }
    }

    return arrayNode;
  }
}
