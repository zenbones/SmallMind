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
 * Factory for constructing JSON value instances used in Bayeux messages.
 *
 * @param <V> concrete value subtype produced
 */
public interface ValueFactory<V extends Value<V>> {

  /**
   * @return a new empty object value
   */
  ObjectValue<V> objectValue ();

  /**
   * @return a new empty array value
   */
  ArrayValue<V> arrayValue ();

  /**
   * Creates a text value.
   *
   * @param text text content
   * @return a new string value
   */
  StringValue<V> textValue (String text);

  /**
   * Creates a numeric value from an integer.
   *
   * @param i integer content
   * @return a numeric value
   */
  NumberValue<V> numberValue (int i);

  /**
   * Creates a numeric value from a long.
   *
   * @param l long content
   * @return a numeric value
   */
  NumberValue<V> numberValue (long l);

  /**
   * Creates a numeric value from a double.
   *
   * @param d double content
   * @return a numeric value
   */
  NumberValue<V> numberValue (double d);

  /**
   * Creates a boolean value.
   *
   * @param bool boolean content
   * @return a boolean value
   */
  BooleanValue<V> booleanValue (boolean bool);

  /**
   * @return the singleton null value
   */
  NullValue<V> nullValue ();
}
