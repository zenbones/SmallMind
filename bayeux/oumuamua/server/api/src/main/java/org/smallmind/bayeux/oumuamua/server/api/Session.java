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

import java.util.concurrent.TimeUnit;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Live Bayeux client session tracking connection state, transport preferences, and the inbound
 * packet queue used for long-poll delivery.
 *
 * @param <V> concrete {@link Value} implementation used for message payloads
 */
public interface Session<V extends Value<V>> extends Attributed {

  /**
   * Base marker type for session-level event listeners.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface Listener<V extends Value<V>> {

  }

  /**
   * Intercepts packets at the session boundary; messages are frozen when delivered from the channel
   * so that mutations here are visible only to this session's delivery stream, not to others.
   *
   * @param <V> concrete {@link Value} implementation
   */
  // Messages are frozen when delivered from the channel to the session, guaranteeing changes generated here are seen only in the sending session
  interface PacketListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called when a meta-channel response packet is about to be sent to this session; may return
     * a replacement packet.
     *
     * @param sender session whose meta command produced the response
     * @param packet response packet frozen for this session
     * @return packet to forward to the client transport
     */
    // For responses from META commands delivered to the sender
    Packet<V> onResponse (Session<V> sender, Packet<V> packet);

    /**
     * Called when a channel delivery packet is about to be sent to this session; may return
     * a replacement packet.
     *
     * @param sender session that originally published the message
     * @param packet delivery packet frozen for this session
     * @return packet to forward to the client transport
     */
    // For published messages delivered to receivers
    Packet<V> onDelivery (Session<V> sender, Packet<V> packet);
  }

  /**
   * Registers a session-level event listener.
   *
   * @param listener listener to add
   */
  void addListener (Listener<V> listener);

  /**
   * Deregisters a session-level event listener.
   *
   * @param listener listener to remove
   */
  void removeListener (Listener<V> listener);

  /**
   * Returns the unique identifier assigned to this session during handshake.
   *
   * @return session id string
   */
  String getId ();

  /**
   * Returns whether this session was created by a server-side component rather than a remote client.
   *
   * @return {@code true} for local (server-side) sessions
   */
  boolean isLocal ();

  /**
   * Returns whether this session is currently using long polling for message delivery.
   *
   * @return {@code true} if long polling is active
   */
  boolean isLongPolling ();

  /**
   * Sets whether this session should use long polling for message delivery.
   *
   * @param longPolling {@code true} to enable long polling
   */
  void setLongPolling (boolean longPolling);

  /**
   * Returns the maximum number of packets that may accumulate in the long-poll queue before
   * older entries are dropped.
   *
   * @return maximum long-poll queue capacity
   */
  int getMaxLongPollQueueSize ();

  /**
   * Returns the current lifecycle state of the session.
   *
   * @return current {@link SessionState}
   */
  SessionState getState ();

  /**
   * Advances the session state to {@link SessionState#HANDSHOOK} after a successful handshake.
   */
  void completeHandshake ();

  /**
   * Advances the session state to {@link SessionState#CONNECTED} after a successful connect.
   */
  void completeConnection ();

  /**
   * Advances the session state to {@link SessionState#DISCONNECTED} upon disconnect.
   */
  void completeDisconnect ();

  /**
   * Passes a response packet through the session's {@link PacketListener} chain and returns
   * the final packet.
   *
   * @param sender session that generated the response
   * @param packet outbound response packet
   * @return packet after all listeners have processed it
   */
  Packet<V> onResponse (Session<V> sender, Packet<V> packet);

  /**
   * Hands a packet directly to the session's underlying transport for immediate sending.
   *
   * @param packet packet to dispatch
   */
  void dispatch (Packet<V> packet);

  /**
   * Blocks until a packet is available for delivery or the timeout elapses; used by long-poll
   * transports to retrieve queued messages.
   *
   * @param timeout maximum time to wait
   * @param unit    time unit of the timeout
   * @return next queued packet, or {@code null} if the timeout elapsed before one arrived
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  Packet<V> poll (long timeout, TimeUnit unit)
    throws InterruptedException;

  /**
   * Delivers a channel publication packet to this session, passing it through session packet
   * listeners before queuing or dispatching it.
   *
   * @param fromChannel channel that is delivering the packet
   * @param sender      session that originally published the message
   * @param packet      delivery packet addressed to this session
   */
  void deliver (Channel<V> fromChannel, Session<V> sender, Packet<V> packet);
}
