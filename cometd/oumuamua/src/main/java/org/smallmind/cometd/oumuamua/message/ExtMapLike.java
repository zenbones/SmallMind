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

import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.Message;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class ExtMapLike extends MapLike {

  private final MapLike original;
  private JsonNode extNode;
  private String encodedText;
  private boolean altered;
  private boolean removed;
  private long parentEncodedVersion;

  public ExtMapLike (MapLike mapLike) {

    super(null, mapLike.getNode());

    original = mapLike;
  }



  @Override
  public void mutate () {

    altered = true;

    super.mutate();
  }

  public boolean isParentMutated () {

    return parentEncodedVersion != original.getVersion();
  }

  public void resetParentMutation () {

    parentEncodedVersion = original.getVersion();
  }

  @Override
  public Object get (Object key) {

    if (Message.EXT_FIELD.equals(key)) {
      if (removed) {

        return null;
      } else if (extNode != null) {

        return out(this, extNode);
      } else {

        JsonNode parentExtNode;

        if ((parentExtNode = original.getNode().get(Message.EXT_FIELD)) == null) {

          return null;
        } else if (JsonNodeType.OBJECT.equals(parentExtNode.getNodeType())) {

          return out(this, extNode = JsonCodec.clone(parentExtNode));
        } else {

          return out(original, parentExtNode);
        }
      }
    } else {

      return original.get(key);
    }
  }

  @Override
  public Map<String, Object> getAsMapLike (String key) {

    if (Message.EXT_FIELD.equals(key)) {
      if (removed) {

        return null;
      } else if (extNode != null) {

        return JsonNodeType.OBJECT.equals(extNode.getNodeType()) ? new MapLike(this, (ObjectNode)extNode) : null;
      } else {

        JsonNode parentExtNode;

        if ((parentExtNode = original.getNode().get(Message.EXT_FIELD)) == null) {

          return null;
        } else if (JsonNodeType.OBJECT.equals(parentExtNode.getNodeType())) {

          return new MapLike(this, (ObjectNode)(extNode = JsonCodec.clone(parentExtNode)));
        } else {

          return null;
        }
      }
    } else {

      return original.getAsMapLike(key);
    }
  }

  @Override
  public Map<String, Object> createIfAbsentMapLike (String key) {

    if (Message.EXT_FIELD.equals(key)) {
      if (removed) {
        mutate();
        removed = false;

        return new MapLike(this, (ObjectNode)(extNode = JsonNodeFactory.instance.objectNode()));
      } else if (extNode != null) {

        return JsonNodeType.OBJECT.equals(extNode.getNodeType()) ? new MapLike(this, (ObjectNode)extNode) : null;
      } else {

        JsonNode parentExtNode;

        if (((parentExtNode = original.getNode().get(Message.EXT_FIELD)) == null) || (!JsonNodeType.OBJECT.equals(parentExtNode.getNodeType()))) {
          mutate();

          return new MapLike(this, (ObjectNode)(extNode = JsonNodeFactory.instance.objectNode()));
        } else {

          return new MapLike(this, (ObjectNode)(extNode = JsonCodec.clone(parentExtNode)));
        }
      }
    } else {

      return original.createIfAbsentMapLike(key);
    }
  }

  @Override
  public Object put (String key, Object value) {

    if (Message.EXT_FIELD.equals(key)) {

      Object previousValue;

      if (removed) {
        previousValue = null;
      } else if (extNode != null) {
        previousValue = out(null, extNode);
      } else {
        previousValue = out(null, getNode().get(Message.EXT_FIELD));
      }

      mutate();
      extNode = in(value);
      removed = false;

      return previousValue;
    } else {

      return original.put(key, value);
    }
  }

  @Override
  public Object remove (Object key) {

    if (Message.EXT_FIELD.equals(key)) {

      Object previousValue;

      if (removed) {
        previousValue = null;
      } else if (extNode != null) {
        previousValue = out(null, extNode);
      } else {
        previousValue = out(null, getNode().get(Message.EXT_FIELD));
      }

      if (!removed) {
        mutate();
      }

      extNode = null;
      removed = true;

      return previousValue;
    } else {

      return original.remove(key);
    }
  }

  @Override
  public ObjectNode flatten () {

    if (!(altered || removed)) {

      return original.getNode();
    } else {

      ObjectNode clonedNode = (ObjectNode)JsonCodec.clone(original.getNode());

      if (removed || JsonNodeType.NULL.equals(extNode.getNodeType())) {
        clonedNode.remove(Message.EXT_FIELD);
      } else {
        clonedNode.set(Message.EXT_FIELD, extNode);
      }

      return clonedNode;
    }
  }

  @Override
  public String encode ()
    throws JsonProcessingException {

    if (!(altered || removed)) {

      return original.encode();
    } else if ((encodedText == null) || isMutated() || isParentMutated()) {

      ObjectNode clonedNode = (ObjectNode)JsonCodec.clone(original.getNode());

      if (removed || JsonNodeType.NULL.equals(extNode.getNodeType())) {
        clonedNode.remove(Message.EXT_FIELD);
      } else {
        clonedNode.set(Message.EXT_FIELD, extNode);
      }

      encodedText = JsonCodec.writeAsString(clonedNode);
      resetMutation();
      resetParentMutation();

      return encodedText;
    } else {

      return encodedText;
    }
  }
}
