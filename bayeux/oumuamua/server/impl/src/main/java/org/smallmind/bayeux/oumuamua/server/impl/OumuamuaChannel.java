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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.nutsnbolts.util.Pair;

/**
 * Concrete Bayeux channel that tracks subscribed sessions, channel-scoped listeners,
 * reflection/streaming flags, and a time-to-live for idle cleanup.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class OumuamuaChannel<V extends Value<V>> extends AbstractAttributed implements Channel<V> {

  private final DefaultRoute route;
  private final ChannelRoot<V> root;
  private final ConcurrentHashMap<String, Session<V>> sessionMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<Listener<V>> listenerList = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean reflecting = new AtomicBoolean();
  private final AtomicBoolean streaming = new AtomicBoolean();
  private final BiConsumer<Channel<V>, Session<V>> onSubscribedCallback;
  private final BiConsumer<Channel<V>, Session<V>> onUnsubscribedCallback;
  private final long timeToLiveMilliseconds;
  private boolean persistent;
  private boolean terminal;
  private long quiescentTimestamp;
  private int persistentListenerCount;

  /**
   * Creates a channel bound to the given route with reflection and streaming settings derived from
   * the server configuration.
   *
   * @param onSubscribedCallback   invoked with this channel and the session whenever a new
   *                               subscription is recorded
   * @param onUnsubscribedCallback invoked with this channel and the session whenever a subscription
   *                               is removed
   * @param timeToLiveMilliseconds how long the channel may remain quiescent before it becomes
   *                               eligible for removal
   * @param route                  the Bayeux route this channel represents
   * @param root                   server-level facade used for codec, backbone, and config access
   */
  public OumuamuaChannel (BiConsumer<Channel<V>, Session<V>> onSubscribedCallback, BiConsumer<Channel<V>, Session<V>> onUnsubscribedCallback, long timeToLiveMilliseconds, DefaultRoute route, ChannelRoot<V> root) {

    this.onSubscribedCallback = onSubscribedCallback;
    this.onUnsubscribedCallback = onUnsubscribedCallback;
    this.timeToLiveMilliseconds = timeToLiveMilliseconds;
    this.route = route;
    this.root = root;

    reflecting.set(root.isReflecting(route));
    streaming.set(root.isStreaming(route));

    quiescentTimestamp = System.currentTimeMillis();
  }

  /**
   * Fires the server-level subscription callback and all channel-scoped {@link SessionListener}s.
   *
   * @param session the session that just subscribed
   */
  private void onSubscribed (Session<V> session) {

    onSubscribedCallback.accept(this, session);

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onSubscribed(session);
      }
    }
  }

  /**
   * Fires the server-level unsubscription callback and all channel-scoped {@link SessionListener}s.
   *
   * @param session the session that just unsubscribed
   */
  protected void onUnsubscribed (Session<V> session) {

    onUnsubscribedCallback.accept(this, session);

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onUnsubscribed(session);
      }
    }
  }

  /**
   * Runs a delivery packet through every channel-scoped {@link PacketListener}, allowing each one
   * to transform or veto the delivery.
   *
   * @param sender the session originating the delivery, or {@code null} for server-side publishes
   * @param packet the delivery packet; only {@link PacketType#DELIVERY} packets are processed
   * @return the (possibly transformed) packet, or {@code null} if a listener vetoed delivery
   */
  private Packet<V> onProcessing (Session<V> sender, Packet<V> packet) {

    if (PacketType.DELIVERY.equals(packet.getPacketType())) {
      for (Listener<V> listener : listenerList) {
        if (PacketListener.class.isAssignableFrom(listener.getClass())) {
          if ((packet = ((PacketListener<V>)listener).onDelivery(sender, packet)) == null) {
            break;
          }
        }
      }
    }

    return packet;
  }

  /**
   * Appends a listener to the channel's listener chain; persistent listeners also reset the
   * quiescent timestamp so the channel is not removed while they remain registered.
   *
   * @param listener the listener to add; ignored if the channel has been terminated
   */
  @Override
  public synchronized void addListener (Listener<V> listener) {

    if (!terminal) {
      if (listenerList.add(listener) && listener.isPersistent()) {
        persistentListenerCount++;
        quiescentTimestamp = 0;
      }
    }
  }

  /**
   * Removes a previously added listener; starts the idle timer when no persistent listeners or
   * subscribers remain.
   *
   * @param listener the listener to remove; no-op if not present
   */
  @Override
  public synchronized void removeListener (Listener<V> listener) {

    if (listenerList.remove(listener) && listener.isPersistent()) {
      if ((--persistentListenerCount <= 0) && sessionMap.isEmpty()) {
        quiescentTimestamp = System.currentTimeMillis();
      }
    }
  }

  /**
   * Returns the Bayeux route this channel is registered under.
   *
   * @return the immutable route; never {@code null}
   */
  @Override
  public Route getRoute () {

    return route;
  }

  /**
   * Indicates whether the channel has been pinned and will not be pruned by the idle sweep.
   *
   * @return {@code true} if the channel is persistent
   */
  @Override
  public synchronized boolean isPersistent () {

    return persistent;
  }

  /**
   * Controls whether the channel survives idle cleanup.
   *
   * @param persistent {@code true} to exempt the channel from TTL-based removal
   */
  @Override
  public synchronized void setPersistent (boolean persistent) {

    this.persistent = persistent;
  }

  /**
   * Indicates whether deliveries are reflected back to the publishing session.
   *
   * @return {@code true} if the sender also receives its own messages
   */
  @Override
  public boolean isReflecting () {

    return reflecting.get();
  }

  /**
   * Sets whether published messages are echoed back to the publishing session.
   *
   * @param reflecting {@code true} to enable self-delivery for the publisher
   */
  @Override
  public void setReflecting (boolean reflecting) {

    this.reflecting.set(reflecting);
  }

  /**
   * Indicates whether messages on this channel are pushed directly over the active connection
   * rather than being queued for long polling.
   *
   * @return {@code true} if streaming delivery is enabled
   */
  @Override
  public boolean isStreaming () {

    return streaming.get();
  }

  /**
   * Controls whether messages bypass the long-poll queue and are sent immediately over the
   * active connection.
   *
   * @param streaming {@code true} to enable streaming delivery
   */
  @Override
  public void setStreaming (boolean streaming) {

    this.streaming.set(streaming);
  }

  /**
   * Adds the session to the subscriber map and resets the idle timer; fires the subscription
   * callback if this is the first time the session subscribes.
   *
   * @param session the session to subscribe
   * @return {@code true} if the subscription was accepted; {@code false} if the channel has been
   * terminated
   */
  @Override
  public boolean subscribe (Session<V> session) {

    Session<V> subcribedSession = null;

    synchronized (this) {
      if (terminal) {

        return false;
      } else {
        if (sessionMap.putIfAbsent(session.getId(), session) == null) {
          subcribedSession = session;
        }

        quiescentTimestamp = 0;
      }
    }

    if (subcribedSession != null) {
      onSubscribed(subcribedSession);
    }

    return true;
  }

  /**
   * Removes the session from the subscriber map and fires the unsubscription callback; starts the
   * idle timer if no subscribers or persistent listeners remain.
   *
   * @param session the session to unsubscribe; no-op if the session is not currently subscribed
   */
  @Override
  public void unsubscribe (Session<V> session) {

    Session<V> unsubscribedSession = null;

    synchronized (this) {
      if ((unsubscribedSession = sessionMap.remove(session.getId())) != null) {
        if (sessionMap.isEmpty() && (persistentListenerCount <= 0)) {
          quiescentTimestamp = System.currentTimeMillis();
        }
      }
    }

    if (unsubscribedSession != null) {
      onUnsubscribed(unsubscribedSession);
    }
  }

  /**
   * Determines whether the channel has been idle long enough to be pruned from the tree.
   *
   * @param now the current epoch millisecond timestamp to compare against the quiescent start time
   * @return {@code true} if the channel is non-persistent, has no active subscribers or persistent
   * listeners, and has exceeded its configured time-to-live
   */
  @Override
  public synchronized boolean isRemovable (long now) {

    return (!persistent) && (quiescentTimestamp > 0) && ((now - quiescentTimestamp) >= timeToLiveMilliseconds);
  }

  /**
   * Irreversibly closes the channel: flags it terminal, clears all subscribers, and sets the
   * quiescent timestamp when no persistent listeners remain.  The caller is responsible for
   * firing the unsubscription callback for each session in the returned set.
   *
   * @return a {@link org.smallmind.nutsnbolts.util.Pair} of this channel and the set of sessions
   * that were subscribed at the time of termination; the caller must iterate the set and invoke
   * {@link #onUnsubscribed(Session)} for each entry
   */
  public synchronized Pair<OumuamuaChannel<V>, Set<Session<V>>> terminate () {

    HashSet<Session<V>> unsubscribedSet = new HashSet<>(sessionMap.values());

    terminal = true;
    sessionMap.clear();

    if (persistentListenerCount <= 0) {
      quiescentTimestamp = System.currentTimeMillis();
    }

    return Pair.of(this, unsubscribedSet);
  }

  /**
   * Pushes the packet to every subscribed session, skipping sessions that have already received
   * it during this delivery wave and honoring the reflection setting for the sender.
   *
   * @param sender       the session that published the packet, or {@code null} for server-sourced
   *                     deliveries; excluded from delivery unless reflection is enabled
   * @param packet       the packet to deliver; frozen before channel-listener processing so that
   *                     listener changes are scoped to this channel's delivery stream
   * @param sessionIdSet accumulates ids of sessions already delivered to, preventing duplicates
   *                     when wildcard channels overlap
   */
  @Override
  public void deliver (Session<V> sender, Packet<V> packet, Set<String> sessionIdSet) {

    Packet<V> processedPacket;

    // Changes by channel listeners here will be seen only by sessions in this delivery stream
    if ((processedPacket = onProcessing(sender, PacketUtility.freezePacket(packet))) != null) {

      for (Session<V> session : sessionMap.values()) {
        if (sessionIdSet.add(session.getId()) && ((processedPacket.getSenderId() == null) || (!session.getId().equals(processedPacket.getSenderId())) || reflecting.get())) {
          // Changes made by session listeners further down the line will be seen only by the hosting session
          session.deliver(this, sender, PacketUtility.freezePacket(processedPacket));
        }
      }
    }
  }

  /**
   * Constructs a delivery message carrying {@code data} and forwards it through the server
   * backbone so that all nodes in the cluster deliver it to their subscribers.
   *
   * @param data the payload to publish; wrapped in a Bayeux message with the channel path set
   */
  @Override
  public void publish (ObjectValue<V> data) {

    root.forward(this, new Packet<>(PacketType.DELIVERY, null, getRoute(), (Message<V>)root.getCodec().create().put(Message.CHANNEL, getRoute().getPath()).put(Message.DATA, data)));
  }
}
