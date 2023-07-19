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
package org.smallmind.cometd.oumuamua.message;

import java.io.Serializable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public abstract class NodeBacked implements Serializable {

  private final NodeBacked parent;
  private String encodedText;
  private long version;
  private long encodedVersion;

  public NodeBacked (NodeBacked parent) {

    this.parent = parent;
  }

  public abstract JsonNode getNode ();

  public abstract String writeAsString ()
    throws JsonProcessingException;

  public NodeBacked getParent () {

    return parent;
  }

  public boolean isMutated () {

    return encodedVersion != version;
  }

  public void mutate () {

    version += 1;

    if (parent != null) {
      parent.mutate();
    }
  }

  public void resetMutation () {

    encodedVersion = version;
  }

  public String encode ()
    throws JsonProcessingException {

    if ((encodedText == null) || isMutated()) {
      encodedText = writeAsString();
      resetMutation();
    }

    return encodedText;
  }

  public JsonNode in (Object obj) {

    if (obj == null) {

      return JsonNodeFactory.instance.nullNode();
    } else if (NodeBacked.class.isAssignableFrom(obj.getClass())) {

      return ((NodeBacked)obj).getNode();
    }
    if (JsonNode.class.isAssignableFrom(obj.getClass())) {

      return (JsonNode)obj;
    } else {

      return JsonCodec.writeAsJsonNode(obj);
    }
  }

  public Object out (NodeBacked parent, JsonNode node) {

    if (node == null) {

      return null;
    } else if (JsonNodeType.OBJECT.equals(node.getNodeType())) {

      return new MapLike(parent, (ObjectNode)node);
    } else if (JsonNodeType.ARRAY.equals(node.getNodeType())) {

      return new ListLike(parent, (ArrayNode)node);
    } else {
      try {

        return JsonCodec.read(node, Object.class);
      } catch (JsonProcessingException jsonProcessingException) {
        throw new MessageProcessingException(jsonProcessingException);
      }
    }
  }
}
