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
package org.smallmind.web.json.scaffold.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Utility class providing structural equality comparison for {@link JsonNode} instances
 * that treats object fields and array elements as order-independent.
 */
public class JsonNodeUtility {

  /**
   * Returns whether two {@link JsonNode} instances are structurally equal, ignoring field order in objects
   * and element order in arrays.
   *
   * <p>For object nodes, all fields from both nodes must match by key and recursively equal value.
   * For array nodes, every element in one array must have a matching equal element in the other,
   * regardless of position. For number nodes, values are compared as {@code double}. All other
   * node types use standard {@link JsonNode#equals} equality.
   *
   * @param node1 the first node to compare, or {@code null}
   * @param node2 the second node to compare, or {@code null}
   * @return {@code true} if both nodes are structurally equal (ignoring order), {@code false} otherwise;
   * two {@code null} references are considered equal
   */
  public static boolean equalsIgnoresOrder (JsonNode node1, JsonNode node2) {

    if ((node1 == null) && (node2 == null)) {

      return true;
    } else if ((node1 == null) || (node2 == null)) {

      return false;
    } else if (node1.getNodeType().equals(node2.getNodeType())) {
      return switch (node1.getNodeType()) {
        case OBJECT -> {

          if (node1.size() != node2.size()) {

            yield false;
          } else {
            for (Map.Entry<String, JsonNode> attribute1Entry : node1.properties()) {

              JsonNode value2;

              if ((value2 = node2.get(attribute1Entry.getKey())) == null || (!equalsIgnoresOrder(attribute1Entry.getValue(), value2))) {

                yield false;
              }
            }

            yield true;
          }
        }
        case ARRAY -> {

          if (node1.size() != node2.size()) {

            yield false;
          } else {

            LinkedList<JsonNode> item1List = new LinkedList<>();

            for (JsonNode item1Node : node1) {
              item1List.add(item1Node);
            }
            for (JsonNode item2Node : node2) {

              Iterator<JsonNode> item1Iterator = item1List.iterator();
              boolean found = false;

              while (item1Iterator.hasNext()) {

                JsonNode item1Node = item1Iterator.next();

                if (equalsIgnoresOrder(item1Node, item2Node)) {
                  found = true;
                  item1Iterator.remove();
                  break;
                }
              }

              if (!found) {

                yield false;
              }
            }

            yield item1List.isEmpty();
          }
        }
        // compare as double so 2 equals 2.0; Jackson's own equals is type-partitioned (e.g. IntNode never equals DoubleNode)
        case NUMBER -> node1.asDouble() == node2.asDouble();
        default -> node1.equals(node2);
      };
    } else {

      return false;
    }
  }

  /**
   * Returns a hash code for the given {@link JsonNode} that is consistent with
   * {@link #equalsIgnoresOrder(JsonNode, JsonNode)}: whenever {@code equalsIgnoresOrder(node1, node2)} is
   * {@code true}, {@code hashCodeIgnoresOrder(node1) == hashCodeIgnoresOrder(node2)}.
   *
   * <p>Object field hashes and array element hashes are combined additively, so the result is independent
   * of field and element order (addition, not exclusive-or, so element multiplicity is preserved). Number
   * nodes are hashed by their {@code double} value, with {@code -0.0} folded to {@code 0.0}, so that values
   * treated as equal by {@link #equalsIgnoresOrder} (e.g. {@code 2} and {@code 2.0}) hash alike. All other
   * node types use their standard {@link JsonNode#hashCode}.
   *
   * @param node the node to hash, or {@code null}
   * @return an order-independent hash code consistent with {@link #equalsIgnoresOrder}; {@code 0} for a
   * {@code null} reference
   */
  public static int hashCodeIgnoresOrder (JsonNode node) {

    if (node == null) {

      return 0;
    } else {
      return switch (node.getNodeType()) {
        case OBJECT -> {

          int hashCode = 0;

          for (Map.Entry<String, JsonNode> attributeEntry : node.properties()) {
            hashCode += attributeEntry.getKey().hashCode() ^ hashCodeIgnoresOrder(attributeEntry.getValue());
          }

          yield hashCode;
        }
        case ARRAY -> {

          int hashCode = 0;

          for (JsonNode itemNode : node) {
            hashCode += hashCodeIgnoresOrder(itemNode);
          }

          yield hashCode;
        }
        case NUMBER -> {

          double value = node.asDouble();

          // fold -0.0 to 0.0 so values equal under '==' (as equalsIgnoresOrder compares numbers) hash alike
          yield Double.hashCode((value == 0.0D) ? 0.0D : value);
        }
        default -> node.hashCode();
      };
    }
  }
}
