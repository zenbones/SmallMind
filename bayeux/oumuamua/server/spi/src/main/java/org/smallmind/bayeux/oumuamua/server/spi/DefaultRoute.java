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
package org.smallmind.bayeux.oumuamua.server.spi;

import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Route;

public class DefaultRoute implements Route {

  private final String path;
  private final int[] segments;

  public DefaultRoute (String path)
    throws InvalidPathException {

    this.path = path;

    segments = PathValidator.validate(path);
  }

  public String getPath () {

    return path;
  }

  public int size () {

    return segments.length + 1;
  }

  public int lastIndex () {

    return segments.length;
  }

  public boolean isWild () {

    return matches(segments.length, "*");
  }

  public boolean isDeepWild () {

    return matches(segments.length, "**");
  }

  public boolean isMeta () {

    return matches(0, "meta");
  }

  public boolean isService () {

    return matches(0, "service");
  }

  public boolean isDeliverable () {

    return !(isWild() || isDeepWild() || isMeta() || isService());
  }

  public boolean matches (int index, CharSequence name) {

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

  public RouteSegment getSegment (int index) {

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

  public class RouteSegment extends Segment {

    private final int index;

    private RouteSegment (int index) {

      this.index = index;
    }

    @Override
    public boolean matches (Segment segment) {

      return DefaultRoute.this.matches(index, segment);
    }

    @Override
    public int length () {

      return ((index < segments.length) ? segments[index] : path.length()) - ((index == 0) ? 1 : segments[index - 1] + 1);
    }

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

    @Override
    public String toString () {

      return path.substring((index == 0) ? 1 : segments[index - 1] + 1, (index < segments.length) ? segments[index] : path.length());
    }
  }
}
