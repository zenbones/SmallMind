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
 * Lazily-copied {@link ArrayValue} that shares the original backing array for reads and materializes
 * a mutable copy only when a write operation is first invoked.
 *
 * @param <V> the concrete {@link Value} subtype carried by this array
 */
public class CopyOnWriteArrayValue<V extends Value<V>> implements ArrayValue<V> {

  private final ArrayValue<V> innerArrayValue;
  private ArrayValue<V> outerArrayValue;

  /**
   * Wraps {@code innerArrayValue} as the read-through backing array.
   *
   * @param innerArrayValue the original array to wrap; reads fall through to this until a write occurs
   */
  public CopyOnWriteArrayValue (ArrayValue<V> innerArrayValue) {

    this.innerArrayValue = innerArrayValue;
  }

  /**
   * Materializes the mutable outer copy by cloning all elements from the inner array,
   * then returns it for chaining; subsequent calls are no-ops because the outer copy already exists.
   *
   * @return the mutable outer copy, ready for modification
   */
  private ArrayValue<V> fill () {

    outerArrayValue = innerArrayValue.getFactory().arrayValue();
    for (int index = 0; index < innerArrayValue.size(); index++) {
      outerArrayValue.add(innerArrayValue.get(index));
    }

    return outerArrayValue;
  }

  /**
   * Returns the {@link ValueFactory} associated with the inner array.
   *
   * @return value factory for creating new values of type {@code V}
   */
  @Override
  public ValueFactory<V> getFactory () {

    return innerArrayValue.getFactory();
  }

  /**
   * Returns the number of elements in the effective array, consulting the outer copy when available.
   *
   * @return element count
   */
  @Override
  public int size () {

    return (outerArrayValue != null) ? outerArrayValue.size() : innerArrayValue.size();
  }

  /**
   * Reports whether the effective array contains no elements.
   *
   * @return {@code true} when the array is empty
   */
  @Override
  public boolean isEmpty () {

    return size() == 0;
  }

  /**
   * Returns the value at {@code index}, wrapping nested objects in {@link MergingObjectValue} and
   * nested arrays in {@link CopyOnWriteArrayValue} on first access so mutations remain isolated.
   * When the outer copy already exists, delegates directly to it.
   *
   * @param index zero-based position to retrieve
   * @return value at the position, or {@code null} if the slot is empty
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
   * Appends {@code value} to the array, materializing the outer copy if not yet done.
   *
   * @param value value to append
   * @return this array for chaining
   */
  @Override
  public <U extends Value<V>> ArrayValue<V> add (U value) {

    fill().add(value);

    return this;
  }

  /**
   * Replaces the element at {@code index} with {@code value}, materializing the outer copy if not yet done.
   *
   * @param index zero-based position to replace
   * @param value replacement value
   * @return this array for chaining
   */
  @Override
  public <U extends Value<V>> ArrayValue<V> set (int index, U value) {

    fill().set(index, value);

    return this;
  }

  /**
   * Inserts {@code value} at {@code index}, shifting existing elements right, materializing the outer copy if not yet done.
   *
   * @param index zero-based insertion point
   * @param value value to insert
   * @return this array for chaining
   */
  @Override
  public <U extends Value<V>> ArrayValue<V> insert (int index, U value) {

    fill().insert(index, value);

    return this;
  }

  /**
   * Removes and returns the element at {@code index}, materializing the outer copy if not yet done.
   *
   * @param index zero-based position to remove
   * @return the value that was removed
   */
  @Override
  public Value<V> remove (int index) {

    return fill().remove(index);
  }

  /**
   * Appends all elements in {@code values}, materializing the outer copy if not yet done.
   *
   * @param values collection of values to append
   * @return this array for chaining
   */
  @Override
  public <U extends Value<V>> ArrayValue<V> addAll (Collection<U> values) {

    fill().addAll(values);

    return this;
  }

  /**
   * Discards all elements by substituting a fresh empty array as the outer copy, abandoning both
   * the previous outer copy and the inner backing array.
   *
   * @return this array for chaining
   */
  @Override
  public ArrayValue<V> removeAll () {

    outerArrayValue = innerArrayValue.getFactory().arrayValue();

    return this;
  }

  /**
   * Writes the JSON array representation to {@code writer}, using the outer copy when it exists
   * and falling back to the inner array otherwise.
   *
   * @param writer destination for the JSON output
   * @throws IOException if writing to {@code writer} fails
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
