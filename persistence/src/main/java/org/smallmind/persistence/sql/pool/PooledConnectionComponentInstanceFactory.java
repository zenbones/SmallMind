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

import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import org.smallmind.quorum.juggler.Juggler;
import org.smallmind.quorum.juggler.JugglerResourceCreationException;
import org.smallmind.quorum.juggler.NoAvailableJugglerResourceException;
import org.smallmind.quorum.pool.complex.ComponentInstance;
import org.smallmind.quorum.pool.complex.ComponentInstanceFactory;
import org.smallmind.quorum.pool.complex.ComponentPool;

/**
 * {@link ComponentInstanceFactory} that produces {@link PooledConnectionComponentInstance}s using a
 * {@link Juggler} of {@link ConnectionPoolDataSource}s.
 */
public class PooledConnectionComponentInstanceFactory<P extends PooledConnection> implements ComponentInstanceFactory<P> {

  private final Juggler<ConnectionPoolDataSource, P> pooledConnectionJuggler;
  private String validationQuery = "select 1";

  /**
   * Creates a factory with default recovery check interval.
   *
   * @param pooledConnectionClass expected pooled connection type
   * @param dataSources           connection pool data sources to juggle between
   */
  public PooledConnectionComponentInstanceFactory (Class<P> pooledConnectionClass, ConnectionPoolDataSource... dataSources) {

    this(0, pooledConnectionClass, dataSources);
  }

  /**
   * Creates a factory specifying the juggler's recovery check interval.
   *
   * @param recoveryCheckSeconds  seconds between recovery checks
   * @param pooledConnectionClass expected pooled connection type
   * @param dataSources           connection pool data sources to juggle between
   */
  public PooledConnectionComponentInstanceFactory (int recoveryCheckSeconds, Class<P> pooledConnectionClass, ConnectionPoolDataSource... dataSources) {

    pooledConnectionJuggler = new Juggler<>(ConnectionPoolDataSource.class, pooledConnectionClass, recoveryCheckSeconds, new PooledConnectionJugglingPinFactory<>(), dataSources);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize ()
    throws JugglerResourceCreationException {

    pooledConnectionJuggler.initialize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startup () {

    pooledConnectionJuggler.startup();
  }

  /**
   * @return SQL used to validate connections created by this factory
   */
  public String getValidationQuery () {

    return validationQuery;
  }

  /**
   * Sets the SQL used to validate connections. Set to {@code null} or empty to disable validation.
   *
   * @param validationQuery validation SQL
   */
  public void setValidationQuery (String validationQuery) {

    this.validationQuery = validationQuery;
  }

  /**
   * Creates a new pooled connection component instance using the juggler to obtain a data source.
   *
   * @param componentPool pool that will manage the created instance
   * @return wrapped pooled connection component
   * @throws NoAvailableJugglerResourceException if no data source is available
   * @throws SQLException                        if the pooled connection cannot be created or validated
   */
  public ComponentInstance<P> createInstance (ComponentPool<P> componentPool)
    throws NoAvailableJugglerResourceException, SQLException {

    return new PooledConnectionComponentInstance<>(componentPool, pooledConnectionJuggler.pickResource(), validationQuery);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void shutdown () {

    pooledConnectionJuggler.shutdown();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deconstruct () {

    pooledConnectionJuggler.deconstruct();
  }
}
