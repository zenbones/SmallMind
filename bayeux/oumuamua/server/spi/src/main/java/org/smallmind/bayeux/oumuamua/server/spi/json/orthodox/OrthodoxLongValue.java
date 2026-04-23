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
 * Immutable {@link NumberValue} wrapping a 64-bit signed long scalar for the orthodox codec;
 * reports {@link NumberType#LONG}, truncates when narrowed to int, and promotes when widened to double.
 */
public class OrthodoxLongValue extends OrthodoxValue implements NumberValue<OrthodoxValue> {

  private final long value;

  /**
   * Constructs a long value associated with the given factory.
   *
   * @param factory the {@link OrthodoxValueFactory} that owns this value
   * @param value   the primitive long to wrap
   */
  protected OrthodoxLongValue (OrthodoxValueFactory factory, long value) {

    super(factory);

    this.value = value;
  }

  /**
   * Identifies the numeric subtype of this value.
   *
   * @return {@link NumberType#LONG}
   */
  @Override
  public NumberType getNumberType () {

    return NumberType.LONG;
  }

  /**
   * Returns the value boxed as a {@link Long}.
   *
   * @return boxed {@code Long} representation
   */
  @Override
  public Number asNumber () {

    return value;
  }

  /**
   * Returns the value narrowed to a primitive int by truncation of the high 32 bits.
   *
   * @return low 32 bits of the long value cast to int
   */
  @Override
  public int asInt () {

    return (int)value;
  }

  /**
   * Returns the raw wrapped primitive long.
   *
   * @return the long value as stored at construction
   */
  @Override
  public long asLong () {

    return value;
  }

  /**
   * Returns the value widened to a primitive double; values beyond 2^53 may lose precision.
   *
   * @return the long value promoted to double
   */
  @Override
  public double asDouble () {

    return (double)value;
  }

  /**
   * Writes the JSON numeric literal representation of the long to {@code writer}.
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
