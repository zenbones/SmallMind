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

  public OumuamuaChannel (BiConsumer<Channel<V>, Session<V>> onSubscribedCallback, BiConsumer<Channel<V>, Session<V>> onUnsubscribedCallback, long timeToLiveMilliseconds, DefaultRoute route, ChannelRoot<V> root) {

    this.onSubscribedCallback = onSubscribedCallback;
    this.onUnsubscribedCallback = onUnsubscribedCallback;
    this.timeToLiveMilliseconds = timeToLiveMilliseconds;
    this.route = route;
    this.root = root;

    reflecting.set(root.isReflective(route));
    streaming.set(root.isStreaming(route));

    quiescentTimestamp = System.currentTimeMillis();
  }

  private void onSubscribed (Session<V> session) {

    onSubscribedCallback.accept(this, session);

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onSubscribed(session);
      }
    }
  }

  private void onUnsubscribed (Session<V> session) {

    onUnsubscribedCallback.accept(this, session);

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onUnsubscribed(session);
      }
    }
  }

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

  @Override
  public synchronized void addListener (Listener<V> listener) {

    if (!terminal) {
      if (listenerList.add(listener) && listener.isPersistent()) {
        persistentListenerCount++;
        quiescentTimestamp = 0;
      }
    }
  }

  @Override
  public synchronized void removeListener (Listener<V> listener) {

    if (listenerList.remove(listener) && listener.isPersistent()) {
      if ((--persistentListenerCount <= 0) && sessionMap.isEmpty()) {
        quiescentTimestamp = System.currentTimeMillis();
      }
    }
  }

  @Override
  public Route getRoute () {

    return route;
  }

  @Override
  public synchronized boolean isPersistent () {

    return persistent;
  }

  @Override
  public synchronized void setPersistent (boolean persistent) {

    this.persistent = persistent;
  }

  @Override
  public boolean isReflecting () {

    return reflecting.get();
  }

  @Override
  public void setReflecting (boolean reflecting) {

    this.reflecting.set(reflecting);
  }

  @Override
  public boolean isStreaming () {

    return streaming.get();
  }

  @Override
  public void setStreaming (boolean streaming) {

    this.streaming.set(streaming);
  }

  @Override
  public synchronized boolean subscribe (Session<V> session) {

    if (terminal) {

      return false;
    }
    if (sessionMap.putIfAbsent(session.getId(), session) == null) {
      onSubscribed(session);
    }

    quiescentTimestamp = 0;

    return true;
  }

  @Override
  public synchronized void unsubscribe (Session<V> session) {

    Session<V> unsubscribedSession;

    if ((unsubscribedSession = sessionMap.remove(session.getId())) != null) {
      onUnsubscribed(unsubscribedSession);

      if (sessionMap.isEmpty() && (persistentListenerCount <= 0)) {
        quiescentTimestamp = System.currentTimeMillis();
      }
    }
  }

  @Override
  public synchronized boolean isRemovable (long now) {

    return (!persistent) && (quiescentTimestamp > 0) && ((now - quiescentTimestamp) >= timeToLiveMilliseconds);
  }

  public synchronized OumuamuaChannel<V> terminate () {

    HashSet<Session<V>> unsubscribedSet = new HashSet<>(sessionMap.values());

    terminal = true;
    sessionMap.clear();

    for (Session<V> unsubscribedSession : unsubscribedSet) {
      onUnsubscribed(unsubscribedSession);
    }

    if (persistentListenerCount <= 0) {
      quiescentTimestamp = System.currentTimeMillis();
    }

    return this;
  }

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

  @Override
  public void publish (ObjectValue<V> data) {

    root.forward(this, new Packet<>(PacketType.DELIVERY, null, getRoute(), (Message<V>)root.getCodec().create().put(Message.CHANNEL, getRoute().getPath()).put(Message.DATA, data)));
  }
}
