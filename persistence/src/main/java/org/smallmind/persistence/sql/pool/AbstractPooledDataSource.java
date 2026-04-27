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
package org.smallmind.persistence.sql.pool;

import java.io.PrintWriter;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.CommonDataSource;
import javax.sql.PooledConnection;
import org.smallmind.quorum.pool.ComponentPoolException;

/**
 * Base class for data sources backed by a component pool. It exposes minimal {@link CommonDataSource}
 * functionality while deferring actual connection creation to subclasses.
 *
 * @param <D> concrete data source type managed by the pool
 * @param <P> pooled connection type returned by the pool
 */
public abstract class AbstractPooledDataSource<D extends CommonDataSource, P extends PooledConnection> implements CommonDataSource {

  private final Class<D> dataSourceClass;
  private final Class<P> pooledConnectionClass;
  private final PrintWriter logWriter;

  /**
   * Creates a pooled data source descriptor.
   *
   * @param dataSourceClass       underlying data source class
   * @param pooledConnectionClass pooled connection class produced by the pool
   */
  public AbstractPooledDataSource (Class<D> dataSourceClass, Class<P> pooledConnectionClass) {

    this.dataSourceClass = dataSourceClass;
    this.pooledConnectionClass = pooledConnectionClass;

    logWriter = new PrintWriter(new PooledLogWriter());
  }

  /**
   * Initializes the underlying pool; must be called before use.
   *
   * @throws ComponentPoolException if startup fails
   */
  public abstract void startup ()
    throws ComponentPoolException;

  /**
   * Shuts down the underlying pool and releases resources.
   *
   * @throws ComponentPoolException if shutdown fails
   */
  public abstract void shutdown ()
    throws ComponentPoolException;

  /**
   * @return class of the wrapped data source
   */
  public Class<D> getDataSourceClass () {

    return dataSourceClass;
  }

  /**
   * @return class of the pooled connection returned to callers
   */
  public Class<P> getPooledConnectionClass () {

    return pooledConnectionClass;
  }

  public Logger getParentLogger ()
    throws SQLFeatureNotSupportedException {

    throw new SQLFeatureNotSupportedException();
  }

  /**
   * Retrieves the log writer for this DataSource object. The log writer is a character output
   * stream to which all logging and tracing messages for this data source will be printed
   * including messages printed by the methods of this interface. When a DataSource object is
   * created, the log writer is initially null; in other words, the default is for logging to
   * be disabled.
   *
   * @return the log writer for this data source or null if logging is disabled
   */
  public PrintWriter getLogWriter () {

    return logWriter;
  }

  /**
   * Sets the log writer for this DataSource object to the given java.io.PrintWriter object.
   * The log writer is a character output stream to which all logging and tracing messages for
   * this data source will be printed. When a DataSource object is created the log writer is
   * initially null; in other words, the default is for logging to be disabled.
   *
   * @param out the new log writer; to disable logging, set to null
   */
  public void setLogWriter (PrintWriter out) {

    throw new UnsupportedOperationException("Please properly configure the underlying pool which is represented by this DataSource");
  }

  /**
   * Gets the maximum time in seconds that this data source can wait while attempting to connect
   * to a database. A value of zero means that the timeout is the default system timeout if there
   * is one; otherwise, it means that there is no timeout. When a DataSource object is created,
   * the login timeout is initially zero.
   *
   * @return the data source login time limit
   */
  @Override
  public int getLoginTimeout () {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  /**
   * Sets the maximum time in seconds that this data source will wait while attempting to connect
   * to a database. A value of zero specifies that the timeout is the default system timeout if
   * there is one; otherwise, it specifies that there is no timeout. When a DataSource object is
   * created, the login timeout is initially zero.
   *
   * @param seconds the data source login time limit
   */
  public void setLoginTimeout (int seconds) {

    throw new UnsupportedOperationException("Please properly configure the underlying resource managed by the pool which is represented by this DataSource");
  }

  /**
   * Returns true if this either implements the interface argument or is directly or indirectly
   * a wrapper for an object that does. Returns false otherwise.
   *
   * @param clazz a Class defining an interface
   * @return true if this implements the interface or directly or indirectly wraps an object that does
   */
  public boolean isWrapperFor (Class<?> clazz) {

    return false;
  }

  /**
   * Returns an object that implements the given interface to allow access to non-standard methods,
   * or standard methods not exposed by the proxy. If the receiver implements the interface then the
   * result is the receiver or a proxy for the receiver. If the receiver is a wrapper and the
   * wrapped object implements the interface then the result is the wrapped object or a proxy for
   * the wrapped object.
   *
   * @param clazz a Class defining an interface that the result must implement
   * @return an object that implements the interface
   */
  public <T> T unwrap (Class<T> clazz) {

    throw new UnsupportedOperationException("This DataSource represents a connection pool");
  }
}
