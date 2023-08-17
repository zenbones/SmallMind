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
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Route;

public class OumuamuaChannel extends AbstractAttributed implements Channel {

  private final Route route;
  private final ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<Listener> listenerList = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean reflecting = new AtomicBoolean();
  private final long timeToLive;
  private boolean persistent;
  private long removableTimestamp;
  private int persistentListenerCount;

  public OumuamuaChannel (Route route, long timeToLive) {

    this.route = route;
    this.timeToLive = timeToLive;
  }

  public Route getRoute () {

    return route;
  }

  private void onSubscribed (Session session) {

    for (Listener listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener)listener).onSubscribed(session);
      }
    }
  }

  private void onUnsubscribed (Session session) {

    for (Listener listener : listenerList) {
      if (SessionListener.class.isAssignableFrom(listener.getClass())) {
        ((SessionListener)listener).onUnsubscribed(session);
      }
    }
  }

  private void onDelivery (Packet packet) {

    for (Listener listener : listenerList) {
      if (PacketListener.class.isAssignableFrom(listener.getClass())) {
        ((PacketListener)listener).onDelivery(packet);
      }
    }
  }

  @Override
  public synchronized void addListener (Listener listener) {

    if (listenerList.add(listener) && listener.isPersistent()) {
      persistentListenerCount++;
      removableTimestamp = 0;
    }
  }

  @Override
  public synchronized void removeListener (Listener listener) {

    if (listenerList.remove(listener) && listener.isPersistent()) {
      if ((--persistentListenerCount <= 0) && sessionMap.isEmpty()) {
        removableTimestamp = System.currentTimeMillis();
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
  public synchronized void subscribe (Session session) {

    if (sessionMap.putIfAbsent(session.getId(), session) == null) {
      onSubscribed(session);
    }

    removableTimestamp = 0;
  }

  @Override
  public synchronized void unsubscribe (Session session) {

    if (sessionMap.remove(session.getId()) != null) {
      onUnsubscribed(session);

      if (sessionMap.isEmpty() && (persistentListenerCount <= 0)) {
        removableTimestamp = System.currentTimeMillis();
      }
    }
  }

  @Override
  public synchronized boolean isRemovable () {

    return (!persistent) && (removableTimestamp > 0) && ((System.currentTimeMillis() - removableTimestamp) >= timeToLive);
  }

  @Override
  public void deliver (Packet packet, Set<String> sessionIdSet) {

    Packet frozenPacket = PacketUtility.freezePacket(packet);

    onDelivery(frozenPacket);

    for (Session session : sessionMap.values()) {
      if (sessionIdSet.add(session.getId())) {
        session.deliver(frozenPacket);
      }
    }
  }
}
