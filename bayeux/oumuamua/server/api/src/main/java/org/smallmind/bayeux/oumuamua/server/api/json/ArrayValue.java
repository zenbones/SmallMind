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
package org.smallmind.bayeux.oumuamua.server.api.json;

import java.util.Collection;

/**
 * Mutable ordered sequence of {@link Value} instances corresponding to a JSON array,
 * with fluent primitive-convenience overloads for append, replace, and insert operations.
 *
 * @param <V> concrete value subtype held within this array
 */
public interface ArrayValue<V extends Value<V>> extends Value<V> {

  /**
   * Returns {@link ValueType#ARRAY}, identifying this value as a JSON array.
   *
   * @return {@link ValueType#ARRAY}
   */
  default ValueType getType () {

    return ValueType.ARRAY;
  }

  /**
   * Appends a boolean to the end of the array.
   *
   * @param bool value to append
   * @return this array for chaining
   */
  default ArrayValue<V> add (boolean bool) {

    return add(getFactory().booleanValue(bool));
  }

  /**
   * Replaces the element at {@code index} with a boolean.
   *
   * @param index zero-based position to replace
   * @param bool  replacement value
   * @return this array for chaining
   */
  default ArrayValue<V> set (int index, boolean bool) {

    return set(index, getFactory().booleanValue(bool));
  }

  /**
   * Inserts a boolean before the element at {@code index}, shifting subsequent elements right.
   *
   * @param index zero-based position to insert before
   * @param bool  value to insert
   * @return this array for chaining
   */
  default ArrayValue<V> insert (int index, boolean bool) {

    return insert(index, getFactory().booleanValue(bool));
  }

  /**
   * Appends an integer to the end of the array.
   *
   * @param i value to append
   * @return this array for chaining
   */
  default ArrayValue<V> add (int i) {

    return add(getFactory().numberValue(i));
  }

  /**
   * Replaces the element at {@code index} with an integer.
   *
   * @param index zero-based position to replace
   * @param i     replacement value
   * @return this array for chaining
   */
  default ArrayValue<V> set (int index, int i) {

    return set(index, getFactory().numberValue(i));
  }

  /**
   * Inserts an integer before the element at {@code index}, shifting subsequent elements right.
   *
   * @param index zero-based position to insert before
   * @param i     value to insert
   * @return this array for chaining
   */
  default ArrayValue<V> insert (int index, int i) {

    return insert(index, getFactory().numberValue(i));
  }

  /**
   * Appends a long to the end of the array.
   *
   * @param l value to append
   * @return this array for chaining
   */
  default ArrayValue<V> add (long l) {

    return add(getFactory().numberValue(l));
  }

  /**
   * Replaces the element at {@code index} with a long.
   *
   * @param index zero-based position to replace
   * @param l     replacement value
   * @return this array for chaining
   */
  default ArrayValue<V> set (int index, long l) {

    return set(index, getFactory().numberValue(l));
  }

  /**
   * Inserts a long before the element at {@code index}, shifting subsequent elements right.
   *
   * @param index zero-based position to insert before
   * @param l     value to insert
   * @return this array for chaining
   */
  default ArrayValue<V> insert (int index, long l) {

    return insert(index, getFactory().numberValue(l));
  }

  /**
   * Appends a double to the end of the array.
   *
   * @param d value to append
   * @return this array for chaining
   */
  default ArrayValue<V> add (double d) {

    return add(getFactory().numberValue(d));
  }

  /**
   * Replaces the element at {@code index} with a double.
   *
   * @param index zero-based position to replace
   * @param d     replacement value
   * @return this array for chaining
   */
  default ArrayValue<V> set (int index, double d) {

    return set(index, getFactory().numberValue(d));
  }

  /**
   * Inserts a double before the element at {@code index}, shifting subsequent elements right.
   *
   * @param index zero-based position to insert before
   * @param d     value to insert
   * @return this array for chaining
   */
  default ArrayValue<V> insert (int index, double d) {

    return insert(index, getFactory().numberValue(d));
  }

  /**
   * Appends a string to the end of the array.
   *
   * @param text value to append
   * @return this array for chaining
   */
  default ArrayValue<V> add (String text) {

    return add(getFactory().textValue(text));
  }

  /**
   * Replaces the element at {@code index} with a string.
   *
   * @param index zero-based position to replace
   * @param text  replacement value
   * @return this array for chaining
   */
  default ArrayValue<V> set (int index, String text) {

    return set(index, getFactory().textValue(text));
  }

  /**
   * Inserts a string before the element at {@code index}, shifting subsequent elements right.
   *
   * @param index zero-based position to insert before
   * @param text  value to insert
   * @return this array for chaining
   */
  default ArrayValue<V> insert (int index, String text) {

    return insert(index, getFactory().textValue(text));
  }

  /**
   * Returns the number of elements in the array.
   *
   * @return element count, zero when empty
   */
  int size ();

  /**
   * Returns whether the array contains no elements.
   *
   * @return {@code true} if {@link #size()} is zero
   */
  boolean isEmpty ();

  /**
   * Returns the element at the specified index.
   *
   * @param index zero-based position to read
   * @return value at that position
   */
  Value<V> get (int index);

  /**
   * Appends the given value to the end of the array.
   *
   * @param value value to append
   * @param <U>   concrete value subtype
   * @return this array for chaining
   */
  <U extends Value<V>> ArrayValue<V> add (U value);

  /**
   * Replaces the element at the specified index with the given value.
   *
   * @param index zero-based position to replace
   * @param value replacement value
   * @param <U>   concrete value subtype
   * @return this array for chaining
   */
  <U extends Value<V>> ArrayValue<V> set (int index, U value);

  /**
   * Inserts the given value before the element at the specified index.
   *
   * @param index zero-based position to insert before
   * @param value value to insert
   * @param <U>   concrete value subtype
   * @return this array for chaining
   */
  <U extends Value<V>> ArrayValue<V> insert (int index, U value);

  /**
   * Removes and returns the element at the specified index.
   *
   * @param index zero-based position to remove
   * @return the value that was removed
   */
  Value<V> remove (int index);

  /**
   * Appends all values in the collection to the end of the array.
   *
   * @param values values to append
   * @param <U>    concrete value subtype
   * @return this array for chaining
   */
  <U extends Value<V>> ArrayValue<V> addAll (Collection<U> values);

  /**
   * Removes all elements from the array.
   *
   * @return this array for chaining
   */
  ArrayValue<V> removeAll ();
}
