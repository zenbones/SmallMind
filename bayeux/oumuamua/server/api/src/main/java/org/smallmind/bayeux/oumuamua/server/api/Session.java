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
package org.smallmind.bayeux.oumuamua.server.api;

import java.util.concurrent.TimeUnit;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Represents a connected Bayeux client with lifecycle state and message queues.
 *
 * @param <V> concrete {@link Value} implementation used for message payloads
 */
public interface Session<V extends Value<V>> extends Attributed {

  /**
   * Marker for session listeners.
   */
  interface Listener<V extends Value<V>> {

  }

  /**
   * Listener for packet processing within a session.
   */
  // Messages are frozen when delivered from the channel to the session, guaranteeing changes generated here are seen only in the sending session
  interface PacketListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called when a meta response is delivered to this session.
     *
     * @param sender the session that produced the packet
     * @param packet the packet being delivered
     * @return the packet to forward to the client
     */
    // For responses from META commands delivered to the sender
    Packet<V> onResponse (Session<V> sender, Packet<V> packet);

    /**
     * Called when a published packet is delivered to the session.
     *
     * @param sender the session that published
     * @param packet the packet being delivered
     * @return the packet to forward to the client
     */
    // For published messages delivered to receivers
    Packet<V> onDelivery (Session<V> sender, Packet<V> packet);
  }

  /**
   * Adds a listener for session events.
   *
   * @param listener listener to add
   */
  void addListener (Listener<V> listener);

  /**
   * Removes a listener from the session.
   *
   * @param listener listener to remove
   */
  void removeListener (Listener<V> listener);

  /**
   * @return unique identifier for the session
   */
  String getId ();

  /**
   * @return {@code true} if the session is local to this server node
   */
  boolean isLocal ();

  /**
   * @return {@code true} if the session uses long polling
   */
  boolean isLongPolling ();

  /**
   * Toggles long polling usage.
   *
   * @param longPolling {@code true} to use long polling
   */
  void setLongPolling (boolean longPolling);

  /**
   * @return maximum queue size for long poll responses
   */
  int getMaxLongPollQueueSize ();

  /**
   * @return current session state
   */
  SessionState getState ();

  /**
   * Marks the handshake as complete.
   */
  void completeHandshake ();

  /**
   * Marks the connection as established.
   */
  void completeConnection ();

  /**
   * Marks the session as disconnected.
   */
  void completeDisconnect ();

  /**
   * Intercepts a response packet destined for the session.
   *
   * @param sender the originating session
   * @param packet the response packet
   * @return the processed packet
   */
  Packet<V> onResponse (Session<V> sender, Packet<V> packet);

  /**
   * Dispatches a packet to the session's transport.
   *
   * @param packet the packet to dispatch
   */
  void dispatch (Packet<V> packet);

  /**
   * Polls for a packet to deliver to the client.
   *
   * @param timeout duration to wait
   * @param unit    unit of the timeout
   * @return the next packet or {@code null} on timeout
   * @throws InterruptedException if interrupted while waiting
   */
  Packet<V> poll (long timeout, TimeUnit unit)
    throws InterruptedException;

  /**
   * Delivers a packet originating from a channel to this session.
   *
   * @param fromChannel the channel delivering the message
   * @param sender      the publishing session
   * @param packet      the packet being delivered
   */
  void deliver (Channel<V> fromChannel, Session<V> sender, Packet<V> packet);
}
