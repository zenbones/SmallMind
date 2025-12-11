/*
 * Copyright (c) 2007 through 2024 David Berkman
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
 * Protocol implementation for websocket-based Bayeux messaging.
 *
 * @param <V> concrete value type used in messages
 */
public class WebsocketProtocol<V extends Value<V>> extends AbstractProtocol<V> {

  private final WebSocketTransport<V> webSocketTransport;
  private final long longPollTimeoutMilliseconds;

  /**
   * Creates the protocol with default listeners.
   *
   * @param longPollTimeoutMilliseconds timeout value retained for compatibility
   * @param websocketConfiguration      websocket configuration
   */
  public WebsocketProtocol (long longPollTimeoutMilliseconds, WebsocketConfiguration websocketConfiguration) {

    this(longPollTimeoutMilliseconds, websocketConfiguration, null);
  }

  /**
   * Creates the protocol with optional listeners.
   *
   * @param longPollTimeoutMilliseconds timeout value retained for compatibility
   * @param websocketConfiguration      websocket configuration
   * @param listeners                   optional protocol listeners to register
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
   * @return protocol name
   */
  @Override
  public String getName () {

    return Protocols.WEBSOCKET.getName();
  }

  /**
   * Websocket protocol is not long-polling.
   *
   * @return false
   */
  @Override
  public boolean isLongPolling () {

    return false;
  }

  /**
   * @return configured long poll timeout value
   */
  @Override
  public long getLongPollTimeoutMilliseconds () {

    return longPollTimeoutMilliseconds;
  }

  /**
   * @return supported transport names
   */
  @Override
  public String[] getTransportNames () {

    return new String[] {Transports.WEBSOCKET.getName()};
  }

  /**
   * Resolves the websocket transport by name.
   *
   * @param name transport name
   * @return websocket transport or {@code null} if name differs
   */
  @Override
  public Transport<V> getTransport (String name) {

    return Transports.WEBSOCKET.getName().equals(name) ? webSocketTransport : null;
  }
}
