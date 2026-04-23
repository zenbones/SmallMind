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
 * Parsed representation of a Bayeux channel path, providing segment-level access and
 * predicate methods for classifying the route.
 */
public interface Route {

  /**
   * Returns the full channel path string for this route.
   *
   * @return channel path (e.g. {@code /foo/bar})
   */
  String getPath ();

  /**
   * Returns the number of path segments in this route.
   *
   * @return segment count, always at least one
   */
  int size ();

  /**
   * Returns the index of the last segment ({@code size() - 1}).
   *
   * @return zero-based index of the final segment
   */
  int lastIndex ();

  /**
   * Returns the segment at the specified position.
   *
   * @param index zero-based segment index
   * @return segment at that position
   */
  Segment getSegment (int index);

  /**
   * Returns whether the final segment is a single-level wildcard ({@code *}).
   *
   * @return {@code true} if the route ends with {@code *}
   */
  boolean isWild ();

  /**
   * Returns whether the final segment is a deep wildcard ({@code **}).
   *
   * @return {@code true} if the route ends with {@code **}
   */
  boolean isDeepWild ();

  /**
   * Returns whether the route begins with {@code /meta/}.
   *
   * @return {@code true} for meta channels
   */
  boolean isMeta ();

  /**
   * Returns whether the route begins with {@code /service/}.
   *
   * @return {@code true} for service channels
   */
  boolean isService ();

  /**
   * Tests whether the given path segments match this route.
   *
   * @param segments path segments to evaluate against this route's pattern
   * @return {@code true} if the segments satisfy the route's matching rules
   */
  boolean matches (String... segments);

  /**
   * Returns whether user messages can be published and delivered on this route.
   * A route is deliverable when it is not wild, deep wild, meta, or service.
   *
   * @return {@code true} if the route accepts user publications
   */
  default boolean isDeliverable () {

    return !(isWild() || isDeepWild() || isMeta() || isService());
  }
}
