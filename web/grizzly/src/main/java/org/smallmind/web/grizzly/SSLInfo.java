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
package org.smallmind.web.grizzly;

import org.smallmind.nutsnbolts.lang.SecureStore;

/**
 * Encapsulates SSL/TLS configuration for the Grizzly server including key/trust stores, client auth and port settings.
 */
public class SSLInfo {

  private SecureStore keySecureStore;
  private SecureStore trustSecureStore;
  private boolean requireClientAuth = false;
  private boolean proxyMode = false;
  private int port = 443;

  /**
   * @return HTTPS listener port
   */
  public int getPort () {

    return port;
  }

  /**
   * @param port HTTPS listener port to bind
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * @return secure store containing the private key and certificate chain
   */
  public SecureStore getKeySecureStore () {

    return keySecureStore;
  }

  /**
   * @param keySecureStore secure keystore with server credentials
   */
  public void setKeySecureStore (SecureStore keySecureStore) {

    this.keySecureStore = keySecureStore;
  }

  /**
   * @return secure store containing trusted CA certificates
   */
  public SecureStore getTrustSecureStore () {

    return trustSecureStore;
  }

  /**
   * @param trustSecureStore trust store defining accepted client/server certificates
   */
  public void setTrustSecureStore (SecureStore trustSecureStore) {

    this.trustSecureStore = trustSecureStore;
  }

  /**
   * @return {@code true} when client certificates are required
   */
  public boolean isRequireClientAuth () {

    return requireClientAuth;
  }

  /**
   * @param requireClientAuth enables or disables mutual TLS
   */
  public void setRequireClientAuth (boolean requireClientAuth) {

    this.requireClientAuth = requireClientAuth;
  }

  /**
   * @return {@code true} if authentication headers are trusted to be injected by an upstream proxy
   */
  public boolean isProxyMode () {

    return proxyMode;
  }

  /**
   * @param proxyMode set true when an upstream proxy handles client authentication and forwards identity headers
   */
  public void setProxyMode (boolean proxyMode) {

    this.proxyMode = proxyMode;
  }
}
