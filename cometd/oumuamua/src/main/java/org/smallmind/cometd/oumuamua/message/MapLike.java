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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.nutsnbolts.util.IterableIterator;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class MapLike extends NodeBacked implements Map<String, Object> {

  private final ObjectNode node;

  public MapLike (NodeBacked parent, ObjectNode node) {

    super(parent);

    this.node = node;
  }

  public ObjectNode getNode () {

    return node;
  }

  @Override
  public String writeAsString ()
    throws JsonProcessingException {

    return JsonCodec.writeAsString(node);
  }

  @Override
  public int size () {

    return node.size();
  }

  @Override
  public boolean isEmpty () {

    return node.isEmpty();
  }

  @Override
  public boolean containsKey (Object key) {

    return node.has(key.toString());
  }

  @Override
  public boolean containsValue (Object value) {

    JsonNode convertedValue = in(value);

    for (JsonNode element : new IterableIterator<>(node.elements())) {
      if (element.equals(convertedValue)) {

        return true;
      }
    }

    return false;
  }

  @Override
  public Object get (Object key) {

    return out(this, node.get(key.toString()));
  }

  public Map<String, Object> getAsMapLike (String key) {

    JsonNode childNode;

    if ((childNode = node.get(key)) != null) {
      if (JsonNodeType.OBJECT.equals(childNode.getNodeType())) {

        return new MapLike(this, (ObjectNode)childNode);
      }
    }

    return null;
  }

  public Map<String, Object> createIfAbsentMapLike (String key) {

    JsonNode childNode;

    if (((childNode = node.get(key)) == null) || (!JsonNodeType.OBJECT.equals(childNode.getNodeType()))) {
      node.set(key, childNode = JsonNodeFactory.instance.objectNode());
      mutate();
    }

    return new MapLike(this, (ObjectNode)childNode);
  }

  @Override
  public Object put (String key, Object value) {

    mutate();

    return node.set(key, in(value));
  }

  @Override
  public Object remove (Object key) {

    JsonNode removedNode = node.remove(key.toString());

    if (removedNode == null) {

      return null;
    } else {
      mutate();

      return out(null, removedNode);
    }
  }

  @Override
  public void putAll (Map<? extends String, ?> m) {

    for (Map.Entry<? extends String, ?> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }

    if (!m.isEmpty()) {
      mutate();
    }
  }

  @Override
  public void clear () {

    mutate();

    node.removeAll();
  }

  @Override
  public Set<String> keySet () {

    HashSet<String> keySet = new HashSet<>();

    for (String key : new IterableIterator<>(node.fieldNames())) {
      keySet.add(key);
    }

    return keySet;
  }

  @Override
  public Collection<Object> values () {

    LinkedList<Object> valueList = new LinkedList<>();

    for (JsonNode element : new IterableIterator<>(node.elements())) {
      valueList.add(out(this, element));
    }

    return valueList;
  }

  @Override
  public Set<Entry<String, Object>> entrySet () {

    HashSet<Map.Entry<String, Object>> entrySet = new HashSet<>();

    for (Map.Entry<String, JsonNode> entry : new IterableIterator<>(node.fields())) {
      entrySet.add(new JsonEntry(entry.getKey(), entry.getValue()));
    }

    return entrySet;
  }

  private class JsonEntry implements Map.Entry<String, Object> {

    private final String key;
    private JsonNode value;

    public JsonEntry (String key, JsonNode value) {

      this.key = key;
      this.value = value;
    }

    @Override
    public String getKey () {

      return key;
    }

    @Override
    public Object getValue () {

      return out(MapLike.this, value);
    }

    @Override
    public Object setValue (Object value) {

      JsonNode translatedValue;

      node.set(key, translatedValue = in(value));
      this.value = translatedValue;

      mutate();

      return value;
    }
  }
}
