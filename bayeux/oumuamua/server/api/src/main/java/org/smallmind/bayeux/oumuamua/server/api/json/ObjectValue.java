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

import java.util.Iterator;

/**
 * Mutable JSON object value with helpers for primitive assignment.
 */
public interface ObjectValue<V extends Value<V>> extends Value<V> {

  /**
   * Identifies this value as an object.
   *
   * @return {@link ValueType#OBJECT}
   */
  default ValueType getType () {

    return ValueType.OBJECT;
  }

  /**
   * Adds or replaces a boolean field.
   *
   * @param field field name
   * @param bool  value to store
   * @return this object
   */
  default ObjectValue<V> put (String field, boolean bool) {

    return put(field, getFactory().booleanValue(bool));
  }

  /**
   * Adds or replaces an integer field.
   *
   * @param field field name
   * @param i     value to store
   * @return this object
   */
  default ObjectValue<V> put (String field, int i) {

    return put(field, getFactory().numberValue(i));
  }

  /**
   * Adds or replaces a long field.
   *
   * @param field field name
   * @param l     value to store
   * @return this object
   */
  default ObjectValue<V> put (String field, long l) {

    return put(field, getFactory().numberValue(l));
  }

  /**
   * Adds or replaces a double field.
   *
   * @param field field name
   * @param d     value to store
   * @return this object
   */
  default ObjectValue<V> put (String field, double d) {

    return put(field, getFactory().numberValue(d));
  }

  /**
   * Adds or replaces a string field (or null).
   *
   * @param field field name
   * @param text  value to store; {@code null} results in a JSON null
   * @return this object
   */
  default ObjectValue<V> put (String field, String text) {

    return put(field, (text == null) ? getFactory().nullValue() : getFactory().textValue(text));
  }

  /**
   * @return number of fields present
   */
  int size ();

  /**
   * @return {@code true} if no fields are present
   */
  boolean isEmpty ();

  /**
   * @return iterator over field names
   */
  Iterator<String> fieldNames ();

  /**
   * Retrieves a field value.
   *
   * @param field field name
   * @return the stored value or {@code null} if missing
   */
  Value<V> get (String field);

  /**
   * Adds or replaces a field with a value.
   *
   * @param field field name
   * @param value value to store
   * @return this object
   */
  <U extends Value<V>> ObjectValue<V> put (String field, U value);

  /**
   * Removes a field.
   *
   * @param field field name to remove
   * @return the removed value or {@code null} if absent
   */
  Value<V> remove (String field);

  /**
   * Removes all fields from this object.
   *
   * @return this object
   */
  ObjectValue<V> removeAll ();
}
