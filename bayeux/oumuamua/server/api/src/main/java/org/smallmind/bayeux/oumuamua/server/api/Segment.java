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
package org.smallmind.bayeux.oumuamua.server.api;

/**
 * Single path component of a channel {@link Route}, supporting character-sequence matching
 * that may be literal or wildcard depending on the concrete subtype.
 */
public abstract class Segment implements CharSequence {

  /**
   * Evaluates whether this segment matches the given character sequence according to its
   * matching rules (literal equality, single-level wildcard, or deep wildcard).
   *
   * @param charSequence sequence to test against this segment
   * @return {@code true} if the sequence satisfies this segment's matching criterion
   */
  public abstract boolean matches (CharSequence charSequence);

  /**
   * Returns the literal text of this segment as it appears in the channel path.
   *
   * @return segment text
   */
  public abstract String toString ();

  /**
   * Computes a hash code derived from the characters of this segment.
   *
   * @return character-based hash code
   */
  @Override
  public int hashCode () {

    int hashCode = 0;

    for (int pos = 0; pos < length(); pos++) {
      hashCode += (hashCode * 31) + charAt(pos);
    }

    return hashCode;
  }

  /**
   * Returns whether another object is a {@link Segment} that this segment matches.
   *
   * @param obj object to compare
   * @return {@code true} if {@code obj} is a {@link Segment} and {@link #matches(CharSequence)} returns {@code true} for it
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof Segment) && matches((Segment)obj);
  }
}
