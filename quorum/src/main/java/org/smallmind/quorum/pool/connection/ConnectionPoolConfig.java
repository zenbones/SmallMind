/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.pool.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.quorum.pool.PoolConfig;

public class ConnectionPoolConfig extends PoolConfig<ConnectionPoolConfig> {

  private final AtomicBoolean testOnConnect = new AtomicBoolean(false);
  private final AtomicBoolean testOnAcquire = new AtomicBoolean(false);
  private final AtomicLong connectionTimeoutMillis = new AtomicLong(0);

  public ConnectionPoolConfig () {

  }

  public ConnectionPoolConfig (PoolConfig<?> poolConfig) {

    super(poolConfig);

    if (poolConfig.getConfigurationClass().isAssignableFrom(ConnectionPoolConfig.class)) {
      setTestOnConnect(((ConnectionPoolConfig)poolConfig).isTestOnConnect());
      setTestOnAcquire(((ConnectionPoolConfig)poolConfig).isTestOnAcquire());
      setConnectionTimeoutMillis(((ConnectionPoolConfig)poolConfig).getConnectionTimeoutMillis());
    }
  }

  @Override
  public Class<ConnectionPoolConfig> getConfigurationClass () {

    return ConnectionPoolConfig.class;
  }

  public boolean requiresDeconstruction () {

    return (getMaxLeaseTimeSeconds() > 0) || (getMaxIdleTimeSeconds() > 0) || (getUnReturnedElementTimeoutSeconds() > 0);
  }

  public boolean isTestOnConnect () {

    return testOnConnect.get();
  }

  public ConnectionPoolConfig setTestOnConnect (boolean testOnConnect) {

    this.testOnConnect.set(testOnConnect);

    return getConfigurationClass().cast(this);
  }

  public boolean isTestOnAcquire () {

    return testOnAcquire.get();
  }

  public ConnectionPoolConfig setTestOnAcquire (boolean testOnAcquire) {

    this.testOnAcquire.set(testOnAcquire);

    return getConfigurationClass().cast(this);
  }

  public long getConnectionTimeoutMillis () {

    return connectionTimeoutMillis.get();
  }

  public ConnectionPoolConfig setConnectionTimeoutMillis (long connectionTimeoutMillis) {

    if (connectionTimeoutMillis < 0) {
      throw new IllegalArgumentException("Connection timeout must be >= 0");
    }

    this.connectionTimeoutMillis.set(connectionTimeoutMillis);

    return getConfigurationClass().cast(this);
  }
}
