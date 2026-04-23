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
package org.smallmind.bayeux.oumuamua.server.spi.json.orthodox;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;
import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Linked-list-backed {@link ArrayValue} implementation for the orthodox codec, providing ordered
 * positional access and mutation with simple O(n) index traversal.
 */
public class OrthodoxArrayValue extends OrthodoxValue implements ArrayValue<OrthodoxValue> {

  private final LinkedList<Value<OrthodoxValue>> valueList = new LinkedList<>();

  /**
   * Constructs an empty array associated with the given factory.
   *
   * @param factory the {@link OrthodoxValueFactory} that owns this value
   */
  protected OrthodoxArrayValue (OrthodoxValueFactory factory) {

    super(factory);
  }

  /**
   * Returns the number of elements currently held in the array.
   *
   * @return element count
   */
  @Override
  public int size () {

    return valueList.size();
  }

  /**
   * Reports whether the array contains no elements.
   *
   * @return {@code true} when the array is empty
   */
  @Override
  public boolean isEmpty () {

    return valueList.isEmpty();
  }

  /**
   * Returns the value at the specified zero-based index.
   *
   * @param index zero-based position to retrieve
   * @return value at {@code index}
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   */
  @Override
  public Value<OrthodoxValue> get (int index) {

    return valueList.get(index);
  }

  /**
   * Appends {@code value} at the end of the array.
   *
   * @param value value to append
   * @return this array for chaining
   */
  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> add (U value) {

    valueList.add(value);

    return this;
  }

  /**
   * Replaces the element at {@code index} with {@code value}.
   *
   * @param index zero-based position to replace
   * @param value replacement value
   * @return this array for chaining
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   */
  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> set (int index, U value) {

    valueList.set(index, value);

    return this;
  }

  /**
   * Inserts {@code value} at {@code index}, shifting all subsequent elements one position right.
   *
   * @param index zero-based insertion point
   * @param value value to insert
   * @return this array for chaining
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   */
  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> insert (int index, U value) {

    valueList.add(index, value);

    return this;
  }

  /**
   * Removes and returns the element at {@code index}, shifting subsequent elements left.
   *
   * @param index zero-based position to remove
   * @return the value that occupied {@code index}
   * @throws IndexOutOfBoundsException if {@code index} is out of range
   */
  @Override
  public Value<OrthodoxValue> remove (int index) {

    return valueList.remove(index);
  }

  /**
   * Appends every element of {@code values} in iteration order.
   *
   * @param values collection of values to append
   * @return this array for chaining
   */
  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> addAll (Collection<U> values) {

    valueList.addAll(values);

    return this;
  }

  /**
   * Removes all elements from the array, leaving it empty.
   *
   * @return this array for chaining
   */
  @Override
  public ArrayValue<OrthodoxValue> removeAll () {

    valueList.clear();

    return this;
  }

  /**
   * Writes the JSON array representation of all non-null elements to {@code writer}.
   *
   * @param writer destination for the JSON output
   * @throws IOException if writing to {@code writer} fails
   */
  @Override
  public void encode (Writer writer)
    throws IOException {

    boolean first = true;

    writer.write('[');
    for (Value<OrthodoxValue> value : valueList) {
      if (value != null) {
        if (!first) {
          writer.write(',');
        }

        value.encode(writer);

        first = false;
      }
    }
    writer.write(']');
  }
}
