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
 * Allocation point for all concrete {@link Value} subtypes used within a single codec binding;
 * implementations are codec-specific and produce values that can be freely combined in messages.
 *
 * @param <V> concrete value subtype produced by this factory
 */
public interface ValueFactory<V extends Value<V>> {

  /**
   * Allocates a new, empty JSON object value.
   *
   * @return new mutable object with no fields
   */
  ObjectValue<V> objectValue ();

  /**
   * Allocates a new, empty JSON array value.
   *
   * @return new mutable array with no elements
   */
  ArrayValue<V> arrayValue ();

  /**
   * Creates a JSON string value wrapping the given text.
   *
   * @param text string content to wrap
   * @return new string value
   */
  StringValue<V> textValue (String text);

  /**
   * Creates a JSON number value backed by a 32-bit integer.
   *
   * @param i integer to wrap
   * @return new numeric value with {@link NumberType#INTEGER} backing
   */
  NumberValue<V> numberValue (int i);

  /**
   * Creates a JSON number value backed by a 64-bit long.
   *
   * @param l long to wrap
   * @return new numeric value with {@link NumberType#LONG} backing
   */
  NumberValue<V> numberValue (long l);

  /**
   * Creates a JSON number value backed by a 64-bit double.
   *
   * @param d double to wrap
   * @return new numeric value with {@link NumberType#DOUBLE} backing
   */
  NumberValue<V> numberValue (double d);

  /**
   * Creates a JSON boolean value.
   *
   * @param bool boolean to wrap
   * @return new boolean value
   */
  BooleanValue<V> booleanValue (boolean bool);

  /**
   * Returns the shared JSON null literal for this factory.
   *
   * @return null value singleton (or equivalent)
   */
  NullValue<V> nullValue ();
}
