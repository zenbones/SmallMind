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

import java.util.Set;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Server-side Bayeux channel that manages subscriptions, listeners, and message delivery to its subscriber set.
 *
 * @param <V> concrete {@link Value} implementation used to represent message payloads
 */
public interface Channel<V extends Value<V>> extends Attributed {

  String WILD = "*";
  String DEEP_WILD = "**";

  /**
   * Base listener type for channel events; implementors may elect to survive server restarts.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface Listener<V extends Value<V>> {

    /**
     * Returns whether this listener should be retained when the server restarts.
     *
     * @return {@code true} if the listener survives restart
     */
    boolean isPersistent ();
  }

  /**
   * Notified when sessions subscribe to or unsubscribe from the channel.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface SessionListener<V extends Value<V>> extends Listener<V> {

    /**
     * Called immediately after a session successfully subscribes.
     *
     * @param session the session that subscribed
     */
    void onSubscribed (Session<V> session);

    /**
     * Called immediately after a session unsubscribes.
     *
     * @param session the session that unsubscribed
     */
    void onUnsubscribed (Session<V> session);
  }

  // Messages are frozen before this call, guaranteeing that all sessions in this delivery stream, but not others, see any changes generated here

  /**
   * Intercepts packet delivery to channel subscribers; messages are frozen before this call, so
   * mutations are visible only within the current delivery stream.
   *
   * @param <V> concrete {@link Value} implementation
   */
  interface PacketListener<V extends Value<V>> extends Listener<V> {

    /**
     * Returns whether this listener should be retained when the server restarts.
     *
     * @return {@code true} if the listener survives restart
     */
    boolean isPersistent ();

    // For published messages delivered to receivers

    /**
     * Called when a publish packet is about to be delivered to subscribers; may return a
     * replacement packet.
     *
     * @param sender session that originally published the packet
     * @param packet frozen packet being distributed
     * @return packet to continue delivering, possibly a replacement
     */
    Packet<V> onDelivery (Session<V> sender, Packet<V> packet);
  }

  /**
   * Registers a listener to receive channel events.
   *
   * @param listener listener to add
   */
  void addListener (Listener<V> listener);

  /**
   * Deregisters a previously added listener.
   *
   * @param listener listener to remove
   */
  void removeListener (Listener<V> listener);

  /**
   * Returns the route that identifies this channel's path.
   *
   * @return route for this channel
   */
  Route getRoute ();

  /**
   * Returns whether this channel's route ends with a single-level wildcard ({@code *}).
   *
   * @return {@code true} if the route is wild
   */
  default boolean isWild () {

    return getRoute().isWild();
  }

  /**
   * Returns whether this channel's route ends with a deep wildcard ({@code **}).
   *
   * @return {@code true} if the route is deep wild
   */
  default boolean isDeepWild () {

    return getRoute().isDeepWild();
  }

  /**
   * Returns whether this is a meta channel ({@code /meta/...}).
   *
   * @return {@code true} for meta channels
   */
  default boolean isMeta () {

    return getRoute().isMeta();
  }

  /**
   * Returns whether this is a service channel ({@code /service/...}).
   *
   * @return {@code true} for service channels
   */
  default boolean isService () {

    return getRoute().isService();
  }

  /**
   * Returns whether user messages can be published and delivered on this channel.
   *
   * @return {@code true} when the channel accepts user publications
   */
  default boolean isDeliverable () {

    return getRoute().isDeliverable();
  }

  /**
   * Returns whether this channel is marked persistent and should not be removed when empty.
   *
   * @return {@code true} if persistent
   */
  boolean isPersistent ();

  /**
   * Controls whether the channel is kept alive when it has no subscribers.
   *
   * @param persistent {@code true} to prevent automatic removal when empty
   */
  void setPersistent (boolean persistent);

  /**
   * Returns whether published messages are echoed back to the publishing session.
   *
   * @return {@code true} if the sender receives its own messages
   */
  boolean isReflecting ();

  /**
   * Controls whether published messages are echoed back to the publishing session.
   *
   * @param reflecting {@code true} to echo messages to the sender
   */
  void setReflecting (boolean reflecting);

  /**
   * Returns whether the channel delivers each message individually without batching.
   *
   * @return {@code true} if streaming delivery is active
   */
  boolean isStreaming ();

  /**
   * Controls whether the channel delivers messages individually (streaming) or batched.
   *
   * @param streaming {@code true} for per-message streaming delivery
   */
  void setStreaming (boolean streaming);

  /**
   * Subscribes a session to receive deliveries on this channel.
   *
   * @param session session to subscribe
   * @return {@code true} if the session was newly added; {@code false} if already subscribed
   */
  boolean subscribe (Session<V> session);

  /**
   * Removes a session's subscription from this channel.
   *
   * @param session session to unsubscribe
   */
  void unsubscribe (Session<V> session);

  /**
   * Evaluates whether this channel is eligible for automatic removal given the current time.
   *
   * @param now current wall-clock time in milliseconds
   * @return {@code true} if the channel can be removed (e.g., non-persistent and empty)
   */
  boolean isRemovable (long now);

  /**
   * Pushes a packet to the sessions in the provided id set, invoking packet listeners along the way.
   *
   * @param sender       session originating the packet
   * @param packet       packet to deliver
   * @param sessionIdSet identifiers of sessions that should receive the packet
   */
  void deliver (Session<V> sender, Packet<V> packet, Set<String> sessionIdSet);

  /**
   * Wraps the given data in a delivery packet and distributes it to all subscribers.
   *
   * @param data JSON object to publish as the message payload
   */
  void publish (ObjectValue<V> data);
}
