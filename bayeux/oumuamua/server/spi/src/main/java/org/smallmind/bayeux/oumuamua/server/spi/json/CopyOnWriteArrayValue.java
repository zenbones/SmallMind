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
package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueFactory;

/**
 * Array wrapper that defers copying until mutation occurs, sharing the original array for reads.
 *
 * @param <V> concrete value type used in messages
 */
public class CopyOnWriteArrayValue<V extends Value<V>> implements ArrayValue<V> {

  private final ArrayValue<V> innerArrayValue;
  private ArrayValue<V> outerArrayValue;

  /**
   * Wraps an existing array value.
   *
   * @param innerArrayValue array to wrap
   */
  public CopyOnWriteArrayValue (ArrayValue<V> innerArrayValue) {

    this.innerArrayValue = innerArrayValue;
  }

  /**
   * Realizes a copy of the inner array to allow safe mutation.
   *
   * @return the mutable copy
   */
  private ArrayValue<V> fill () {

    outerArrayValue = innerArrayValue.getFactory().arrayValue();
    for (int index = 0; index < innerArrayValue.size(); index++) {
      outerArrayValue.add(innerArrayValue.get(index));
    }

    return outerArrayValue;
  }

  /**
   * @return the value factory associated with the underlying array
   */
  @Override
  public ValueFactory<V> getFactory () {

    return innerArrayValue.getFactory();
  }

  /**
   * @return current array size, reading from the copy if it exists
   */
  @Override
  public int size () {

    return (outerArrayValue != null) ? outerArrayValue.size() : innerArrayValue.size();
  }

  /**
   * @return {@code true} when the array is empty
   */
  @Override
  public boolean isEmpty () {

    return size() == 0;
  }

  /**
   * Retrieves a value, creating defensive wrappers for nested objects/arrays when needed.
   *
   * @param index index to retrieve
   * @return the value at the index or {@code null}
   */
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

  /**
   * Appends a value, realizing the copy if necessary.
   *
   * @param value value to add
   * @return this array
   */
  @Override
  public <U extends Value<V>> ArrayValue<V> add (U value) {

    fill().add(value);

    return this;
  }

  /**
   * Sets a value at the index, realizing the copy if necessary.
   *
   * @param index index to set
   * @param value value to store
   * @return this array
   */
  @Override
  public <U extends Value<V>> ArrayValue<V> set (int index, U value) {

    fill().set(index, value);

    return this;
  }

  /**
   * Inserts a value before the index, realizing the copy if necessary.
   *
   * @param index position to insert
   * @param value value to insert
   * @return this array
   */
  @Override
  public <U extends Value<V>> ArrayValue<V> insert (int index, U value) {

    fill().insert(index, value);

    return this;
  }

  /**
   * Removes a value at the index, realizing the copy if necessary.
   *
   * @param index index to remove
   * @return removed value
   */
  @Override
  public Value<V> remove (int index) {

    return fill().remove(index);
  }

  /**
   * Appends a collection of values, realizing the copy if necessary.
   *
   * @param values values to add
   * @return this array
   */
  @Override
  public <U extends Value<V>> ArrayValue<V> addAll (Collection<U> values) {

    fill().addAll(values);

    return this;
  }

  /**
   * Clears all values by replacing with a new array instance.
   *
   * @return this array
   */
  @Override
  public ArrayValue<V> removeAll () {

    outerArrayValue = innerArrayValue.getFactory().arrayValue();

    return this;
  }

  /**
   * Encodes either the original array or the realized copy.
   *
   * @param writer destination writer
   * @throws IOException if encoding fails
   */
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
