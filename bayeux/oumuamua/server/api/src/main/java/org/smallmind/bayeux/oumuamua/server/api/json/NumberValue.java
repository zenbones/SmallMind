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

/**
 * JSON numeric value within the Bayeux value hierarchy, carrying an integer, long, or double
 * depending on the precision of the underlying representation.
 *
 * @param <V> concrete value subtype used by the enclosing codec
 */
public interface NumberValue<V extends Value<V>> extends Value<V> {

  /**
   * Returns {@link ValueType#NUMBER}, identifying this value as a JSON number.
   *
   * @return {@link ValueType#NUMBER}
   */
  default ValueType getType () {

    return ValueType.NUMBER;
  }

  /**
   * Returns the precision category of the number held by this value.
   *
   * @return {@link NumberType#INTEGER}, {@link NumberType#LONG}, or {@link NumberType#DOUBLE}
   */
  NumberType getNumberType ();

  /**
   * Returns the value as a boxed {@link Number} without narrowing conversion.
   *
   * @return boxed numeric value in its natural type
   */
  Number asNumber ();

  /**
   * Returns the value narrowed to a primitive {@code int}, truncating if necessary.
   *
   * @return int representation of the stored number
   */
  int asInt ();

  /**
   * Returns the value widened or narrowed to a primitive {@code long}.
   *
   * @return long representation of the stored number
   */
  long asLong ();

  /**
   * Returns the value widened or narrowed to a primitive {@code double}.
   *
   * @return double representation of the stored number
   */
  double asDouble ();
}
