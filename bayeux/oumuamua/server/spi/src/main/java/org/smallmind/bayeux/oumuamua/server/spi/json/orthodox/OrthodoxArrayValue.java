/*
 * Copyright (c) 2007 through 2024 David Berkman
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
 * Simple list-backed array value implementation for the orthodox codec.
 */
public class OrthodoxArrayValue extends OrthodoxValue implements ArrayValue<OrthodoxValue> {

  private final LinkedList<Value<OrthodoxValue>> valueList = new LinkedList<>();

  /**
   * Creates an empty array value.
   *
   * @param factory owning factory
   */
  protected OrthodoxArrayValue (OrthodoxValueFactory factory) {

    super(factory);
  }

  /**
   * @return number of elements in the array
   */
  @Override
  public int size () {

    return valueList.size();
  }

  /**
   * @return {@code true} if the array has no values
   */
  @Override
  public boolean isEmpty () {

    return valueList.isEmpty();
  }

  /**
   * Retrieves a value at the provided index.
   *
   * @param index index to read
   * @return value at the index
   */
  @Override
  public Value<OrthodoxValue> get (int index) {

    return valueList.get(index);
  }

  /**
   * Appends a value.
   *
   * @param value value to add
   * @return this array
   */
  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> add (U value) {

    valueList.add(value);

    return this;
  }

  /**
   * Replaces a value at the given index.
   *
   * @param index index to set
   * @param value value to store
   * @return this array
   */
  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> set (int index, U value) {

    valueList.set(index, value);

    return this;
  }

  /**
   * Inserts a value at the specified index.
   *
   * @param index insertion point
   * @param value value to insert
   * @return this array
   */
  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> insert (int index, U value) {

    valueList.add(index, value);

    return this;
  }

  /**
   * Removes a value at the index.
   *
   * @param index index to remove
   * @return removed value
   */
  @Override
  public Value<OrthodoxValue> remove (int index) {

    return valueList.remove(index);
  }

  /**
   * Appends all values from the provided collection.
   *
   * @param values values to add
   * @return this array
   */
  @Override
  public <U extends Value<OrthodoxValue>> ArrayValue<OrthodoxValue> addAll (Collection<U> values) {

    valueList.addAll(values);

    return this;
  }

  /**
   * Clears the array.
   *
   * @return this array
   */
  @Override
  public ArrayValue<OrthodoxValue> removeAll () {

    valueList.clear();

    return this;
  }

  /**
   * Encodes the array to JSON.
   *
   * @param writer destination writer
   * @throws IOException if writing fails
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
