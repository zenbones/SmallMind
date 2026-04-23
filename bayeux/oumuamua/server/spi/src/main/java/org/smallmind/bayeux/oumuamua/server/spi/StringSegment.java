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
package org.smallmind.bayeux.oumuamua.server.spi;

import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Segment;

/**
 * {@link Segment} implementation backed by a plain string literal, with shared singleton
 * instances for the single-level ({@code *}) and deep ({@code **}) Bayeux wildcards.
 */
public class StringSegment extends Segment {

  private static final StringSegment WILD_SEGMENT = new StringSegment(Channel.WILD);
  private static final StringSegment DEEP_WILD_SEGMENT = new StringSegment(Channel.DEEP_WILD);
  private final String name;

  /**
   * Constructs a segment wrapping the given text.
   *
   * @param name segment text; must not be null
   * @throws NullPointerException if {@code name} is null
   */
  public StringSegment (String name) {

    if (name == null) {
      throw new NullPointerException();
    } else {

      this.name = name;
    }
  }

  /**
   * Returns the shared single-level wildcard segment ({@code *}).
   *
   * @return singleton {@code StringSegment} whose text equals {@link Channel#WILD}
   */
  public static StringSegment wild () {

    return WILD_SEGMENT;
  }

  /**
   * Returns the shared deep wildcard segment ({@code **}).
   *
   * @return singleton {@code StringSegment} whose text equals {@link Channel#DEEP_WILD}
   */
  public static StringSegment deepWild () {

    return DEEP_WILD_SEGMENT;
  }

  /**
   * Tests whether this segment's text equals {@code charSequence} character-for-character.
   *
   * @param charSequence sequence to compare; null or different length always returns {@code false}
   * @return {@code true} if every character matches
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
   * Returns the segment text as a plain string.
   *
   * @return the literal string backing this segment
   */
  @Override
  public String toString () {

    return name;
  }

  /**
   * Returns the number of characters in the segment text.
   *
   * @return character count of the backing string
   */
  @Override
  public int length () {

    return name.length();
  }

  /**
   * Returns the character at the given index within the segment text.
   *
   * @param index zero-based character position
   * @return character at that position
   * @throws StringIndexOutOfBoundsException if {@code index} is out of range
   */
  @Override
  public char charAt (int index) {

    return name.charAt(index);
  }

  /**
   * Returns the sub-sequence of the segment text between {@code start} and {@code end}.
   *
   * @param start start index, inclusive
   * @param end   end index, exclusive
   * @return the requested substring
   * @throws StringIndexOutOfBoundsException if the indices are out of range
   */
  @Override
  public CharSequence subSequence (int start, int end) {

    return name.substring(start, end);
  }
}
