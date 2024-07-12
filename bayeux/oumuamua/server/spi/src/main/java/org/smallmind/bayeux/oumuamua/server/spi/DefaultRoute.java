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

import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Segment;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

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

  public class RouteSegment extends Segment {

    private final int index;

    private RouteSegment (int index) {

      this.index = index;
    }

    @Override
    public boolean matches (CharSequence charSequence) {

      return DefaultRoute.this.matches(index, charSequence);
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
