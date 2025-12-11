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
 * Defines a channel route broken into path segments with matching helpers.
 */
public interface Route {

  /**
   * @return the channel path string
   */
  String getPath ();

  /**
   * @return number of segments in the route
   */
  int size ();

  /**
   * @return the last valid segment index
   */
  int lastIndex ();

  /**
   * Retrieves a segment by index.
   *
   * @param index position of the segment
   * @return the segment at the given index
   */
  Segment getSegment (int index);

  /**
   * @return {@code true} if the route ends in a single-level wildcard
   */
  boolean isWild ();

  /**
   * @return {@code true} if the route ends in a deep wildcard
   */
  boolean isDeepWild ();

  /**
   * @return {@code true} if the route references a meta channel
   */
  boolean isMeta ();

  /**
   * @return {@code true} if the route references a service channel
   */
  boolean isService ();

  /**
   * Evaluates whether the provided path segments match this route.
   *
   * @param segments the segments to test
   * @return {@code true} if they match
   */
  boolean matches (String... segments);

  /**
   * Indicates whether the route can accept user messages.
   *
   * @return {@code true} when deliverable
   */
  default boolean isDeliverable () {

    return !(isWild() || isDeepWild() || isMeta() || isService());
  }
}
