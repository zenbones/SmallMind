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
import org.smallmind.bayeux.oumuamua.server.api.json.NumberType;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;

/**
 * Immutable {@link NumberValue} wrapping a IEEE 754 double-precision floating-point scalar for the
 * orthodox codec; reports {@link NumberType#DOUBLE} and truncates when coerced to integer types.
 */
public class OrthodoxDoubleValue extends OrthodoxValue implements NumberValue<OrthodoxValue> {

  private final double value;

  /**
   * Constructs a double value associated with the given factory.
   *
   * @param factory the {@link OrthodoxValueFactory} that owns this value
   * @param value   the double-precision number to wrap
   */
  protected OrthodoxDoubleValue (OrthodoxValueFactory factory, double value) {

    super(factory);

    this.value = value;
  }

  /**
   * Identifies the numeric subtype of this value.
   *
   * @return {@link NumberType#DOUBLE}
   */
  @Override
  public NumberType getNumberType () {

    return NumberType.DOUBLE;
  }

  /**
   * Returns the value boxed as a {@link Double}.
   *
   * @return boxed {@code Double} representation
   */
  @Override
  public Number asNumber () {

    return value;
  }

  /**
   * Returns the value narrowed to a primitive int by truncation toward zero.
   *
   * @return integer part of the double, truncated
   */
  @Override
  public int asInt () {

    return (int)value;
  }

  /**
   * Returns the value narrowed to a primitive long by truncation toward zero.
   *
   * @return integer part of the double as a long, truncated
   */
  @Override
  public long asLong () {

    return (long)value;
  }

  /**
   * Returns the raw wrapped double value.
   *
   * @return the primitive double as stored at construction
   */
  @Override
  public double asDouble () {

    return value;
  }

  /**
   * Writes the JSON numeric literal representation of the double to {@code writer}.
   *
   * @param writer destination for the JSON output
   * @throws IOException if writing to {@code writer} fails
   */
  @Override
  public void encode (Writer writer)
    throws IOException {

    writer.write(String.valueOf(value));
  }
}
