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
package org.smallmind.persistence.cache.aop;

import java.io.Serializable;
import java.util.Iterator;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;

/**
 * Lazy {@link Iterable} wrapper that passes each element through a {@link VectoredDao} as it is consumed,
 * ensuring every durable is recorded in the cache without forcing eager materialisation.
 *
 * @param <I> serializable and comparable identifier type of the durable
 * @param <D> durable entity type
 */
public class CacheCoherentIterable<I extends Serializable & Comparable<I>, D extends Durable<I>> implements Iterable<D> {

  private final Iterable<D> durableIterable;
  private final VectoredDao<I, D> vectoredDao;
  private final Class<D> durableClass;

  /**
   * Wraps an existing iterable with cache-coherent traversal.
   *
   * @param durableIterable source iterable of durable entities
   * @param durableClass    runtime class of the managed durable type
   * @param vectoredDao     cache-aware DAO used to persist each element
   */
  public CacheCoherentIterable (Iterable<D> durableIterable, Class<D> durableClass, VectoredDao<I, D> vectoredDao) {

    this.durableIterable = durableIterable;
    this.durableClass = durableClass;
    this.vectoredDao = vectoredDao;
  }

  /**
   * Returns an iterator that persists each element into the cache as it is retrieved.
   *
   * @return cache-coherent iterator over the wrapped durables
   */
  @Override
  public Iterator<D> iterator () {

    return new CacheCoherentIterator(durableIterable.iterator());
  }

  /**
   * Iterator that routes every call to {@link #next()} through the {@link VectoredDao} to enforce cache coherence.
   */
  private class CacheCoherentIterator implements Iterator<D> {

    private final Iterator<D> durableIter;

    /**
     * Constructs the iterator over the supplied underlying iterator.
     *
     * @param durableIter source iterator of raw durable elements
     */
    public CacheCoherentIterator (Iterator<D> durableIter) {

      this.durableIter = durableIter;
    }

    /**
     * Returns {@code true} if the underlying source has more elements.
     *
     * @return {@code true} when at least one more element is available
     */
    @Override
    public boolean hasNext () {

      return durableIter.hasNext();
    }

    /**
     * Returns the next durable after persisting it into the vector cache.
     *
     * @return the cache-coherent durable instance
     */
    @Override
    public D next () {

      return vectoredDao.persist(durableClass, durableIter.next(), UpdateMode.SOFT);
    }

    /**
     * Delegates removal to the underlying iterator.
     */
    @Override
    public void remove () {

      durableIter.remove();
    }
  }
}
