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
 * {@link SSLSocketFactory} that trusts all certificates via {@link NaiveTrustManager}, suitable for testing against
 * servers with self-signed or otherwise untrusted certificates only.
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
   * Initializes an SSL context with a {@link NaiveTrustManager} and stores the resulting socket factory.
   *
   * @throws KeyManagementException   if SSL context initialization fails
   * @throws NoSuchAlgorithmException if the {@code SSL} algorithm is unavailable
   */
  private NaiveSSLSocketFactory ()
    throws KeyManagementException, NoSuchAlgorithmException {

    TrustManager[] trustManagers = new TrustManager[] {new NaiveTrustManager()};
    SSLContext context = SSLContext.getInstance("SSL");
    context.init(new KeyManager[0], trustManagers, new SecureRandom());

    internalSSLSocketFactory = context.getSocketFactory();
  }

  /**
   * Returns the shared singleton instance of this factory.
   *
   * @return the singleton {@link NaiveSSLSocketFactory}
   */
  public static NaiveSSLSocketFactory getDefault () {

    return INSTANCE;
  }

  /**
   * Returns the cipher suites enabled by default in the underlying SSL context.
   *
   * @return array of default cipher suite names
   */
  @Override
  public String[] getDefaultCipherSuites () {

    return internalSSLSocketFactory.getDefaultCipherSuites();
  }

  /**
   * Returns all cipher suites supported by the underlying SSL context.
   *
   * @return array of supported cipher suite names
   */
  @Override
  public String[] getSupportedCipherSuites () {

    return internalSSLSocketFactory.getSupportedCipherSuites();
  }

  /**
   * Creates an unconnected SSL socket.
   *
   * @return a new unconnected {@link Socket}
   * @throws IOException if the socket cannot be created
   */
  @Override
  public Socket createSocket ()
    throws IOException {

    return internalSSLSocketFactory.createSocket();
  }

  /**
   * Wraps an existing socket with SSL, connecting to the specified host and port.
   *
   * @param s         the existing socket to layer SSL over
   * @param host      the server hostname
   * @param port      the server port
   * @param autoClose whether the underlying socket is closed when the returned socket is closed
   * @return an SSL-wrapped {@link Socket}
   * @throws IOException if the SSL socket cannot be created
   */
  @Override
  public Socket createSocket (Socket s, String host, int port, boolean autoClose)
    throws IOException {

    return internalSSLSocketFactory.createSocket(s, host, port, autoClose);
  }

  /**
   * Creates an SSL socket connected to the specified host and port.
   *
   * @param host the server hostname
   * @param port the server port
   * @return a connected SSL {@link Socket}
   * @throws IOException if the connection cannot be established
   */
  @Override
  public Socket createSocket (String host, int port)
    throws IOException {

    return internalSSLSocketFactory.createSocket(host, port);
  }

  /**
   * Creates an SSL socket connected to the specified host and port, bound to a specific local address and port.
   *
   * @param host      the server hostname
   * @param port      the server port
   * @param localHost the local address to bind to
   * @param localPort the local port to bind to
   * @return a connected SSL {@link Socket}
   * @throws IOException if the connection cannot be established
   */
  @Override
  public Socket createSocket (String host, int port, InetAddress localHost, int localPort)
    throws IOException {

    return internalSSLSocketFactory.createSocket(host, port, localHost, localPort);
  }

  /**
   * Creates an SSL socket connected to the specified address and port.
   *
   * @param host the server address
   * @param port the server port
   * @return a connected SSL {@link Socket}
   * @throws IOException if the connection cannot be established
   */
  @Override
  public Socket createSocket (InetAddress host, int port)
    throws IOException {

    return internalSSLSocketFactory.createSocket(host, port);
  }

  /**
   * Creates an SSL socket connected to the specified remote address and port, bound to a specific local address and port.
   *
   * @param address      the remote server address
   * @param port         the remote server port
   * @param localAddress the local address to bind to
   * @param localPort    the local port to bind to
   * @return a connected SSL {@link Socket}
   * @throws IOException if the connection cannot be established
   */
  @Override
  public Socket createSocket (InetAddress address, int port, InetAddress localAddress, int localPort)
    throws IOException {

    return internalSSLSocketFactory.createSocket(address, port, localAddress, localPort);
  }
}
