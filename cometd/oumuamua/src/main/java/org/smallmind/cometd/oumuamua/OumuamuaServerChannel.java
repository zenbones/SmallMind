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
package org.smallmind.cometd.oumuamua;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.Authorizer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.cometd.oumuamua.message.OumuamuaLazyPacket;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;

public class OumuamuaServerChannel implements ServerChannel {

  private final OumuamuaServer oumuamuaServer;
  private final ReentrantReadWriteLock lifeLock = new ReentrantReadWriteLock();
  private final ConcurrentHashMap<String, OumuamuaServerSession> subscriptionMap = new ConcurrentHashMap<>();
  private final HashMap<String, Object> attributeMap = new HashMap<>();
  private final ConcurrentLinkedQueue<ServerChannelListener> listenerList = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<ServerChannelListener.Weak> weakListenerList = new ConcurrentLinkedQueue<>();
  private final LinkedList<Authorizer> authorizerList = new LinkedList<>();
  private final ChannelId channelId;
  private final boolean meta;
  private final boolean service;
  private final boolean broadcast;
  private boolean persistent;
  private boolean broadcastToPublisher;
  private boolean lazy;
  private long expirationTimestamp;
  private long lazyTimeout = -1;

  public OumuamuaServerChannel (OumuamuaServer oumuamuaServer, ChannelId channelId) {

    this.oumuamuaServer = oumuamuaServer;
    this.channelId = channelId;

    meta = channelId.isMeta();
    service = channelId.isService();
    broadcast = !(meta || service);

    expirationTimestamp = System.currentTimeMillis() + (oumuamuaServer.getConfiguration().getInactiveChannelLifetimeMinutes() * 60 * 1000L);
  }

  public boolean hasExpired (long now) {

    lifeLock.readLock().lock();

    try {

      return (expirationTimestamp > 0) && (expirationTimestamp < now);
    } finally {
      lifeLock.readLock().unlock();
    }
  }

  // Call from synchronized methods only
  private void checkTimeToLive () {

    if ((expirationTimestamp < 0) && (!persistent) && subscriptionMap.isEmpty() && listenerList.isEmpty()) {
      expirationTimestamp = System.currentTimeMillis() + (oumuamuaServer.getConfiguration().getInactiveChannelLifetimeMinutes() * 60 * 1000L);
    }
  }

  @Override
  public String getId () {

    return channelId.getId();
  }

  @Override
  public ChannelId getChannelId () {

    return channelId;
  }

  @Override
  public boolean isMeta () {

    return meta;
  }

  @Override
  public boolean isService () {

    return service;
  }

  @Override
  public boolean isBroadcast () {

    return broadcast;
  }

  @Override
  public boolean isWild () {

    return channelId.isWild();
  }

  @Override
  public boolean isDeepWild () {

    return channelId.isDeepWild();
  }

  @Override
  public boolean isLazy () {

    return lazy;
  }

  @Override
  public void setLazy (boolean lazy) {

    this.lazy = lazy;
  }

  @Override
  public long getLazyTimeout () {

    return lazyTimeout;
  }

  @Override
  public void setLazyTimeout (long lazyTimeout) {

    this.lazyTimeout = lazyTimeout;
  }

  @Override
  public boolean isPersistent () {

    lifeLock.readLock().lock();

    try {

      return persistent;
    } finally {
      lifeLock.readLock().unlock();
    }
  }

  @Override
  public void setPersistent (boolean persistent) {

    lifeLock.writeLock().lock();

    try {
      if (persistent != this.persistent) {
        this.persistent = persistent;

        if (persistent) {
          expirationTimestamp = -1;
        } else {
          checkTimeToLive();
        }
      }
    } finally {
      lifeLock.writeLock().unlock();
    }
  }

  @Override
  public boolean isBroadcastToPublisher () {

    return broadcastToPublisher;
  }

  @Override
  public void setBroadcastToPublisher (boolean broadcastToPublisher) {

    this.broadcastToPublisher = broadcastToPublisher;
  }

  @Override
  public List<Authorizer> getAuthorizers () {

    return authorizerList;
  }

  @Override
  public void addAuthorizer (Authorizer authorizer) {

    authorizerList.add(authorizer);
  }

  @Override
  public void removeAuthorizer (Authorizer authorizer) {

    authorizerList.remove(authorizer);
  }

  @Override
  public void setAttribute (String name, Object value) {

    attributeMap.put(name, value);
  }

  @Override
  public Object getAttribute (String name) {

    return attributeMap.get(name);
  }

  @Override
  public Set<String> getAttributeNames () {

    return attributeMap.keySet();
  }

  @Override
  public Object removeAttribute (String name) {

    return attributeMap.remove(name);
  }

  @Override
  // Do not call this in order to process listeners
  public List<ServerChannelListener> getListeners () {

    LinkedList<ServerChannelListener> joinedList = new LinkedList<>(weakListenerList);

    joinedList.addAll(listenerList);

    return joinedList;
  }

  @Override
  public void addListener (ServerChannelListener listener) {

    lifeLock.writeLock().lock();

    try {
      if (ServerChannelListener.Weak.class.isAssignableFrom(listener.getClass())) {
        weakListenerList.add((ServerChannelListener.Weak)listener);
      } else {
        listenerList.add(listener);
        expirationTimestamp = -1;
      }
    } finally {
      lifeLock.writeLock().unlock();
    }
  }

  @Override
  public void removeListener (ServerChannelListener listener) {

    lifeLock.writeLock().lock();

    try {
      if (ServerChannelListener.Weak.class.isAssignableFrom(listener.getClass())) {
        weakListenerList.remove(listener);
      } else if (listenerList.remove(listener)) {
        if (listenerList.isEmpty()) {
          checkTimeToLive();
        }
      }
    } finally {
      lifeLock.writeLock().unlock();
    }
  }

  public boolean isSubscribed () {

    return !subscriptionMap.isEmpty();
  }

  @Override
  // Do not call this in order to process subscriptions
  public Set<ServerSession> getSubscribers () {

    return new HashSet<>(subscriptionMap.values());
  }

  @Override
  public boolean subscribe (ServerSession session) {

    lifeLock.writeLock().lock();

    try {
      subscriptionMap.putIfAbsent(session.getId(), (OumuamuaServerSession)session);
      expirationTimestamp = -1;

      return true;
    } finally {
      lifeLock.writeLock().unlock();
    }
  }

  @Override
  public boolean unsubscribe (ServerSession session) {

    lifeLock.writeLock().lock();

    try {
      ServerSession serverSession;

      if ((serverSession = subscriptionMap.remove(session.getId())) == null) {

        return false;
      } else {
        // TODO: session listener???

        if (subscriptionMap.isEmpty()) {
          checkTimeToLive();
        }

        return true;
      }
    } finally {
      lifeLock.writeLock().unlock();
    }
  }

  @Override
  public void publish (Session from, ServerMessage.Mutable message, Promise<Boolean> promise) {

  }

  @Override
  public void publish (Session from, Object data, Promise<Boolean> promise) {

  }

  public void send (OumuamuaPacket packet, HashSet<String> sessionIdSet) {

    OumuamuaPacket downstreamPacket = null;

    for (OumuamuaServerSession serverSession : subscriptionMap.values()) {
      if (sessionIdSet.add(serverSession.getId())) {

        if (downstreamPacket == null) {
          downstreamPacket = lazy ? (lazyTimeout > 0) ? new OumuamuaLazyPacket(packet, System.currentTimeMillis() + lazyTimeout) : packet : packet;
        }

        serverSession.send(downstreamPacket);
      }
    }
  }

  @Override
  public void remove () {

    oumuamuaServer.removeChannel(this);
  }
}
