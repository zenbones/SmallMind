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

import java.util.Set;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Represents a server side channel with listener callbacks and subscription semantics.
 *
 * @param <V> concrete {@link Value} implementation used to represent message payloads
 */
public interface Channel<V extends Value<V>> extends Attributed {

  String WILD = "*";
  String DEEP_WILD = "**";

  /**
   * Marker for channel listeners that can opt into persistence.
   */
  interface Listener<V extends Value<V>> {

    /**
     * Indicates whether the listener is kept across server restarts.
     *
     * @return {@code true} when persistent
     */
    boolean isPersistent ();
  }

  /**
   * Listener that reacts to subscription lifecycle events.
   */
  interface SessionListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called when a session subscribes to the channel.
     *
     * @param session the subscribing session
     */
    void onSubscribed (Session<V> session);

    /**
     * Called when a session unsubscribes from the channel.
     *
     * @param session the unsubscribing session
     */
    void onUnsubscribed (Session<V> session);
  }

  // Messages are frozen before this call, guaranteeing that all sessions in this delivery stream, but not others, see any changes generated here
  /**
   * Listener that can react to packet delivery events.
   */
  interface PacketListener<V extends Value<V>> extends Listener<V> {

    /**
     * Indicates whether the listener is kept across server restarts.
     *
     * @return {@code true} when persistent
     */
    boolean isPersistent ();

    // For published messages delivered to receivers
    /**
     * Called when a packet is being delivered to channel subscribers.
     *
     * @param sender the session that published the packet
     * @param packet the packet being delivered
     * @return a possibly modified packet to distribute
     */
    Packet<V> onDelivery (Session<V> sender, Packet<V> packet);
  }

  /**
   * Registers a listener for channel activity.
   *
   * @param listener the listener to add
   */
  void addListener (Listener<V> listener);

  /**
   * Removes a listener from the channel.
   *
   * @param listener the listener to remove
   */
  void removeListener (Listener<V> listener);

  /**
   * Returns the channel route.
   *
   * @return the route definition
   */
  Route getRoute ();

  /**
   * Indicates whether the route contains a single-level wildcard.
   *
   * @return {@code true} when the route is wild
   */
  default boolean isWild () {

    return getRoute().isWild();
  }

  /**
   * Indicates whether the route contains a multi-level wildcard.
   *
   * @return {@code true} when the route is deep wild
   */
  default boolean isDeepWild () {

    return getRoute().isDeepWild();
  }

  /**
   * Indicates whether the channel is a meta channel.
   *
   * @return {@code true} for meta channels
   */
  default boolean isMeta () {

    return getRoute().isMeta();
  }

  /**
   * Indicates whether the channel is a service channel.
   *
   * @return {@code true} for service channels
   */
  default boolean isService () {

    return getRoute().isService();
  }

  /**
   * Indicates whether the channel can receive published data.
   *
   * @return {@code true} if deliverable
   */
  default boolean isDeliverable () {

    return getRoute().isDeliverable();
  }

  /**
   * Indicates whether the channel persists across disconnects.
   *
   * @return {@code true} when persistent
   */
  boolean isPersistent ();

  /**
   * Sets whether this channel persists across disconnects.
   *
   * @param persistent flag indicating persistence
   */
  void setPersistent (boolean persistent);

  /**
   * Indicates whether messages sent to this channel are reflected back to the sender.
   *
   * @return {@code true} when reflecting
   */
  boolean isReflecting ();

  /**
   * Enables or disables reflection to the sender.
   *
   * @param reflecting {@code true} to reflect
   */
  void setReflecting (boolean reflecting);

  /**
   * Indicates whether the channel streams messages without batching.
   *
   * @return {@code true} when streaming
   */
  boolean isStreaming ();

  /**
   * Enables or disables streaming for this channel.
   *
   * @param streaming {@code true} for streaming behavior
   */
  void setStreaming (boolean streaming);

  /**
   * Adds a session subscription to the channel.
   *
   * @param session the subscribing session
   * @return {@code true} if the session was added
   */
  boolean subscribe (Session<V> session);

  /**
   * Removes a session subscription from the channel.
   *
   * @param session the unsubscribing session
   */
  void unsubscribe (Session<V> session);

  /**
   * Determines whether the channel can be removed due to idleness.
   *
   * @param now current time in milliseconds
   * @return {@code true} if removable
   */
  boolean isRemovable (long now);

  /**
   * Delivers a packet to channel subscribers.
   *
   * @param sender the session sending the packet
   * @param packet the packet to deliver
   * @param sessionIdSet a set of session identifiers that should receive the packet
   */
  void deliver (Session<V> sender, Packet<V> packet, Set<String> sessionIdSet);

  /**
   * Publishes data to the channel, creating a packet for distribution.
   *
   * @param data payload to publish
   */
  void publish (ObjectValue<V> data);
}
