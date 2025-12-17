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
package org.smallmind.quorum.namespace.backingStore;

/**
 * Connection parameters used to build backing store contexts (e.g., LDAP).
 */
public class NamingConnectionDetails {

  private final String host;
  private final String rootNamespace;
  private final String userName;
  private final String password;
  private final int port;
  private boolean tls = false;

  /**
   * Constructs connection details.
   *
   * @param host          server host
   * @param port          server port
   * @param tls           whether TLS should be used
   * @param rootNamespace root namespace for lookups
   * @param userName      user name for authentication
   * @param password      password for authentication
   */
  public NamingConnectionDetails (String host, int port, boolean tls, String rootNamespace, String userName, String password) {

    this.host = host;
    this.port = port;
    this.tls = tls;
    this.rootNamespace = rootNamespace;
    this.userName = userName;
    this.password = password;
  }

  /**
   * Returns the server host.
   *
   * @return host
   */
  public String getHost () {

    return host;
  }

  /**
   * Returns the server port.
   *
   * @return port
   */
  public int getPort () {

    return port;
  }

  /**
   * Indicates whether TLS should be used.
   *
   * @return {@code true} if TLS is enabled
   */
  public boolean useTLS () {

    return tls;
  }

  /**
   * Returns the root namespace for lookups.
   *
   * @return root namespace
   */
  public String getRootNamespace () {

    return rootNamespace;
  }

  /**
   * Returns the user name for authentication.
   *
   * @return user name
   */
  public String getUserName () {

    return userName;
  }

  /**
   * Returns the password for authentication.
   *
   * @return password
   */
  public String getPassword () {

    return password;
  }
}
