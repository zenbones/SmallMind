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
package org.smallmind.bayeux.oumuamua.spi.json.jackson;

import java.util.Iterator;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.bayeux.oumuamua.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.api.json.Value;

public class JacksonObjectValue extends JacksonValue<ObjectNode> implements ObjectValue<JacksonValue<?>> {

  public JacksonObjectValue (ObjectNode node) {

    super(node);
  }

  @Override
  public int size () {

    return getNode().size();
  }

  @Override
  public boolean isEmpty () {

    return getNode().isEmpty();
  }

  @Override
  public JacksonValue<?> get (String field) {

    return JacksonValueUtility.to(getNode().get(field));
  }

  @Override
  public JacksonValue<?> put (String field, Value<JacksonValue<?>> value) {

    getNode().set(field, JacksonValueUtility.from(value));

    return this;
  }

  @Override
  public JacksonValue<?> remove (String field) {

    return JacksonValueUtility.to(getNode().remove(field));
  }

  @Override
  public JacksonValue<?> removeAll () {

    getNode().removeAll();

    return this;
  }

  @Override
  public Iterator<Map.Entry<String, JacksonValue<?>>> iterator () {

    return new EntryIterator(getNode().fields());
  }

  private static class ValueEntry implements Map.Entry<String, JacksonValue<?>> {

    private final Map.Entry<String, JsonNode> entry;

    public ValueEntry (Map.Entry<String, JsonNode> entry) {

      this.entry = entry;
    }

    @Override
    public String getKey () {

      return entry.getKey();
    }

    @Override
    public JacksonValue<?> getValue () {

      return JacksonValueUtility.to(entry.getValue());
    }

    @Override
    public JacksonValue<?> setValue (JacksonValue<?> value) {

      return JacksonValueUtility.to(entry.setValue(JacksonValueUtility.from(value)));
    }
  }

  private static class EntryIterator implements Iterator<Map.Entry<String, JacksonValue<?>>> {

    private final Iterator<Map.Entry<String, JsonNode>> nodeIterator;

    public EntryIterator (Iterator<Map.Entry<String, JsonNode>> nodeIterator) {

      this.nodeIterator = nodeIterator;
    }

    @Override
    public boolean hasNext () {

      return nodeIterator.hasNext();
    }

    @Override
    public Map.Entry<String, JacksonValue<?>> next () {

      return new ValueEntry(nodeIterator.next());
    }
  }
}
