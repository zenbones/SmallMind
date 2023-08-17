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
package org.smallmind.bayeux.oumuamua.server.spi.json.jackson;

import java.util.Collection;
import java.util.Iterator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;

public class JacksonArrayValue extends JacksonValue<ArrayNode> implements ArrayValue<JacksonValue<?>> {

  public JacksonArrayValue (ArrayNode node, ValueFactory<JacksonValue<?>> factory) {

    super(node, factory);
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
  public JacksonValue<?> get (int index) {

    return JacksonValueUtility.to(getNode().get(index), getFactory());
  }

  @Override
  public JacksonValue<?> add (Value<JacksonValue<?>> value) {

    getNode().add(JacksonValueUtility.from(value));

    return this;
  }

  @Override
  public JacksonValue<?> set (int index, Value<JacksonValue<?>> value) {

    getNode().set(index, JacksonValueUtility.from(value));

    return this;
  }

  @Override
  public JacksonValue<?> insert (int index, Value<JacksonValue<?>> value) {

    getNode().insert(index, JacksonValueUtility.from(value));

    return this;
  }

  @Override
  public JacksonValue<?> remove (int index) {

    return JacksonValueUtility.to(getNode().remove(index), getFactory());
  }

  @Override
  public JacksonValue<?> addAll (Collection<JacksonValue<?>> values) {

    for (JacksonValue<?> value : values) {
      getNode().add(JacksonValueUtility.from(value));
    }

    return this;
  }

  @Override
  public JacksonValue<?> removeAll () {

    getNode().removeAll();

    return this;
  }

  @Override
  public Iterator<JacksonValue<?>> iterator () {

    return new ArrayIterator(getNode().iterator());
  }

  private class ArrayIterator implements Iterator<JacksonValue<?>> {

    private final Iterator<JsonNode> nodeIterator;

    public ArrayIterator (Iterator<JsonNode> nodeIterator) {

      this.nodeIterator = nodeIterator;
    }

    @Override
    public boolean hasNext () {

      return nodeIterator.hasNext();
    }

    @Override
    public JacksonValue<?> next () {

      return JacksonValueUtility.to(nodeIterator.next(), getFactory());
    }
  }
}
