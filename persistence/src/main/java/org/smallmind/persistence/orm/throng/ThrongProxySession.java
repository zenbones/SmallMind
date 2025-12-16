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
package org.smallmind.persistence.orm.throng;

import org.smallmind.mongodb.throng.ThrongClient;
import org.smallmind.persistence.orm.ProxySession;

/**
 * Throng implementation of {@link ProxySession}, providing a singleton transaction wrapper per thread.
 */
public class ThrongProxySession extends ProxySession<ThrongClientFactory, ThrongClient> {

  private final ThrongClientFactory throngClientFactory;
  private final ThrongProxyTransaction proxyTransaction;

  /**
   * Creates a Throng proxy session.
   *
   * @param dataSourceType      descriptive data source name for metrics
   * @param sessionSourceKey    key used to register this session
   * @param throngClientFactory factory for creating {@link ThrongClient} instances
   * @param boundaryEnforced    whether boundary enforcement is enabled by default
   * @param cacheEnabled        whether cache usage is enabled by default
   */
  public ThrongProxySession (String dataSourceType, String sessionSourceKey, ThrongClientFactory throngClientFactory, boolean boundaryEnforced, boolean cacheEnabled) {

    super(dataSourceType, sessionSourceKey, boundaryEnforced, cacheEnabled);

    this.throngClientFactory = throngClientFactory;

    proxyTransaction = new ThrongProxyTransaction(this);
  }

  /**
   * @return the current transaction wrapper
   */
  @Override
  public ThrongProxyTransaction currentTransaction () {

    return proxyTransaction;
  }

  /**
   * Begins (returns) the singleton transaction wrapper.
   *
   * @return the transaction wrapper
   */
  @Override
  public ThrongProxyTransaction beginTransaction () {

    return proxyTransaction;
  }

  /**
   * Unsupported in Throng.
   */
  @Override
  public void flush () {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported in Throng.
   */
  @Override
  public void clear () {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported in Throng.
   */
  @Override
  public void close () {

    throw new UnsupportedOperationException();
  }

  /**
   * Unsupported in Throng.
   */
  @Override
  public boolean isClosed () {

    throw new UnsupportedOperationException();
  }

  /**
   * @return the Throng client factory
   */
  @Override
  public ThrongClientFactory getNativeSessionFactory () {

    return throngClientFactory;
  }

  /**
   * @return a Throng client from the factory
   */
  @Override
  public ThrongClient getNativeSession () {

    return throngClientFactory.getThrongClient();
  }
}
