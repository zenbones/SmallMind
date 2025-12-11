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
package org.smallmind.bayeux.oumuamua.server.api;

/**
 * Represents a segment of a channel route with matching rules.
 */
public abstract class Segment implements CharSequence {

  /**
   * Determines whether this segment matches the provided character sequence.
   *
   * @param charSequence the sequence to test
   * @return {@code true} if the segment matches
   */
  public abstract boolean matches (CharSequence charSequence);

  /**
   * @return the literal representation of the segment
   */
  public abstract String toString ();

  /**
   * Computes a hash code based on the characters in the segment.
   *
   * @return hash code for the segment
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
   * Compares another object for equality based on segment matching rules.
   *
   * @param obj object to compare
   * @return {@code true} if the object is a segment with equivalent content
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof Segment) && matches((Segment)obj);
  }
}
