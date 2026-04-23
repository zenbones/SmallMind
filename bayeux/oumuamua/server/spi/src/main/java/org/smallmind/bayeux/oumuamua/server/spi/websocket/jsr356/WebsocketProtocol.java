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
package org.smallmind.bayeux.oumuamua.server.spi.websocket.jsr356;

import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractProtocol;
import org.smallmind.bayeux.oumuamua.server.spi.Protocols;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;

/**
 * {@link AbstractProtocol} implementation that drives Bayeux messaging over a JSR-356 websocket
 * connection, owning a single {@link WebSocketTransport} and exposing the
 * {@link org.smallmind.bayeux.oumuamua.server.spi.Transports#WEBSOCKET} transport name.
 *
 * @param <V> the concrete {@link Value} type carried by messages in this deployment
 */
public class WebsocketProtocol<V extends Value<V>> extends AbstractProtocol<V> {

  private final WebSocketTransport<V> webSocketTransport;
  private final long longPollTimeoutMilliseconds;

  /**
   * Creates the protocol with no additional listeners.
   *
   * @param longPollTimeoutMilliseconds long-poll timeout stored and exposed via
   *                                    {@link #getLongPollTimeoutMilliseconds()}; not used by the
   *                                    websocket transport itself
   * @param websocketConfiguration      configuration for the underlying {@link WebSocketTransport}
   */
  public WebsocketProtocol (long longPollTimeoutMilliseconds, WebsocketConfiguration websocketConfiguration) {

    this(longPollTimeoutMilliseconds, websocketConfiguration, null);
  }

  /**
   * Creates the protocol and registers the supplied listeners.
   *
   * @param longPollTimeoutMilliseconds long-poll timeout stored and exposed via
   *                                    {@link #getLongPollTimeoutMilliseconds()}; not used by the
   *                                    websocket transport itself
   * @param websocketConfiguration      configuration for the underlying {@link WebSocketTransport}
   * @param listeners                   protocol listeners to register, or {@code null} for none
   */
  public WebsocketProtocol (long longPollTimeoutMilliseconds, WebsocketConfiguration websocketConfiguration, ProtocolListener<V>[] listeners) {

    this.longPollTimeoutMilliseconds = longPollTimeoutMilliseconds;

    webSocketTransport = new WebSocketTransport<>(this, websocketConfiguration);

    if (listeners != null) {
      for (ProtocolListener<V> listener : listeners) {
        addListener(listener);
      }
    }
  }

  /**
   * Returns the canonical name of this protocol as defined by
   * {@link org.smallmind.bayeux.oumuamua.server.spi.Protocols#WEBSOCKET}.
   *
   * @return protocol name string
   */
  @Override
  public String getName () {

    return Protocols.WEBSOCKET.getName();
  }

  /**
   * Indicates that websocket connections are persistent and therefore not long-polling.
   *
   * @return {@code false} always
   */
  @Override
  public boolean isLongPolling () {

    return false;
  }

  /**
   * Returns the long-poll timeout value supplied at construction time.
   *
   * @return timeout in milliseconds as configured; not used internally by the websocket transport
   */
  @Override
  public long getLongPollTimeoutMilliseconds () {

    return longPollTimeoutMilliseconds;
  }

  /**
   * Returns the single transport name supported by this protocol.
   *
   * @return a one-element array containing the websocket transport name
   */
  @Override
  public String[] getTransportNames () {

    return new String[] {Transports.WEBSOCKET.getName()};
  }

  /**
   * Looks up the transport by name, returning the owned {@link WebSocketTransport} when the name
   * matches, or {@code null} for any other name.
   *
   * @param name the transport name to look up
   * @return the {@link WebSocketTransport} instance, or {@code null} if {@code name} does not match
   */
  @Override
  public Transport<V> getTransport (String name) {

    return Transports.WEBSOCKET.getName().equals(name) ? webSocketTransport : null;
  }
}
