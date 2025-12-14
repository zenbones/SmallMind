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

import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.codec.LargeValueCompressingCodec;
import org.smallmind.memcached.cubby.codec.ObjectStreamCubbyCodec;
import org.smallmind.memcached.cubby.locator.DefaultKeyLocator;
import org.smallmind.memcached.cubby.locator.KeyLocator;
import org.smallmind.memcached.cubby.locator.MaglevKeyLocator;
import org.smallmind.memcached.cubby.translator.DefaultKeyTranslator;
import org.smallmind.memcached.cubby.translator.KeyTranslator;
import org.smallmind.memcached.cubby.translator.LargeKeyHashingTranslator;

/**
 * Configuration holder for Cubby clients, covering codec selection, routing, authentication and
 * timeout behavior.
 */
public class CubbyConfiguration {

  public static final CubbyConfiguration DEFAULT = new CubbyConfiguration();
  public static final CubbyConfiguration OPTIMAL = new CubbyConfiguration()
                                                     .setCodec(new LargeValueCompressingCodec(new ObjectStreamCubbyCodec()))
                                                     .setKeyLocator(new MaglevKeyLocator())
                                                     .setKeyTranslator(new LargeKeyHashingTranslator(new DefaultKeyTranslator()));

  private CubbyCodec codec = new ObjectStreamCubbyCodec();
  private KeyLocator keyLocator = new DefaultKeyLocator();
  private KeyTranslator keyTranslator = new DefaultKeyTranslator();
  private Authentication authentication;
  private long defaultRequestTimeoutMilliseconds = 0;
  private long connectionTimeoutMilliseconds = 3000;
  private long readTimeoutMilliseconds = 30000;
  private long keepAliveSeconds = 30;
  private long resuscitationSeconds = 10;
  private int connectionsPerHost = 1;

  /**
   * @return the codec used to serialize values
   */
  public CubbyCodec getCodec () {

    return codec;
  }

  /**
   * Sets the codec used to serialize and deserialize values.
   *
   * @param codec codec implementation
   * @return this configuration for chaining
   */
  public CubbyConfiguration setCodec (CubbyCodec codec) {

    this.codec = codec;

    return this;
  }

  /**
   * @return the key locator responsible for mapping keys to hosts
   */
  public KeyLocator getKeyLocator () {

    return keyLocator;
  }

  /**
   * Sets the key locator responsible for routing requests.
   *
   * @param keyLocator locator implementation
   * @return this configuration for chaining
   */
  public CubbyConfiguration setKeyLocator (KeyLocator keyLocator) {

    this.keyLocator = keyLocator;

    return this;
  }

  /**
   * @return the key translator used to normalize keys before routing
   */
  public KeyTranslator getKeyTranslator () {

    return keyTranslator;
  }

  /**
   * Sets the key translator used to normalize and constrain keys.
   *
   * @param keyTranslator translator implementation
   * @return this configuration for chaining
   */
  public CubbyConfiguration setKeyTranslator (KeyTranslator keyTranslator) {

    this.keyTranslator = keyTranslator;

    return this;
  }

  /**
   * @return configured authentication credentials, or {@code null} when unauthenticated
   */
  public Authentication getAuthentication () {

    return authentication;
  }

  /**
   * Sets the authentication credentials used for SASL.
   *
   * @param authentication username/password pair
   * @return this configuration for chaining
   */
  public CubbyConfiguration setAuthentication (Authentication authentication) {

    this.authentication = authentication;

    return this;
  }

  /**
   * @return default request timeout in milliseconds
   */
  public long getDefaultRequestTimeoutMilliseconds () {

    return defaultRequestTimeoutMilliseconds;
  }

  /**
   * Sets the default request timeout.
   *
   * @param defaultRequestTimeoutMilliseconds timeout in milliseconds
   * @return this configuration for chaining
   */
  public CubbyConfiguration setDefaultRequestTimeoutMilliseconds (long defaultRequestTimeoutMilliseconds) {

    this.defaultRequestTimeoutMilliseconds = defaultRequestTimeoutMilliseconds;

    return this;
  }

  /**
   * @return connection establishment timeout in milliseconds
   */
  public long getConnectionTimeoutMilliseconds () {

    return connectionTimeoutMilliseconds;
  }

  /**
   * Sets the connection establishment timeout.
   *
   * @param connectionTimeoutMilliseconds timeout in milliseconds
   * @return this configuration for chaining
   */
  public CubbyConfiguration setConnectionTimeoutMilliseconds (long connectionTimeoutMilliseconds) {

    this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;

    return this;
  }

  /**
   * @return read timeout in milliseconds
   */
  public long getReadTimeoutMilliseconds () {

    return readTimeoutMilliseconds;
  }

  /**
   * Sets the read timeout.
   *
   * @param readTimeoutMilliseconds timeout in milliseconds
   * @return this configuration for chaining
   */
  public CubbyConfiguration setReadTimeoutMilliseconds (long readTimeoutMilliseconds) {

    this.readTimeoutMilliseconds = readTimeoutMilliseconds;

    return this;
  }

  /**
   * @return keep-alive heartbeat interval in seconds
   */
  public long getKeepAliveSeconds () {

    return keepAliveSeconds;
  }

  /**
   * Sets the keep-alive heartbeat interval.
   *
   * @param keepAliveSeconds heartbeat interval in seconds
   * @return this configuration for chaining
   */
  public CubbyConfiguration setKeepAliveSeconds (long keepAliveSeconds) {

    this.keepAliveSeconds = keepAliveSeconds;

    return this;
  }

  /**
   * @return delay between reconnection attempts in seconds
   */
  public long getResuscitationSeconds () {

    return resuscitationSeconds;
  }

  /**
   * Sets the delay between reconnection attempts for unhealthy hosts.
   *
   * @param resuscitationSeconds delay in seconds
   * @return this configuration for chaining
   */
  public CubbyConfiguration setResuscitationSeconds (long resuscitationSeconds) {

    this.resuscitationSeconds = resuscitationSeconds;

    return this;
  }

  /**
   * @return number of connections to create per host
   */
  public int getConnectionsPerHost () {

    return connectionsPerHost;
  }

  /**
   * Sets the number of connections to create per host.
   *
   * @param connectionsPerHost number of connections per host
   * @return this configuration for chaining
   */
  public CubbyConfiguration setConnectionsPerHost (int connectionsPerHost) {

    this.connectionsPerHost = connectionsPerHost;

    return this;
  }
}
