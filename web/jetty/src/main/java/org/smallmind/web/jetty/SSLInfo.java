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
package org.smallmind.web.jetty;

/**
 * Encapsulates the keystore, truststore, port, and behavioral flags needed to configure an HTTPS connector in Jetty.
 */
public class SSLInfo {

  private SSLStore keySSLStore;
  private SSLStore trustSSLStore;
  private boolean requireClientAuth = false;
  private boolean proxyMode = false;
  private int port = 443;

  /**
   * Returns the port number on which Jetty listens for HTTPS connections.
   *
   * @return configured HTTPS port
   */
  public int getPort () {

    return port;
  }

  /**
   * Sets the port number on which Jetty should listen for HTTPS connections.
   *
   * @param port the HTTPS port
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Returns the store containing the server's private key and certificate chain.
   *
   * @return the keystore configuration
   */
  public SSLStore getKeySSLStore () {

    return keySSLStore;
  }

  /**
   * Sets the store that provides the server's private key and certificate chain.
   *
   * @param keySSLStore the keystore configuration
   */
  public void setKeySSLStore (SSLStore keySSLStore) {

    this.keySSLStore = keySSLStore;
  }

  /**
   * Returns the store containing trusted certificate authorities used to verify peers.
   *
   * @return the truststore configuration
   */
  public SSLStore getTrustSSLStore () {

    return trustSSLStore;
  }

  /**
   * Sets the store that contains trusted certificate authorities.
   *
   * @param trustSSLStore the truststore configuration
   */
  public void setTrustSSLStore (SSLStore trustSSLStore) {

    this.trustSSLStore = trustSSLStore;
  }

  /**
   * Returns whether Jetty requires clients to present a certificate during the SSL handshake.
   *
   * @return {@code true} if client authentication is required
   */
  public boolean isRequireClientAuth () {

    return requireClientAuth;
  }

  /**
   * Configures whether Jetty demands client certificate authentication.
   *
   * @param requireClientAuth {@code true} to enforce mutual TLS
   */
  public void setRequireClientAuth (boolean requireClientAuth) {

    this.requireClientAuth = requireClientAuth;
  }

  /**
   * Returns whether Jetty operates in proxy mode, which may alter SSL handling when behind another proxy.
   *
   * @return {@code true} if proxy mode is active
   */
  public boolean isProxyMode () {

    return proxyMode;
  }

  /**
   * Configures whether Jetty should operate in proxy mode.
   *
   * @param proxyMode {@code true} to enable proxy-aware SSL behavior
   */
  public void setProxyMode (boolean proxyMode) {

    this.proxyMode = proxyMode;
  }
}
