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
package org.smallmind.claxon.registry.json;

import java.util.concurrent.TimeUnit;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.smallmind.nutsnbolts.time.Stint;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * JAXB/Jackson {@link XmlAdapter} that converts between a {@link Stint} and a JSON
 * {@link JsonNode}. The JSON representation is an object with two fields:
 * <ul>
 *   <li>{@code time} &ndash; the numeric duration as a {@code long}</li>
 *   <li>{@code timeUnit} &ndash; the {@link TimeUnit} name as a {@code String}</li>
 * </ul>
 * A {@code null} input on either side of the conversion produces a {@code null} output.
 */
public class StintXmlAdapter extends XmlAdapter<JsonNode, Stint> {

  /**
   * Serializes a {@link Stint} to a JSON {@link ObjectNode} containing {@code time} and
   * {@code timeUnit} fields.
   *
   * @param stint the {@link Stint} to serialize; may be {@code null}
   * @return a JSON {@link ObjectNode} representing the stint, or {@code null} if {@code stint}
   * is {@code null}
   */
  @Override
  public JsonNode marshal (Stint stint) {

    if (stint == null) {

      return null;
    } else {

      ObjectNode node = JsonNodeFactory.instance.objectNode();

      node.put("time", stint.getTime());
      node.put("timeUnit", stint.getTimeUnit().name());

      return node;
    }
  }

  /**
   * Deserializes a JSON {@link JsonNode} to a {@link Stint} by reading the {@code time}
   * and {@code timeUnit} fields.
   *
   * @param node the JSON node to deserialize; may be {@code null}
   * @return the reconstructed {@link Stint}, or {@code null} if {@code node} is {@code null}
   */
  @Override
  public Stint unmarshal (JsonNode node) {

    return (node == null) ? null : new Stint(node.get("time").longValue(), TimeUnit.valueOf(node.get("timeUnit").asString()));
  }
}
