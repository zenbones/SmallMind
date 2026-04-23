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

import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Segment;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

/**
 * Standard {@link Route} implementation that parses and validates a Bayeux channel path,
 * exposing segment-level access and wildcard matching for channel resolution.
 */
public class DefaultRoute implements Route {

  public static final DefaultRoute HANDSHAKE_ROUTE;
  public static final DefaultRoute CONNECT_ROUTE;
  public static final DefaultRoute DISCONNECT_ROUTE;
  public static final DefaultRoute SUBSCRIBE_ROUTE;
  public static final DefaultRoute UNSUBSCRIBE_ROUTE;

  private final String path;
  private final int[] segments;

  static {
    try {
      HANDSHAKE_ROUTE = new DefaultRoute("/meta/handshake");
      CONNECT_ROUTE = new DefaultRoute("/meta/connect");
      DISCONNECT_ROUTE = new DefaultRoute("/meta/disconnect");
      SUBSCRIBE_ROUTE = new DefaultRoute("/meta/subscribe");
      UNSUBSCRIBE_ROUTE = new DefaultRoute("/meta/unsubscribe");
    } catch (InvalidPathException invalidPathException) {
      throw new StaticInitializationError(invalidPathException);
    }
  }

  /**
   * Parses {@code path} into segments and validates its conformance to Bayeux channel rules.
   *
   * @param path channel path; must begin with {@code '/'} and contain at least one segment
   * @throws InvalidPathException if the path is null, empty, missing a leading slash,
   *                              contains empty segments, has illegal characters, or
   *                              uses a malformed wildcard expression
   */
  public DefaultRoute (String path)
    throws InvalidPathException {

    this.path = path;

    segments = PathValidator.validate(path);
  }

  /**
   * Returns the full channel path string as provided at construction.
   *
   * @return path string beginning with {@code '/'}
   */
  public String getPath () {

    return path;
  }

  /**
   * Returns the total number of segments in this route.
   *
   * @return segment count; always at least 1
   */
  public int size () {

    return segments.length + 1;
  }

  /**
   * Returns the zero-based index of the last segment.
   *
   * @return index of the trailing segment
   */
  public int lastIndex () {

    return segments.length;
  }

  /**
   * Indicates whether the final segment is a single-level wildcard ({@code *}).
   *
   * @return {@code true} if the last segment equals {@code *}
   */
  public boolean isWild () {

    return matches(segments.length, "*");
  }

  /**
   * Indicates whether the final segment is a deep wildcard ({@code **}).
   *
   * @return {@code true} if the last segment equals {@code **}
   */
  public boolean isDeepWild () {

    return matches(segments.length, "**");
  }

  /**
   * Indicates whether this path belongs to the {@code /meta} namespace.
   *
   * @return {@code true} if the first segment is {@code meta}
   */
  public boolean isMeta () {

    return matches(0, "meta");
  }

  /**
   * Indicates whether this path belongs to the {@code /service} namespace.
   *
   * @return {@code true} if the first segment is {@code service}
   */
  public boolean isService () {

    return matches(0, "service");
  }

  /**
   * Tests whether this route matches the given sequence of segment names, respecting
   * single-level ({@code *}) and deep ({@code **}) wildcards in {@code matchingSegments}.
   *
   * @param matchingSegments ordered segment names to test; a {@code **} entry matches all remaining segments
   * @return {@code true} if every supplied segment matches the corresponding segment of this route
   */
  @Override
  public boolean matches (String... matchingSegments) {

    if ((matchingSegments == null) || (matchingSegments.length > (segments.length + 1))) {

      return false;
    } else {

      int index = 0;

      for (String matchingSegment : matchingSegments) {
        if ("**".equals(matchingSegment)) {

          return true;
        } else if ((!"*".equals(matchingSegment)) && (!matches(index, matchingSegment))) {

          return false;
        } else {
          index++;
        }
      }

      return true;
    }
  }

  /**
   * Tests whether the segment at {@code index} exactly equals {@code name} by comparing
   * characters directly against the underlying path string.
   *
   * @param index zero-based segment index within this route
   * @param name  character sequence to compare; {@code null} always returns {@code false}
   * @return {@code true} if the segment text equals {@code name} character-for-character
   * @throws IndexOutOfBoundsException if {@code index} is negative or greater than {@link #lastIndex()}
   */
  protected boolean matches (int index, CharSequence name) {

    if ((index < 0) || (index > segments.length)) {
      throw new IndexOutOfBoundsException("0 <= index < " + size());
    } else {

      int startPos = (index == 0) ? 1 : segments[index - 1] + 1;

      if ((name == null) || (name.length() != (((index < segments.length) ? segments[index] : getPath().length()) - startPos))) {

        return false;
      } else {
        for (int pos = 0; pos < name.length(); pos++) {
          if (name.charAt(pos) != path.charAt(startPos + pos)) {

            return false;
          }
        }

        return true;
      }
    }
  }

  /**
   * Returns a {@link Segment} view backed by this route for the given index.
   *
   * @param index zero-based segment index
   * @return segment wrapping the path characters at that position
   * @throws IndexOutOfBoundsException if {@code index} is negative or greater than {@link #lastIndex()}
   */
  @Override
  public Segment getSegment (int index) {

    if ((index < 0) || (index > segments.length)) {
      throw new IndexOutOfBoundsException("0 <= index < " + size());
    } else {

      return new RouteSegment(index);
    }
  }

  @Override
  public int hashCode () {

    return path.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof DefaultRoute) && ((DefaultRoute)obj).path.equals(path);
  }

  /**
   * {@link Segment} view into a single path segment of its enclosing {@link DefaultRoute},
   * delegating character access and matching directly to the parent path string.
   */
  public class RouteSegment extends Segment {

    private final int index;

    private RouteSegment (int index) {

      this.index = index;
    }

    /**
     * Delegates matching to {@link DefaultRoute#matches(int, CharSequence)} for this segment's index.
     *
     * @param charSequence character sequence to compare against this segment
     * @return {@code true} if the segment text equals {@code charSequence} character-for-character
     */
    @Override
    public boolean matches (CharSequence charSequence) {

      return DefaultRoute.this.matches(index, charSequence);
    }

    /**
     * Returns the number of characters in this segment.
     *
     * @return character count, excluding any delimiter slashes
     */
    @Override
    public int length () {

      return ((index < segments.length) ? segments[index] : path.length()) - ((index == 0) ? 1 : segments[index - 1] + 1);
    }

    /**
     * Returns the character at {@code pos} within this segment.
     *
     * @param pos zero-based position within the segment
     * @return the character at that position
     * @throws StringIndexOutOfBoundsException if {@code pos} is out of bounds for this segment
     */
    @Override
    public char charAt (int pos) {

      int startPos = (index == 0) ? 1 : segments[index - 1] + 1;
      int endPos = (index < segments.length) ? segments[index] : path.length();

      if ((pos < 0) || (pos >= (endPos - startPos))) {
        throw new StringIndexOutOfBoundsException("0 <= index < " + (endPos - startPos));
      } else {

        return path.charAt(startPos + pos);
      }
    }

    /**
     * Returns the sub-sequence of this segment between {@code start} (inclusive) and {@code end} (exclusive).
     *
     * @param start start offset within the segment, inclusive
     * @param end   end offset within the segment, exclusive
     * @return subsequence as a {@link String}
     * @throws StringIndexOutOfBoundsException if {@code start} or {@code end} is out of range
     */
    @Override
    public CharSequence subSequence (int start, int end) {

      int startPos = (index == 0) ? 1 : segments[index - 1] + 1;
      int endPos = (index < segments.length) ? segments[index] : path.length();

      if ((start < 0) || (end >= (endPos - startPos))) {
        throw new StringIndexOutOfBoundsException("0 <= index < " + (endPos - startPos));
      } else {

        return path.substring(startPos + start, startPos + end);
      }
    }

    /**
     * Returns the full text of this segment as a plain string.
     *
     * @return segment text extracted from the parent path, without surrounding slashes
     */
    @Override
    public String toString () {

      return path.substring((index == 0) ? 1 : segments[index - 1] + 1, (index < segments.length) ? segments[index] : path.length());
    }
  }
}
