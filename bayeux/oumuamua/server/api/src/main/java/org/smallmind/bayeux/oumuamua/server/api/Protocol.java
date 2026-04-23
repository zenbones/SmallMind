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
 * Named Bayeux protocol variant that owns one or more {@link Transport} instances and exposes
 * protocol-level event hooks for receipt, publish, and delivery.
 *
 * @param <V> concrete {@link Value} implementation used for JSON payloads
 */
public interface Protocol<V extends Value<V>> {

  /**
   * Base marker type for protocol-level event listeners.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface Listener<V extends Value<V>> {

  }

  /**
   * Observes the three key moments in protocol message flow: initial receipt from a client,
   * publication of a derived outbound message, and final delivery to transports.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface ProtocolListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called when a batch of messages arrives from a client before any processing.
     *
     * @param incomingMessages raw messages received from the client
     */
    void onReceipt (Message<V>[] incomingMessages);

    /**
     * Called when a client publish message has been transformed into an outbound delivery message.
     *
     * @param originatingMessage the publish message received from the client
     * @param outgoingMessage    the delivery message constructed for subscribers
     */
    void onPublish (Message<V> originatingMessage, Message<V> outgoingMessage);

    /**
     * Called when a packet is being handed off to transports for sending.
     *
     * @param outgoingPacket packet being dispatched to one or more transports
     */
    void onDelivery (Packet<V> outgoingPacket);
  }

  /**
   * Initializes all transports owned by this protocol.
   *
   * @param server        hosting server passed to each transport
   * @param servletConfig servlet configuration passed to each transport
   * @throws ServletException if any transport fails to initialize
   */
  default void init (Server<V> server, ServletConfig servletConfig)
    throws ServletException {

    for (String transportName : getTransportNames()) {
      getTransport(transportName).init(server, servletConfig);
    }
  }

  /**
   * Returns the name used to identify and negotiate this protocol.
   *
   * @return protocol name string
   */
  String getName ();

  /**
   * Returns whether this protocol uses long polling as its connection model.
   *
   * @return {@code true} if long polling is used
   */
  boolean isLongPolling ();

  /**
   * Returns how long a long poll connection is held open before timing out.
   *
   * @return long poll timeout in milliseconds
   */
  long getLongPollTimeoutMilliseconds ();

  /**
   * Returns the names of all transports registered under this protocol.
   *
   * @return array of transport names
   */
  String[] getTransportNames ();

  /**
   * Looks up a transport by its registration name.
   *
   * @param name transport name to look up
   * @return the matching transport
   */
  Transport<V> getTransport (String name);

  /**
   * Registers a listener for protocol-level events.
   *
   * @param listener listener to add
   */
  void addListener (Listener<V> listener);

  /**
   * Deregisters a previously added protocol listener.
   *
   * @param listener listener to remove
   */
  void removeListener (Listener<V> listener);
}
