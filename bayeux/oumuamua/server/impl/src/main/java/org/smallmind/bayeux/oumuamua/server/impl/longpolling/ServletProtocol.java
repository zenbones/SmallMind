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
package org.smallmind.bayeux.oumuamua.server.impl.longpolling;

import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractProtocol;
import org.smallmind.bayeux.oumuamua.server.spi.Protocols;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;

/**
 * Bayeux protocol implementation for servlet-based HTTP long-polling, managing a single
 * {@link LongPollingTransport} and exposing a configurable poll timeout to the server.
 *
 * @param <V> the concrete {@link Value} type used by the server's JSON codec
 */
public class ServletProtocol<V extends Value<V>> extends AbstractProtocol<V> {

  private final LongPollingTransport<V> longPollingTransport;
  private final long longPollTimeoutMilliseconds;

  /**
   * Constructs a protocol with the supplied poll timeout and no additional listeners.
   *
   * @param longPollTimeoutMilliseconds maximum time in milliseconds a poll request may be
   *                                    held open before the server sends an empty response
   */
  public ServletProtocol (long longPollTimeoutMilliseconds) {

    this(longPollTimeoutMilliseconds, null);
  }

  /**
   * Constructs a protocol with the supplied poll timeout and registers the given listeners.
   *
   * @param longPollTimeoutMilliseconds maximum time in milliseconds a poll request may be
   *                                    held open before the server sends an empty response
   * @param listeners                   protocol event listeners to register, or {@code null}
   *                                    to register none
   */
  public ServletProtocol (long longPollTimeoutMilliseconds, ProtocolListener<V>[] listeners) {

    this.longPollTimeoutMilliseconds = longPollTimeoutMilliseconds;

    longPollingTransport = new LongPollingTransport<>(this);

    if (listeners != null) {
      for (ProtocolListener<V> listener : listeners) {
        addListener(listener);
      }
    }
  }

  /**
   * Returns the canonical name identifying this protocol to the server.
   *
   * @return the servlet protocol name
   */
  @Override
  public String getName () {

    return Protocols.SERVLET.getName();
  }

  /**
   * Confirms that this protocol employs long-polling request semantics.
   *
   * @return {@code true} always
   */
  @Override
  public boolean isLongPolling () {

    return true;
  }

  /**
   * Returns the maximum duration a long-poll request will be held open before the server
   * responds with an empty message batch.
   *
   * @return the configured long-poll timeout in milliseconds
   */
  @Override
  public long getLongPollTimeoutMilliseconds () {

    return longPollTimeoutMilliseconds;
  }

  /**
   * Returns the set of Bayeux transport names supported by this protocol.
   *
   * @return a single-element array containing the {@code long-polling} transport name
   */
  @Override
  public String[] getTransportNames () {

    return new String[] {Transports.LONG_POLLING.getName()};
  }

  /**
   * Looks up a transport by its Bayeux name.
   *
   * @param name the Bayeux transport name to look up
   * @return the {@link LongPollingTransport} when {@code name} matches {@code long-polling},
   * or {@code null} for any other name
   */
  @Override
  public Transport<V> getTransport (String name) {

    return Transports.LONG_POLLING.getName().equals(name) ? longPollingTransport : null;
  }
}
