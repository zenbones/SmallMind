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

import org.smallmind.bayeux.oumuamua.server.api.json.ArrayValue;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.NullValue;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueFactory;

/**
 * {@link ValueFactory} that instantiates the full suite of orthodox value implementations,
 * serving as the single creation point for all {@link OrthodoxValue} subtypes within a codec session.
 */
public class OrthodoxValueFactory implements ValueFactory<OrthodoxValue> {

  /**
   * Allocates and returns a new, empty {@link OrthodoxObjectValue}.
   *
   * @return new mutable object value backed by a {@link java.util.HashMap}
   */
  @Override
  public ObjectValue<OrthodoxValue> objectValue () {

    return new OrthodoxObjectValue(this);
  }

  /**
   * Allocates and returns a new, empty {@link OrthodoxArrayValue}.
   *
   * @return new mutable array value backed by a {@link java.util.LinkedList}
   */
  @Override
  public ArrayValue<OrthodoxValue> arrayValue () {

    return new OrthodoxArrayValue(this);
  }

  /**
   * Creates an {@link OrthodoxTextValue} wrapping {@code text}.
   *
   * @param text the string to wrap; encoded with JSON escaping when serialized
   * @return new immutable string value
   */
  @Override
  public StringValue<OrthodoxValue> textValue (String text) {

    return new OrthodoxTextValue(this, text);
  }

  /**
   * Creates an {@link OrthodoxIntegerValue} wrapping {@code i}.
   *
   * @param i the primitive int to wrap
   * @return new immutable integer number value with {@link org.smallmind.bayeux.oumuamua.server.api.json.NumberType#INTEGER}
   */
  @Override
  public NumberValue<OrthodoxValue> numberValue (int i) {

    return new OrthodoxIntegerValue(this, i);
  }

  /**
   * Creates an {@link OrthodoxLongValue} wrapping {@code l}.
   *
   * @param l the primitive long to wrap
   * @return new immutable long number value with {@link org.smallmind.bayeux.oumuamua.server.api.json.NumberType#LONG}
   */
  @Override
  public NumberValue<OrthodoxValue> numberValue (long l) {

    return new OrthodoxLongValue(this, l);
  }

  /**
   * Creates an {@link OrthodoxDoubleValue} wrapping {@code d}.
   *
   * @param d the primitive double to wrap
   * @return new immutable double number value with {@link org.smallmind.bayeux.oumuamua.server.api.json.NumberType#DOUBLE}
   */
  @Override
  public NumberValue<OrthodoxValue> numberValue (double d) {

    return new OrthodoxDoubleValue(this, d);
  }

  /**
   * Creates an {@link OrthodoxBooleanValue} wrapping {@code bool}.
   *
   * @param bool the primitive boolean to wrap
   * @return new immutable boolean value encoding as {@code true} or {@code false}
   */
  @Override
  public BooleanValue<OrthodoxValue> booleanValue (boolean bool) {

    return new OrthodoxBooleanValue(this, bool);
  }

  /**
   * Creates an {@link OrthodoxNullValue} encoding the JSON literal {@code null}.
   *
   * @return new null value instance
   */
  @Override
  public NullValue<OrthodoxValue> nullValue () {

    return new OrthodoxNullValue(this);
  }
}
