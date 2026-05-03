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
 * Aggregates all tunable parameters for a {@link CubbyMemcachedClient} instance.
 *
 * <p>Settings cover value serialization ({@link CubbyCodec}), key routing ({@link KeyLocator}),
 * key normalization ({@link KeyTranslator}), optional SASL authentication, and various timeout and
 * connection-pool knobs. Every setter returns {@code this} to support a fluent builder style.</p>
 *
 * <p>Two pre-built constants are provided for common scenarios:
 * <ul>
 *   <li>{@link #DEFAULT} &ndash; plain Java-serialization codec, simple rendezvous hashing, no auth.</li>
 *   <li>{@link #OPTIMAL} &ndash; large-value compression, Maglev consistent hashing, and large-key
 *       hashing for production deployments.</li>
 * </ul>
 */
public class CubbyConfiguration {

  /**
   * A ready-to-use configuration with sensible defaults suitable for development and testing.
   */
  public static final CubbyConfiguration DEFAULT = new CubbyConfiguration();

  /**
   * A ready-to-use configuration tuned for production: compressed large values, Maglev consistent
   * hashing for minimal key remapping on topology changes, and hashed oversized keys.
   */
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
   * Returns the codec used to serialize values before storing them in memcached and to
   * deserialize the raw bytes retrieved from the server.
   *
   * @return the configured {@link CubbyCodec}
   */
  public CubbyCodec getCodec () {

    return codec;
  }

  /**
   * Sets the codec used for value serialization and deserialization.
   *
   * @param codec the codec implementation to use; must not be {@code null}
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setCodec (CubbyCodec codec) {

    this.codec = codec;

    return this;
  }

  /**
   * Returns the key locator responsible for mapping a cache key to a specific host in the pool.
   *
   * @return the configured {@link KeyLocator}
   */
  public KeyLocator getKeyLocator () {

    return keyLocator;
  }

  /**
   * Sets the key locator used for consistent-hashing or other routing strategies.
   *
   * @param keyLocator the locator implementation; must not be {@code null}
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setKeyLocator (KeyLocator keyLocator) {

    this.keyLocator = keyLocator;

    return this;
  }

  /**
   * Returns the key translator that normalizes or hashes keys before they are used for routing
   * or sent to the server.
   *
   * @return the configured {@link KeyTranslator}
   */
  public KeyTranslator getKeyTranslator () {

    return keyTranslator;
  }

  /**
   * Sets the key translator applied to every key before routing or wire transmission.
   *
   * @param keyTranslator the translator implementation; must not be {@code null}
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setKeyTranslator (KeyTranslator keyTranslator) {

    this.keyTranslator = keyTranslator;

    return this;
  }

  /**
   * Returns the SASL authentication credentials, or {@code null} if no authentication is required.
   *
   * @return the configured {@link Authentication}, or {@code null}
   */
  public Authentication getAuthentication () {

    return authentication;
  }

  /**
   * Sets the SASL authentication credentials forwarded to the server during connection setup.
   *
   * @param authentication the username/password pair; pass {@code null} to disable authentication
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setAuthentication (Authentication authentication) {

    this.authentication = authentication;

    return this;
  }

  /**
   * Returns the default per-request timeout applied when no explicit timeout is provided to an
   * operation. A value of {@code 0} indicates no timeout.
   *
   * @return the default request timeout in milliseconds
   */
  public long getDefaultRequestTimeoutMilliseconds () {

    return defaultRequestTimeoutMilliseconds;
  }

  /**
   * Sets the default per-request timeout used when the caller does not supply an explicit one.
   *
   * @param defaultRequestTimeoutMilliseconds timeout in milliseconds; {@code 0} disables the timeout
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setDefaultRequestTimeoutMilliseconds (long defaultRequestTimeoutMilliseconds) {

    this.defaultRequestTimeoutMilliseconds = defaultRequestTimeoutMilliseconds;

    return this;
  }

  /**
   * Returns the maximum time allowed for a TCP connection to be established before the attempt
   * is considered failed.
   *
   * @return the connection establishment timeout in milliseconds
   */
  public long getConnectionTimeoutMilliseconds () {

    return connectionTimeoutMilliseconds;
  }

  /**
   * Sets the maximum time allowed for a TCP connection to be established.
   *
   * @param connectionTimeoutMilliseconds timeout in milliseconds; must be positive
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setConnectionTimeoutMilliseconds (long connectionTimeoutMilliseconds) {

    this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;

    return this;
  }

  /**
   * Returns the socket read timeout, i.e., the maximum time the client will wait for data
   * from the server on an established connection.
   *
   * @return the read timeout in milliseconds
   */
  public long getReadTimeoutMilliseconds () {

    return readTimeoutMilliseconds;
  }

  /**
   * Sets the socket read timeout applied to established connections.
   *
   * @param readTimeoutMilliseconds timeout in milliseconds; must be positive
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setReadTimeoutMilliseconds (long readTimeoutMilliseconds) {

    this.readTimeoutMilliseconds = readTimeoutMilliseconds;

    return this;
  }

  /**
   * Returns the interval between keep-alive probes sent on idle connections.
   *
   * @return the keep-alive heartbeat interval in seconds
   */
  public long getKeepAliveSeconds () {

    return keepAliveSeconds;
  }

  /**
   * Sets the interval at which keep-alive probes are sent on idle connections.
   *
   * @param keepAliveSeconds heartbeat interval in seconds; must be positive
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setKeepAliveSeconds (long keepAliveSeconds) {

    this.keepAliveSeconds = keepAliveSeconds;

    return this;
  }

  /**
   * Returns the delay between successive reconnection attempts made by the
   * {@link ServerDefibrillator} for hosts that have gone offline.
   *
   * @return the reconnection retry interval in seconds
   */
  public long getResuscitationSeconds () {

    return resuscitationSeconds;
  }

  /**
   * Sets the delay between reconnection attempts for unhealthy hosts.
   *
   * @param resuscitationSeconds retry interval in seconds; must be positive
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setResuscitationSeconds (long resuscitationSeconds) {

    this.resuscitationSeconds = resuscitationSeconds;

    return this;
  }

  /**
   * Returns the number of independent {@link ConnectionCoordinator} instances (and therefore
   * independent TCP connections) that will be created per memcached host.
   *
   * @return the number of connections per host
   */
  public int getConnectionsPerHost () {

    return connectionsPerHost;
  }

  /**
   * Sets the number of independent connections to maintain per host. Increasing this value
   * can improve throughput under high concurrency by reducing lock contention.
   *
   * @param connectionsPerHost number of connections per host; must be at least 1
   * @return this configuration instance for method chaining
   */
  public CubbyConfiguration setConnectionsPerHost (int connectionsPerHost) {

    this.connectionsPerHost = connectionsPerHost;

    return this;
  }
}
