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
 * Encapsulates SSL configuration for Jetty, including keystore, truststore, and connector settings.
 */
public class SSLInfo {

  private SSLStore keySSLStore;
  private SSLStore trustSSLStore;
  private boolean requireClientAuth = false;
  private boolean proxyMode = false;
  private int port = 443;

  /**
   * Retrieves the HTTPS port number Jetty should bind to.
   *
   * @return configured HTTPS port
   */
  public int getPort () {

    return port;
  }

  /**
   * Sets the HTTPS port number Jetty should bind to.
   *
   * @param port port to expose for secure connections
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Returns the SSL store containing the server's private key and certificate.
   *
   * @return keystore configuration
   */
  public SSLStore getKeySSLStore () {

    return keySSLStore;
  }

  /**
   * Sets the SSL store that contains the server's key material.
   *
   * @param keySSLStore keystore configuration
   */
  public void setKeySSLStore (SSLStore keySSLStore) {

    this.keySSLStore = keySSLStore;
  }

  /**
   * Returns the SSL store containing trusted certificate authorities.
   *
   * @return truststore configuration
   */
  public SSLStore getTrustSSLStore () {

    return trustSSLStore;
  }

  /**
   * Sets the SSL store used to validate client or upstream certificates.
   *
   * @param trustSSLStore truststore configuration
   */
  public void setTrustSSLStore (SSLStore trustSSLStore) {

    this.trustSSLStore = trustSSLStore;
  }

  /**
   * Indicates whether Jetty should require client certificate authentication.
   *
   * @return {@code true} if client certificates are required
   */
  public boolean isRequireClientAuth () {

    return requireClientAuth;
  }

  /**
   * Configures whether client certificates must be presented during SSL handshake.
   *
   * @param requireClientAuth {@code true} to enforce client authentication
   */
  public void setRequireClientAuth (boolean requireClientAuth) {

    this.requireClientAuth = requireClientAuth;
  }

  /**
   * Indicates whether Jetty should operate in proxy mode.
   *
   * @return {@code true} if proxy-specific SSL handling should be applied
   */
  public boolean isProxyMode () {

    return proxyMode;
  }

  /**
   * Configures proxy mode, which may alter SSL behavior when Jetty sits behind another proxy.
   *
   * @param proxyMode {@code true} to enable proxy mode
   */
  public void setProxyMode (boolean proxyMode) {

    this.proxyMode = proxyMode;
  }
}
