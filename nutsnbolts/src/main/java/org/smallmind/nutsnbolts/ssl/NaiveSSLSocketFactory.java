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
package org.smallmind.nutsnbolts.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

/**
 * {@link SSLSocketFactory} implementation that accepts all certificates using a {@link NaiveTrustManager}.
 * Useful for testing against servers with self-signed or otherwise untrusted certificates. Do not use
 * in production where peer verification is required.
 */
public class NaiveSSLSocketFactory extends SSLSocketFactory {

  private static final NaiveSSLSocketFactory INSTANCE;

  private final SSLSocketFactory internalSSLSocketFactory;

  static {
    try {
      INSTANCE = new NaiveSSLSocketFactory();
    } catch (Exception exception) {
      throw new StaticInitializationError(exception);
    }
  }

  /**
   * Builds an SSL socket factory that trusts all certificates using the JVM's SSL context.
   *
   * @throws KeyManagementException   if SSL context initialization fails
   * @throws NoSuchAlgorithmException if the SSL algorithm is unavailable
   */
  private NaiveSSLSocketFactory ()
    throws KeyManagementException, NoSuchAlgorithmException {

    TrustManager[] trustManagers = new TrustManager[] {new NaiveTrustManager()};
    SSLContext context = SSLContext.getInstance("SSL");
    context.init(new KeyManager[0], trustManagers, new SecureRandom());

    internalSSLSocketFactory = context.getSocketFactory();
  }

  /**
   * Returns the shared singleton instance to avoid repeated insecure context creation.
   *
   * @return the reusable insecure socket factory
   */
  public static NaiveSSLSocketFactory getDefault () {

    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getDefaultCipherSuites () {

    return internalSSLSocketFactory.getDefaultCipherSuites();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getSupportedCipherSuites () {

    return internalSSLSocketFactory.getSupportedCipherSuites();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket ()
    throws IOException {

    return internalSSLSocketFactory.createSocket();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket (Socket s, String host, int port, boolean autoClose)
    throws IOException {

    return internalSSLSocketFactory.createSocket(s, host, port, autoClose);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket (String host, int port)
    throws IOException {

    return internalSSLSocketFactory.createSocket(host, port);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket (String host, int port, InetAddress localHost, int localPort)
    throws IOException {

    return internalSSLSocketFactory.createSocket(host, port, localHost, localPort);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket (InetAddress host, int port)
    throws IOException {

    return internalSSLSocketFactory.createSocket(host, port);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Socket createSocket (InetAddress address, int port, InetAddress localAddress, int localPort)
    throws IOException {

    return internalSSLSocketFactory.createSocket(address, port, localAddress, localPort);
  }
}
