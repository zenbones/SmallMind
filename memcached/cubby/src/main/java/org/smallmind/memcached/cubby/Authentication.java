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
package org.smallmind.memcached.cubby;

/**
 * Immutable credentials used to authenticate with SASL-enabled memcached servers.
 *
 * <p>An {@code Authentication} instance is supplied to {@link CubbyConfiguration} and is
 * forwarded to each connection during the SASL handshake phase. Both the username and
 * password are treated as opaque strings; no encoding or hashing is applied by this class.</p>
 */
public class Authentication {

  private final String username;
  private final String password;

  /**
   * Constructs a new credential pair with the given username and password.
   *
   * @param username the SASL username presented during authentication
   * @param password the SASL password presented during authentication
   */
  public Authentication (String username, String password) {

    this.username = username;
    this.password = password;
  }

  /**
   * Returns the configured SASL username.
   *
   * @return the username, never {@code null}
   */
  public String getUsername () {

    return username;
  }

  /**
   * Returns the configured SASL password.
   *
   * @return the password, never {@code null}
   */
  public String getPassword () {

    return password;
  }
}
