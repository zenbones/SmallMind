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
package org.smallmind.mongodb.throng;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.smallmind.mongodb.throng.mapping.ThrongEntityCodec;

public class ThrongIterable<T> implements Iterable<T> {

  private final FindIterable<ThrongDocument> findIterable;
  private final ThrongEntityCodec<T> codec;

  public ThrongIterable (FindIterable<ThrongDocument> findIterable, ThrongEntityCodec<T> codec) {

    this.findIterable = findIterable;
    this.codec = codec;
  }

  public List<T> asList () {

    LinkedList<T> list = new LinkedList<>();

    iterator().forEachRemaining(list::add);

    return list;
  }

  @Override
  public Iterator<T> iterator () {

    return new ThrongIterator(findIterable.iterator());
  }

  private class ThrongIterator implements Iterator<T> {

    private final MongoCursor<ThrongDocument> mongoCursor;

    public ThrongIterator (MongoCursor<ThrongDocument> mongoCursor) {

      this.mongoCursor = mongoCursor;
    }

    @Override
    public boolean hasNext () {

      return mongoCursor.hasNext();
    }

    @Override
    public T next () {

      return TranslationUtility.fromBson(codec, mongoCursor.next().getBsonDocument());
    }
  }
}
