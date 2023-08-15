/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.util.LinkedList;

public interface Route extends Iterable<String> {

  String getPath ();

  int size ();

  int lastIndex ();

  int separatorPos (int index);

  default String getSegment (int index) {

    if ((index < 0) || (index > lastIndex())) {
      throw new IndexOutOfBoundsException("0 >= index < " + size());
    } else {

      return getPath().substring((index == 0) ? 1 : separatorPos(index) + 1, (index < lastIndex()) ? separatorPos(index + 1) : getPath().length());
    }
  }

  default boolean matchesSegment (int index, String subPath) {

    if ((index < 0) || (index > lastIndex())) {
      throw new IndexOutOfBoundsException("0 >= index < " + size());
    } else {

      int startPos = (index == 0) ? 1 : separatorPos(index) + 1;

      if ((subPath == null) || (subPath.length() != (((index < lastIndex()) ? separatorPos(index + 1) : getPath().length()) - startPos))) {

        return false;
      } else {
        for (int pos = 0; pos < subPath.length(); pos++) {
          if (subPath.charAt(pos) != getPath().charAt(startPos + pos)) {

            return false;
          }
        }

        return true;
      }
    }
  }

  default boolean isWild () {

    return matchesSegment(lastIndex(), "*");
  }

  default boolean isDeepWild () {

    return matchesSegment(lastIndex(), "**");
  }

  default boolean isMeta () {

    return matchesSegment(0, "meta");
  }

  default boolean isService () {

    return matchesSegment(0, "service");
  }

  default boolean isDeliverable () {

    return !(isWild() || isDeepWild() || isMeta() || isService());
  }

  default int[] validate (String path)
    throws IllegalPathException {

    if ((path == null) || (path.length() < 2) || (path.charAt(0) != '/')) {
      throw new IllegalPathException("Path(%s) must start with a '/' and specify at least one segment");
    } else {

      LinkedList<Integer> segmentList = new LinkedList<>();
      int startIndex = 1;
      int asterisks = 0;

      for (int index = 1; index < path.length(); index++) {

        char c;

        if ((c = path.charAt(index)) == '/') {
          if ((index - startIndex) == 0) {
            throw new IllegalPathException("Path(%s) must not contain empty segments");
          } else if (asterisks > 0) {
            throw new IllegalPathException("Path(%s) uses an illegal wildcard '*' definition");
          } else {
            segmentList.add(index);
            startIndex = index + 1;
            asterisks = 0;
          }
        } else if (c == '*') {
          asterisks++;
        } else if (!((c >= 'A' && c <= 'Z') ||
                       (c >= 'a' && c <= 'z') ||
                       (c >= '0' && c <= '9') ||
                       (" !#$()+-.@_{}~".indexOf(c) >= 0))) {
          throw new IllegalPathException("Path(%s) has illegal characters");
        }
      }

      if ((path.length() - startIndex) == 0) {
        throw new IllegalPathException("Path(%s) must not contain empty segments");
      } else if (((asterisks > 0) && (asterisks < (path.length() - startIndex))) || (asterisks > 2)) {
        throw new IllegalPathException("Path(%s) uses an illegal wildcard '*' definition");
      } else {

        int[] segments = new int[segmentList.size()];
        int index = 0;

        for (int segment : segmentList) {
          segments[index++] = segment;
        }

        return segments;
      }
    }
  }
}
