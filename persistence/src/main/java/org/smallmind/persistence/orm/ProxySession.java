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

/**
 * Abstraction over a native ORM session, providing toggleable cache/boundary behavior
 * and lifecycle hooks used by DAO implementations.
 *
 * @param <F> native session factory type
 * @param <N> native session type
 */
public abstract class ProxySession<F, N> {

  private final String dataSourceType;
  private final String sessionSourceKey;
  private final boolean boundaryEnforced;
  private final ThreadLocal<Boolean> boundaryEnforcedThreadLocal = new ThreadLocal<Boolean>() {

    /**
     * Supplies the default boundary enforcement flag for the thread-local wrapper.
     *
     * @return {@code true} when boundaries are enforced by default
     */
    protected Boolean initialValue () {

      return boundaryEnforced;
    }
  };
  private final boolean cacheEnabled;
  private final ThreadLocal<Boolean> cacheEnabledThreadLocal = new ThreadLocal<Boolean>() {

    /**
     * Supplies the default cache enabled flag for the thread-local wrapper.
     *
     * @return {@code true} when cache usage is enabled by default
     */
    protected Boolean initialValue () {

      return cacheEnabled;
    }
  };

  /**
   * Creates a proxy session wrapper with configurable boundary and cache defaults.
   *
   * @param dataSourceType   descriptive name of the backing data source
   * @param sessionSourceKey key used to register this session with the {@link SessionManager}
   * @param boundaryEnforced default transactional boundary enforcement
   * @param cacheEnabled     default cache usage flag
   */
  public ProxySession (String dataSourceType, String sessionSourceKey, boolean boundaryEnforced, boolean cacheEnabled) {

    this.dataSourceType = dataSourceType;
    this.sessionSourceKey = sessionSourceKey;
    this.boundaryEnforced = boundaryEnforced;
    this.cacheEnabled = cacheEnabled;
  }

  /**
   * Registers this session with the {@link SessionManager} for lookup by session source key.
   */
  public void register () {

    SessionManager.register(sessionSourceKey, this);
  }

  /**
   * @return a string describing the data source backing this session
   */
  public String getDataSourceType () {

    return dataSourceType;
  }

  /**
   * @return the key used to register and locate this session
   */
  public String getSessionSourceKey () {

    return sessionSourceKey;
  }

  /**
   * Indicates whether transactional boundaries are currently enforced for this session.
   *
   * @return {@code true} when boundary enforcement is active
   */
  public boolean isBoundaryEnforced () {

    return boundaryEnforcedThreadLocal.get();
  }

  /**
   * Overrides boundary enforcement for the current thread.
   *
   * @param boundaryEnforced whether to enforce boundaries
   */
  public void overrideBoundaryEnforced (boolean boundaryEnforced) {

    boundaryEnforcedThreadLocal.set(boundaryEnforced);
  }

  /**
   * Indicates whether cache operations are enabled for this session.
   *
   * @return {@code true} when cache access is allowed
   */
  public boolean isCacheEnabled () {

    return cacheEnabledThreadLocal.get();
  }

  /**
   * Overrides cache enablement for the current thread.
   *
   * @param cacheEnabled whether cache should be used
   */
  public void overrideCacheEnabled (boolean cacheEnabled) {

    cacheEnabledThreadLocal.set(cacheEnabled);
  }

  /**
   * @return the native session factory
   */
  public abstract F getNativeSessionFactory ();

  /**
   * @return the native session instance
   */
  public abstract N getNativeSession ();

  /**
   * Begins a new native transaction.
   *
   * @return the transaction wrapper
   */
  public abstract ProxyTransaction<?> beginTransaction ();

  /**
   * Returns the current native transaction if one is active.
   *
   * @return the transaction wrapper or {@code null} if none
   */
  public abstract ProxyTransaction<?> currentTransaction ();

  /**
   * Flushes the native session.
   */
  public abstract void flush ();

  /**
   * Clears the native session.
   */
  public abstract void clear ();

  /**
   * Indicates whether the native session is closed.
   *
   * @return {@code true} when closed
   */
  public abstract boolean isClosed ();

  /**
   * Closes the native session.
   */
  public abstract void close ();
}
