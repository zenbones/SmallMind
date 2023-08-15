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

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.smallmind.bayeux.oumuamua.server.api.IllegalPathException;
import org.smallmind.bayeux.oumuamua.server.api.Route;

public class DefaultRoute implements Route {

  private final String path;
  private final int[] segments;

  public DefaultRoute (String path)
    throws IllegalPathException {

    this.path = path;

    segments = validate(path);
  }

  @Override
  public String getPath () {

    return path;
  }

  @Override
  public int size () {

    return segments.length + 1;
  }

  @Override
  public int lastIndex () {

    return segments.length;
  }

  @Override
  public int separatorPos (int index) {

    if ((index < 0) || (index > segments.length)) {
      throw new IndexOutOfBoundsException("0 >= index < " + (segments.length + 1));
    } else {

      return segments[index];
    }
  }

  @Override
  public Iterator<String> iterator () {

    return new SegmentIterator();
  }

  private class SegmentIterator implements Iterator<String> {

    private int index = 0;

    @Override
    public boolean hasNext () {

      return index < size();
    }

    @Override
    public String next () {

      if (index >= size()) {

        throw new NoSuchElementException();
      } else {

        return getSegment(index++);
      }
    }
  }
}
