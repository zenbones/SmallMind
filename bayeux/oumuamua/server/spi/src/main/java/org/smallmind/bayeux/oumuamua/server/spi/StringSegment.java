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
package org.smallmind.bayeux.oumuamua.server.spi;

import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Segment;

/**
 * Concrete {@link Segment} backed by a string literal.
 */
public class StringSegment extends Segment {

  private static final StringSegment WILD_SEGMENT = new StringSegment(Channel.WILD);
  private static final StringSegment DEEP_WILD_SEGMENT = new StringSegment(Channel.DEEP_WILD);
  private final String name;

  /**
   * Creates a segment with the provided text.
   *
   * @param name segment text
   */
  public StringSegment (String name) {

    if (name == null) {
      throw new NullPointerException();
    } else {

      this.name = name;
    }
  }

  /**
   * @return shared instance representing a single-level wildcard
   */
  public static StringSegment wild () {

    return WILD_SEGMENT;
  }

  /**
   * @return shared instance representing a deep wildcard
   */
  public static StringSegment deepWild () {

    return DEEP_WILD_SEGMENT;
  }


  /**
   * Compares the segment to a character sequence for equality.
   *
   * @param charSequence sequence to match
   * @return {@code true} if identical
   */
  @Override
  public boolean matches (CharSequence charSequence) {

    if ((charSequence == null) || (name.length() != charSequence.length())) {

      return false;
    } else {
      for (int pos = 0; pos < name.length(); pos++) {
        if (name.charAt(pos) != charSequence.charAt(pos)) {

          return false;
        }
      }

      return true;
    }
  }


  /**
   * @return the literal value of the segment
   */
  @Override
  public String toString () {

    return name;
  }

  /**
   * @return length of the segment
   */
  @Override
  public int length () {

    return name.length();
  }

  /**
   * Retrieves a character by index.
   *
   * @param index character index
   * @return character at the index
   */
  @Override
  public char charAt (int index) {

    return name.charAt(index);
  }

  /**
   * Returns a subsequence of the segment text.
   *
   * @param start start offset
   * @param end end offset
   * @return subsequence text
   */
  @Override
  public CharSequence subSequence (int start, int end) {

    return name.substring(start, end);
  }
}
