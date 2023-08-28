/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;

public class OumuamuaChannel<V extends Value<V>> extends AbstractAttributed implements Channel<V> {

  private final DefaultRoute route;
  private final ConcurrentHashMap<String, Session<V>> sessionMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<Listener<V>> listenerList = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean reflecting = new AtomicBoolean();
  private final long timeToLiveMilliseconds;
  private boolean persistent;
  private boolean terminal;
  private long quiescentTimestamp;
  private int persistentListenerCount;

  public OumuamuaChannel (long timeToLiveMilliseconds, DefaultRoute route) {

    this.route = route;
    this.timeToLiveMilliseconds = timeToLiveMilliseconds;
  }

  public DefaultRoute getRoute () {

    return route;
  }

  private void onSubscribed (Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onSubscribed(session);
      }
    }
  }

  private void onUnsubscribed (Session<V> session) {

    for (Listener<V> listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener<V>)listener).onUnsubscribed(session);
      }
    }
  }

  private void onDelivery (Packet<V> packet) {

    if (PacketType.DELIVERY.equals(packet.getPacketType())) {
      for (Listener<V> listener : listenerList) {
        if (PacketListener.class.isAssignableFrom(listener.getClass())) {
          ((PacketListener<V>)listener).onDelivery(packet);
        }
      }
    }
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
  public boolean isWild () {

    return route.isWild();
  }

  @Override
  public boolean isDeepWild () {

    return route.isDeepWild();
  }

  @Override
  public boolean isMeta () {

    return route.isMeta();
  }

  @Override
  public boolean isService () {

    return route.isService();
  }

  @Override
  public boolean isDeliverable () {

    return route.isDeliverable();
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
  public synchronized boolean subscribe (Session<V> session) {

    boolean success = false;

    if (!terminal) {
      if (sessionMap.putIfAbsent(session.getId(), session) == null) {
        success = true;
        onSubscribed(session);
      }

      quiescentTimestamp = 0;
    }

    return success;
  }

  @Override
  public synchronized boolean unsubscribe (Session<V> session) {

    boolean success = false;

    if (sessionMap.remove(session.getId()) != null) {
      success = true;
      onUnsubscribed(session);

      if (sessionMap.isEmpty() && (persistentListenerCount <= 0)) {
        quiescentTimestamp = System.currentTimeMillis();
      }
    }

    return success;
  }

  @Override
  public synchronized boolean isRemovable (long now) {

    return (!persistent) && (quiescentTimestamp > 0) && ((now - quiescentTimestamp) >= timeToLiveMilliseconds);
  }

  public synchronized void terminate () {

    terminal = true;
  }

  public synchronized void clearSubscriptions () {

    terminal = true;

    for (Session<V> session : sessionMap.values()) {
      onUnsubscribed(session);
    }

    sessionMap.clear();

    if (persistentListenerCount <= 0) {
      quiescentTimestamp = System.currentTimeMillis();
    }
  }

  @Override
  public void deliver (Packet<V> packet, Set<String> sessionIdSet) {

    Packet<V> frozenPacket = PacketUtility.freezePacket(packet);

    onDelivery(frozenPacket);

    for (Session<V> session : sessionMap.values()) {
      if (sessionIdSet.add(session.getId()) && ((!session.getId().equals(frozenPacket.getSenderId())) || reflecting.get())) {
        session.deliver(frozenPacket);
      }
    }
  }
}
