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
package org.smallmind.bayeux.oumuamua.server.api.json;

import java.util.Collection;

/**
 * Mutable JSON array value with convenience helpers for primitive insertion.
 */
public interface ArrayValue<V extends Value<V>> extends Value<V> {

  /**
   * Identifies this value as an array.
   *
   * @return {@link ValueType#ARRAY}
   */
  default ValueType getType () {

    return ValueType.ARRAY;
  }

  /**
   * Appends a boolean value.
   *
   * @param bool boolean to add
   * @return this array
   */
  default ArrayValue<V> add (boolean bool) {

    return add(getFactory().booleanValue(bool));
  }

  /**
   * Sets a boolean at the given index.
   *
   * @param index position to set
   * @param bool boolean to store
   * @return this array
   */
  default ArrayValue<V> set (int index, boolean bool) {

    return set(index, getFactory().booleanValue(bool));
  }

  /**
   * Inserts a boolean before the given index.
   *
   * @param index position to insert at
   * @param bool boolean to insert
   * @return this array
   */
  default ArrayValue<V> insert (int index, boolean bool) {

    return insert(index, getFactory().booleanValue(bool));
  }

  /**
   * Appends an int value.
   *
   * @param i integer to add
   * @return this array
   */
  default ArrayValue<V> add (int i) {

    return add(getFactory().numberValue(i));
  }

  /**
   * Sets an int at the given index.
   *
   * @param index position to set
   * @param i integer to store
   * @return this array
   */
  default ArrayValue<V> set (int index, int i) {

    return set(index, getFactory().numberValue(i));
  }

  /**
   * Inserts an int before the given index.
   *
   * @param index position to insert at
   * @param i integer to insert
   * @return this array
   */
  default ArrayValue<V> insert (int index, int i) {

    return insert(index, getFactory().numberValue(i));
  }

  /**
   * Appends a long value.
   *
   * @param l long to add
   * @return this array
   */
  default ArrayValue<V> add (long l) {

    return add(getFactory().numberValue(l));
  }

  /**
   * Sets a long at the given index.
   *
   * @param index position to set
   * @param l long to store
   * @return this array
   */
  default ArrayValue<V> set (int index, long l) {

    return set(index, getFactory().numberValue(l));
  }

  /**
   * Inserts a long before the given index.
   *
   * @param index position to insert at
   * @param l long to insert
   * @return this array
   */
  default ArrayValue<V> insert (int index, long l) {

    return insert(index, getFactory().numberValue(l));
  }

  /**
   * Appends a double value.
   *
   * @param d double to add
   * @return this array
   */
  default ArrayValue<V> add (double d) {

    return add(getFactory().numberValue(d));
  }

  /**
   * Sets a double at the given index.
   *
   * @param index position to set
   * @param d double to store
   * @return this array
   */
  default ArrayValue<V> set (int index, double d) {

    return set(index, getFactory().numberValue(d));
  }

  /**
   * Inserts a double before the given index.
   *
   * @param index position to insert at
   * @param d double to insert
   * @return this array
   */
  default ArrayValue<V> insert (int index, double d) {

    return insert(index, getFactory().numberValue(d));
  }

  /**
   * Appends a string value.
   *
   * @param text text to add
   * @return this array
   */
  default ArrayValue<V> add (String text) {

    return add(getFactory().textValue(text));
  }

  /**
   * Sets a string at the given index.
   *
   * @param index position to set
   * @param text text to store
   * @return this array
   */
  default ArrayValue<V> set (int index, String text) {

    return set(index, getFactory().textValue(text));
  }

  /**
   * Inserts a string before the given index.
   *
   * @param index position to insert at
   * @param text text to insert
   * @return this array
   */
  default ArrayValue<V> insert (int index, String text) {

    return insert(index, getFactory().textValue(text));
  }

  /**
   * @return number of items in the array
   */
  int size ();

  /**
   * @return {@code true} if the array has no items
   */
  boolean isEmpty ();

  /**
   * Retrieves a value at the specified index.
   *
   * @param index index to read
   * @return the value at the index
   */
  Value<V> get (int index);

  /**
   * Appends a value.
   *
   * @param value value to add
   * @return this array
   */
  <U extends Value<V>> ArrayValue<V> add (U value);

  /**
   * Replaces a value at the specified index.
   *
   * @param index index to set
   * @param value value to store
   * @return this array
   */
  <U extends Value<V>> ArrayValue<V> set (int index, U value);

  /**
   * Inserts a value before the given index.
   *
   * @param index index to insert before
   * @param value value to insert
   * @return this array
   */
  <U extends Value<V>> ArrayValue<V> insert (int index, U value);

  /**
   * Removes the value at the given index.
   *
   * @param index index to remove
   * @return the removed value
   */
  Value<V> remove (int index);

  /**
   * Appends a collection of values.
   *
   * @param values values to add
   * @param <U> value subtype
   * @return this array
   */
  <U extends Value<V>> ArrayValue<V> addAll (Collection<U> values);

  /**
   * Clears all entries from the array.
   *
   * @return this array
   */
  ArrayValue<V> removeAll ();
}
