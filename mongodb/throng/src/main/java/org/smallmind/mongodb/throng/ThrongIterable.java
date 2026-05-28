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
package org.smallmind.mongodb.throng;

import java.util.LinkedList;
import java.util.List;
import com.mongodb.client.MongoIterable;
import org.smallmind.mongodb.throng.mapping.ThrongEntityCodec;

/**
 * Lazy {@link Iterable} over driver results that decodes each raw {@link ThrongDocument} into a typed entity
 * on demand using the supplied codec. Backed by any {@link MongoIterable}, so the same wrapper serves both
 * {@code find} and {@code aggregate} call sites. The returned {@link ThrongIterator} is {@link AutoCloseable},
 * so callers iterating directly should use try-with-resources to release the underlying server-side cursor.
 *
 * @param <T> the entity type yielded by iteration
 */
public class ThrongIterable<T> implements Iterable<T> {

  private final MongoIterable<ThrongDocument> mongoIterable;
  private final ThrongEntityCodec<T> codec;

  /**
   * Constructs an iterable backed by the given driver result and entity codec.
   *
   * @param mongoIterable the driver iterable that yields raw {@link ThrongDocument} values
   * @param codec         the codec used to decode each document into an entity instance
   */
  public ThrongIterable (MongoIterable<ThrongDocument> mongoIterable, ThrongEntityCodec<T> codec) {

    this.mongoIterable = mongoIterable;
    this.codec = codec;
  }

  /**
   * Drains the entire result set into a {@link List}. The underlying cursor is fully consumed and
   * therefore released by the driver before this method returns.
   *
   * @return a list of all decoded entity instances
   */
  public List<T> asList () {

    LinkedList<T> list = new LinkedList<>();

    try (ThrongIterator<T> iterator = iterator()) {
      iterator.forEachRemaining(list::add);
    }

    return list;
  }

  /**
   * Returns an iterator that decodes {@link ThrongDocument} values into typed entities one at a time.
   * The returned iterator is {@link AutoCloseable} and should be closed once iteration is complete to
   * release the underlying server-side cursor.
   *
   * @return an iterator over the decoded entities
   */
  @Override
  public ThrongIterator<T> iterator () {

    return new ThrongIterator<>(mongoIterable.iterator(), codec);
  }
}
