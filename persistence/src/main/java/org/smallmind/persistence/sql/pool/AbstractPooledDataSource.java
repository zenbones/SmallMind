/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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

  public abstract void startup ()
    throws ComponentPoolException;

  public abstract void shutdown ()
    throws ComponentPoolException;

  public AbstractPooledDataSource (Class<D> dataSourceClass, Class<P> pooledConnectionClass) {

    this.dataSourceClass = dataSourceClass;
    this.pooledConnectionClass = pooledConnectionClass;

    logWriter = new PrintWriter(new PooledLogWriter());
  }

  public Class<D> getDataSourceClass () {

    return dataSourceClass;
  }

  public Class<P> getPooledConnectionClass () {

    return pooledConnectionClass;
  }

  public Logger getParentLogger () throws SQLFeatureNotSupportedException {

    throw new SQLFeatureNotSupportedException();
  }

  public PrintWriter getLogWriter () {

    return logWriter;
  }

  public void setLogWriter (PrintWriter out) {

    throw new UnsupportedOperationException("Please properly configure the underlying pool which is represented by this DataSource");
  }

  public void setLoginTimeout (int seconds) {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  public int getLoginTimeout () {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  public boolean isWrapperFor (Class<?> iface) {

    return false;
  }

  public <T> T unwrap (Class<T> iface) {

    throw new UnsupportedOperationException("This DataSource represents a connection pool");
  }
}
