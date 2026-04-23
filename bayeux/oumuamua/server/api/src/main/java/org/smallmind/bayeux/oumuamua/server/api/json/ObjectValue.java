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

import java.util.Iterator;

/**
 * Mutable ordered map of string-keyed {@link Value} entries corresponding to a JSON object,
 * with fluent primitive-convenience overloads for field assignment.
 *
 * @param <V> concrete value subtype held within this object
 */
public interface ObjectValue<V extends Value<V>> extends Value<V> {

  /**
   * Returns {@link ValueType#OBJECT}, identifying this value as a JSON object.
   *
   * @return {@link ValueType#OBJECT}
   */
  default ValueType getType () {

    return ValueType.OBJECT;
  }

  /**
   * Sets the named field to a boolean, replacing any existing value.
   *
   * @param field field name
   * @param bool  boolean value to store
   * @return this object for chaining
   */
  default ObjectValue<V> put (String field, boolean bool) {

    return put(field, getFactory().booleanValue(bool));
  }

  /**
   * Sets the named field to an integer, replacing any existing value.
   *
   * @param field field name
   * @param i     integer value to store
   * @return this object for chaining
   */
  default ObjectValue<V> put (String field, int i) {

    return put(field, getFactory().numberValue(i));
  }

  /**
   * Sets the named field to a long, replacing any existing value.
   *
   * @param field field name
   * @param l     long value to store
   * @return this object for chaining
   */
  default ObjectValue<V> put (String field, long l) {

    return put(field, getFactory().numberValue(l));
  }

  /**
   * Sets the named field to a double, replacing any existing value.
   *
   * @param field field name
   * @param d     double value to store
   * @return this object for chaining
   */
  default ObjectValue<V> put (String field, double d) {

    return put(field, getFactory().numberValue(d));
  }

  /**
   * Sets the named field to a string, or to JSON {@code null} if {@code text} is {@code null},
   * replacing any existing value.
   *
   * @param field field name
   * @param text  string to store, or {@code null} to store a JSON null
   * @return this object for chaining
   */
  default ObjectValue<V> put (String field, String text) {

    return put(field, (text == null) ? getFactory().nullValue() : getFactory().textValue(text));
  }

  /**
   * Returns the number of fields present in this object.
   *
   * @return field count, zero when empty
   */
  int size ();

  /**
   * Returns whether this object contains no fields.
   *
   * @return {@code true} if {@link #size()} is zero
   */
  boolean isEmpty ();

  /**
   * Returns an iterator over the names of all fields in this object.
   *
   * @return field name iterator
   */
  Iterator<String> fieldNames ();

  /**
   * Returns the value associated with the named field.
   *
   * @param field field name to look up
   * @return stored value, or {@code null} if no field with that name exists
   */
  Value<V> get (String field);

  /**
   * Sets the named field to the given value, replacing any existing value.
   *
   * @param field field name
   * @param value value to store
   * @param <U>   concrete value subtype
   * @return this object for chaining
   */
  <U extends Value<V>> ObjectValue<V> put (String field, U value);

  /**
   * Removes the named field and returns its former value.
   *
   * @param field field name to remove
   * @return the removed value, or {@code null} if the field did not exist
   */
  Value<V> remove (String field);

  /**
   * Removes all fields from this object.
   *
   * @return this object for chaining
   */
  ObjectValue<V> removeAll ();
}
