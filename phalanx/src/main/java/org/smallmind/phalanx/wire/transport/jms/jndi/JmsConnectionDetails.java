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
package org.smallmind.phalanx.wire.transport.jms.jndi;

import javax.naming.Context;
import org.smallmind.quorum.pool.complex.ComponentPool;

/**
 * Encapsulates JNDI connection parameters and a pooled context provider for JMS lookups.
 */
public class JmsConnectionDetails {

  private final ComponentPool<Context> contextPool;
  private final String destinationName;
  private final String connectionFactoryName;
  private final String userName;
  private final String password;

  /**
   * Creates connection details for JNDI lookups.
   *
   * @param contextPool           pool providing JNDI contexts
   * @param destinationName       JNDI name of the destination
   * @param connectionFactoryName JNDI name of the connection factory
   * @param userName              user name for connection authentication
   * @param password              password for connection authentication
   */
  public JmsConnectionDetails (ComponentPool<Context> contextPool, String destinationName, String connectionFactoryName, String userName, String password) {

    this.contextPool = contextPool;
    this.destinationName = destinationName;
    this.connectionFactoryName = connectionFactoryName;
    this.userName = userName;
    this.password = password;
  }

  /**
   * Returns the pool used to obtain JNDI {@link Context} instances for lookups.
   *
   * @return JNDI context pool.
   */
  public ComponentPool<Context> getContextPool () {

    return contextPool;
  }

  /**
   * Returns the JNDI name of the destination.
   *
   * @return destination JNDI name.
   */
  public String getDestinationName () {

    return destinationName;
  }

  /**
   * Returns the JNDI name of the connection factory.
   *
   * @return connection factory JNDI name.
   */
  public String getConnectionFactoryName () {

    return connectionFactoryName;
  }

  /**
   * Returns the username used for connection authentication.
   *
   * @return authentication username.
   */
  public String getUserName () {

    return userName;
  }

  /**
   * Returns the password used for connection authentication.
   *
   * @return authentication password.
   */
  public String getPassword () {

    return password;
  }
}
