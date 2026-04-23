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
package org.smallmind.memcached.cubby.response;

/**
 * Enumeration of the two-character response codes defined by the memcached meta protocol.
 *
 * <p>Each constant name is the exact two-character ASCII token sent by the server. The enum
 * provides helper methods to match raw bytes against a code and to test membership in a set of
 * codes, both of which are used heavily by {@link ResponseParser}.</p>
 *
 * <ul>
 *   <li>{@link #MN} &ndash; NOOP; echoed by the {@code mn} command.</li>
 *   <li>{@link #HD} &ndash; HIT/stored; indicates the operation succeeded.</li>
 *   <li>{@link #VA} &ndash; VALUE; the response carries a value payload.</li>
 *   <li>{@link #EN} &ndash; MISS; the requested key was not found.</li>
 *   <li>{@link #EX} &ndash; EXISTS; a CAS mismatch was detected.</li>
 *   <li>{@link #NF} &ndash; NOT_FOUND; the key does not exist for a CAS store.</li>
 *   <li>{@link #NS} &ndash; NOT_STORED; the item was not stored (non-error condition).</li>
 * </ul>
 */
public enum ResponseCode {

  // (NOOP), returned by the noop command
  MN,
  // (HIT), to indicate success
  HD,
  // (VALUE), followed by the value data
  VA,
  // (MISS), to indicate that the item with this key was not found
  EN,
  // (EXISTS), to indicate that the supplied CAS token does not match the stored item, or to indicate that the item you are trying to store with CAS semantics has been modified since you last fetched it
  EX,
  // (NOT_FOUND), to indicate that the item with this key was not found, or to indicate that the item you are trying to store with CAS semantics did not exist
  NF,
  // (NOT_STORED), to indicate the data was not stored, but not because of an error
  NS;

  /**
   * Tests whether the two raw bytes at the start of a response line match this code.
   *
   * <p>The comparison is byte-for-byte against the two characters of the constant's name.</p>
   *
   * @param first  the first byte of the response line
   * @param second the second byte of the response line
   * @return {@code true} if both bytes match the corresponding characters of this code's name
   */
  public boolean begins (byte first, byte second) {

    return (first == name().charAt(0)) && (second == name().charAt(1));
  }

  /**
   * Tests whether this code is present in the supplied varargs list.
   *
   * @param codes one or more codes to check membership against
   * @return {@code true} if this code equals at least one element in {@code codes}
   */
  public boolean in (ResponseCode... codes) {

    for (ResponseCode code : codes) {
      if (code.equals(this)) {
        return true;
      }
    }

    return false;
  }
}
