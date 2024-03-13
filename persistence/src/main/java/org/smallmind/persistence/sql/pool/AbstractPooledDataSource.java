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
package org.smallmind.persistence.sql.pool;

import java.io.PrintWriter;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.CommonDataSource;
import javax.sql.PooledConnection;
import org.smallmind.quorum.pool.ComponentPoolException;

public abstract class AbstractPooledDataSource<D extends CommonDataSource, P extends PooledConnection> implements CommonDataSource {

  private final Class<D> dataSourceClass;
  private final Class<P> pooledConnectionClass;
  private final PrintWriter logWriter;

  public AbstractPooledDataSource (Class<D> dataSourceClass, Class<P> pooledConnectionClass) {

    this.dataSourceClass = dataSourceClass;
    this.pooledConnectionClass = pooledConnectionClass;

    logWriter = new PrintWriter(new PooledLogWriter());
  }

  public abstract void startup ()
    throws ComponentPoolException;

  public abstract void shutdown ()
    throws ComponentPoolException;

  public Class<D> getDataSourceClass () {

    return dataSourceClass;
  }

  public Class<P> getPooledConnectionClass () {

    return pooledConnectionClass;
  }

  public Logger getParentLogger ()
    throws SQLFeatureNotSupportedException {

    throw new SQLFeatureNotSupportedException();
  }

  public PrintWriter getLogWriter () {

    return logWriter;
  }

  public void setLogWriter (PrintWriter out) {

    throw new UnsupportedOperationException("Please properly configure the underlying pool which is represented by this DataSource");
  }

  public int getLoginTimeout () {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  public void setLoginTimeout (int seconds) {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  public boolean isWrapperFor (Class<?> clazz) {

    return false;
  }

  public <T> T unwrap (Class<T> clazz) {

    throw new UnsupportedOperationException("This DataSource represents a connection pool");
  }
}
