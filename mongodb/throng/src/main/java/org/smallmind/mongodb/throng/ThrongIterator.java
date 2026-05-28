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

import java.util.Iterator;
import com.mongodb.client.MongoCursor;
import org.smallmind.mongodb.throng.mapping.ThrongEntityCodec;

/**
 * Cursor-backed iterator that translates each {@link ThrongDocument} produced by the driver into a typed
 * entity instance via the supplied {@link ThrongEntityCodec}, and exposes the underlying cursor's lifetime
 * as an {@link AutoCloseable} so that callers can release server-side cursor resources deterministically.
 *
 * <p>The {@link #close()} method is idempotent: calling it more than once is safe and only releases the
 * underlying cursor on the first call.
 *
 * @param <T> the entity type yielded by iteration
 */
public class ThrongIterator<T> implements Iterator<T>, AutoCloseable {

  private final MongoCursor<ThrongDocument> mongoCursor;
  private final ThrongEntityCodec<T> codec;
  private boolean closed;

  /**
   * Constructs an iterator wrapping the given driver cursor and entity codec.
   *
   * @param mongoCursor driver cursor over raw {@link ThrongDocument} values
   * @param codec       codec used to translate each document into a typed entity
   */
  public ThrongIterator (MongoCursor<ThrongDocument> mongoCursor, ThrongEntityCodec<T> codec) {

    this.mongoCursor = mongoCursor;
    this.codec = codec;
  }

  /**
   * Returns {@code true} if the underlying cursor has more documents.
   *
   * @return {@code true} when more results are available
   */
  @Override
  public boolean hasNext () {

    return mongoCursor.hasNext();
  }

  /**
   * Advances the cursor and returns the next decoded entity, firing any registered
   * {@code @PreLoad} and {@code @PostLoad} lifecycle callbacks for the entity type.
   *
   * @return the next entity decoded from the cursor
   */
  @Override
  public T next () {

    return TranslationUtility.fromBson(codec, mongoCursor.next().getBsonDocument());
  }

  /**
   * Closes the underlying cursor if not already closed. Safe to call multiple times.
   */
  @Override
  public void close () {

    if (!closed) {
      closed = true;
      mongoCursor.close();
    }
  }
}
