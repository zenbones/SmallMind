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
package org.smallmind.bayeux.oumuamua.server.api;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Abstraction for a Bayeux protocol implementation that owns one or more transports.
 *
 * @param <V> concrete {@link Value} implementation used for JSON payloads
 */
public interface Protocol<V extends Value<V>> {

  /**
   * Marker for protocol listeners.
   */
  interface Listener<V extends Value<V>> {

  }

  /**
   * Listener for protocol-level events such as message receipt and delivery.
   */
  interface ProtocolListener<V extends Value<V>> extends Listener<V> {

    /**
     * Invoked when incoming messages are received from a client.
     *
     * @param incomingMessages the messages received in a batch
     */
    void onReceipt (Message<V>[] incomingMessages);

    /**
     * Invoked when a published message is created from an incoming message.
     *
     * @param originatingMessage the message supplied by the client
     * @param outgoingMessage    the message to be delivered to subscribers
     */
    void onPublish (Message<V> originatingMessage, Message<V> outgoingMessage);

    /**
     * Invoked when an outgoing packet is delivered to transports.
     *
     * @param outgoingPacket the packet being sent
     */
    void onDelivery (Packet<V> outgoingPacket);
  }

  /**
   * Initializes the protocol and its transports using servlet configuration.
   *
   * @param server        the hosting server
   * @param servletConfig the servlet configuration
   * @throws ServletException if transport initialization fails
   */
  default void init (Server<V> server, ServletConfig servletConfig)
    throws ServletException {

    for (String transportName : getTransportNames()) {
      getTransport(transportName).init(server, servletConfig);
    }
  }

  /**
   * @return the protocol name used for registration
   */
  String getName ();

  /**
   * @return {@code true} if the protocol operates using long polling
   */
  boolean isLongPolling ();

  /**
   * @return timeout in milliseconds for long poll connections
   */
  long getLongPollTimeoutMilliseconds ();

  /**
   * @return names of transports supported by this protocol
   */
  String[] getTransportNames ();

  /**
   * Retrieves a transport by name.
   *
   * @param name the transport name
   * @return the associated transport
   */
  Transport<V> getTransport (String name);

  /**
   * Adds a protocol listener.
   *
   * @param listener listener to register
   */
  void addListener (Listener<V> listener);

  /**
   * Removes a previously registered listener.
   *
   * @param listener listener to remove
   */
  void removeListener (Listener<V> listener);
}
