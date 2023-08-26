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
package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import org.smallmind.bayeux.oumuamua.common.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.common.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.common.api.json.ValueFactory;

public class CopyOnWriteArrayValue<V extends Value<V>> implements ArrayValue<V> {

  private final ArrayValue<V> innerArrayValue;
  private ArrayValue<V> outerArrayValue;

  public CopyOnWriteArrayValue (ArrayValue<V> innerArrayValue) {

    this.innerArrayValue = innerArrayValue;
  }

  private ArrayValue<V> fill () {

    outerArrayValue = innerArrayValue.getFactory().arrayValue();
    for (int index = 0; index < innerArrayValue.size(); index++) {
      outerArrayValue.add(innerArrayValue.get(index));
    }

    return outerArrayValue;
  }

  @Override
  public ValueFactory<V> getFactory () {

    return innerArrayValue.getFactory();
  }

  @Override
  public int size () {

    return (outerArrayValue != null) ? outerArrayValue.size() : innerArrayValue.size();
  }

  @Override
  public boolean isEmpty () {

    return size() == 0;
  }

  @Override
  public Value<V> get (int index) {

    if (outerArrayValue != null) {

      return outerArrayValue.get(index);
    } else {

      Value<V> value;

      if ((value = innerArrayValue.get(index)) != null) {
        switch (value.getType()) {
          case OBJECT:

            MergingObjectValue<V> mergedValue;

            fill().set(index, mergedValue = new MergingObjectValue<>((ObjectValue<V>)value));

            return mergedValue;
          case ARRAY:

            CopyOnWriteArrayValue<V> copyOnWriteValue;

            fill().set(index, copyOnWriteValue = new CopyOnWriteArrayValue<>((ArrayValue<V>)value));

            return copyOnWriteValue;
          default:

            return value;
        }
      } else {

        return null;
      }
    }
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> add (U value) {

    fill().add(value);

    return this;
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> set (int index, U value) {

    fill().set(index, value);

    return this;
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> insert (int index, U value) {

    fill().insert(index, value);

    return this;
  }

  @Override
  public Value<V> remove (int index) {

    return fill().remove(index);
  }

  @Override
  public <U extends Value<V>> ArrayValue<V> addAll (Collection<U> values) {

    fill().addAll(values);

    return this;
  }

  @Override
  public ArrayValue<V> removeAll () {

    outerArrayValue = innerArrayValue.getFactory().arrayValue();

    return this;
  }

  @Override
  public void encode (Writer writer)
    throws IOException {

    if (outerArrayValue != null) {
      outerArrayValue.encode(writer);
    } else {
      innerArrayValue.encode(writer);
    }
  }
}
