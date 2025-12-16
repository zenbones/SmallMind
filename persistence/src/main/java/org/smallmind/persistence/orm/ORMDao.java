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
package org.smallmind.persistence.orm;

import java.io.Serializable;
import org.smallmind.persistence.AbstractVectorAwareManagedDao;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.VectoredDao;

/**
 * Base ORM DAO that delegates persistence to a {@link ProxySession} while optionally
 * participating in vector-based caching via {@link VectoredDao}.
 *
 * @param <I> identifier type for the durable
 * @param <D> durable entity type
 * @param <F> framework-specific session type
 * @param <N> native transaction type
 */
public abstract class ORMDao<I extends Serializable & Comparable<I>, D extends Durable<I>, F, N> extends AbstractVectorAwareManagedDao<I, D> implements RelationalDao<I, D, F, N> {

  private final ProxySession<F, N> proxySession;

  /**
   * Constructs an ORM-backed DAO.
   *
   * @param proxySession the session proxy that manages persistence and transactions
   * @param vectoredDao  optional cache-backed DAO used when caching is enabled
   */
  public ORMDao (ProxySession<F, N> proxySession, VectoredDao<I, D> vectoredDao) {

    super(proxySession.getDataSourceType(), vectoredDao);

    this.proxySession = proxySession;
  }

  /**
   * Registers this DAO instance with the {@link OrmDaoManager} for lookup by managed class.
   */
  public void register () {

    OrmDaoManager.register(getManagedClass(), this);
  }

  /**
   * Key that identifies the session source for this DAO.
   *
   * @return the session source key
   */
  @Override
  public String getSessionSourceKey () {

    return proxySession.getSessionSourceKey();
  }

  /**
   * Returns the underlying session proxy.
   *
   * @return the session proxy
   */
  @Override
  public ProxySession<F, N> getSession () {

    return proxySession;
  }

  /**
   * Determines whether cache interaction should be used.
   *
   * @return {@code true} when cache-backed operations are enabled
   */
  @Override
  public boolean isCacheEnabled () {

    return proxySession.isCacheEnabled();
  }

  // The acquire() method gets the managed object directly from the underlying data source (no vector, no cascade)

  /**
   * Retrieves a durable directly from the underlying data source without cache involvement.
   *
   * @param durableClass the durable class
   * @param id           the identifier of the durable
   * @return the loaded durable, or {@code null} if missing
   * @throws ORMOperationException if the fetch fails
   */
  public abstract D acquire (Class<D> durableClass, I id);

  /**
   * Retrieves a durable by id using the managed class.
   *
   * @param id the identifier value
   * @return the durable, or {@code null} if not found
   */
  @Override
  public D get (I id) {

    return get(getManagedClass(), id);
  }

  /**
   * Persists a durable using the managed class.
   *
   * @param durable the durable to store
   * @return the persisted durable
   */
  @Override
  public D persist (D durable) {

    return persist(getManagedClass(), durable);
  }

  /**
   * Deletes a durable using the managed class.
   *
   * @param durable the durable to remove
   */
  @Override
  public void delete (D durable) {

    delete(getManagedClass(), durable);
  }
}
