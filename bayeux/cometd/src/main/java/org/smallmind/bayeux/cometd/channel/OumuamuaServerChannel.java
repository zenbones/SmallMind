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
package org.smallmind.bayeux.cometd.channel;

import java.util.HashSet;
import java.util.Iterator;
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
import org.cometd.bayeux.server.BayeuxContext;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.smallmind.bayeux.cometd.OumuamuaServer;
import org.smallmind.bayeux.cometd.message.ExtMapLike;
import org.smallmind.bayeux.cometd.message.MapLike;
import org.smallmind.bayeux.cometd.message.MapMessageGenerator;
import org.smallmind.bayeux.cometd.message.MessageGenerator;
import org.smallmind.bayeux.cometd.message.OumuamuaLazyPacket;
import org.smallmind.bayeux.cometd.message.OumuamuaPacket;
import org.smallmind.bayeux.cometd.message.PacketUtility;
import org.smallmind.bayeux.cometd.session.OumuamuaServerSession;
import org.smallmind.bayeux.cometd.session.SessionUtility;
import org.smallmind.bayeux.cometd.transport.OumuamuaCarrier;
import org.smallmind.bayeux.cometd.transport.OumuamuaTransport;

public class OumuamuaServerChannel implements ServerChannel {

  private final OumuamuaServer oumuamuaServer;
  private final ReentrantReadWriteLock lifeLock = new ReentrantReadWriteLock();
  private final ConcurrentHashMap<String, OumuamuaServerSession> subscriptionMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Object> attributeMap = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<ServerChannelListener> listenerList = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Authorizer> authorizerList = new ConcurrentLinkedQueue<>();
  private final ChannelId channelId;
  private boolean initialized;
  private boolean persistent;
  private boolean broadcastToPublisher;
  private boolean lazy;
  private long expirationTimestamp;
  private long lazyTimeout = -1;

  public OumuamuaServerChannel (OumuamuaServer oumuamuaServer, ChannelId channelId) {

    this.oumuamuaServer = oumuamuaServer;
    this.channelId = channelId;

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

    return channelId.isMeta();
  }

  @Override
  public boolean isService () {

    return channelId.isService();
  }

  @Override
  public boolean isBroadcast () {

    return !(channelId.isMeta() || channelId.isService());
  }

  @Override
  public boolean isWild () {

    return channelId.isWild();
  }

  @Override
  public boolean isDeepWild () {

    return channelId.isDeepWild();
  }

  public boolean isInitialized () {

    return initialized;
  }

  public void setInitialized (boolean initialized) {

    this.initialized = initialized;
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

    if (lazyTimeout > 0) {
      lazy = true;
    }
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

  public Iterator<Authorizer> iterateAuthorizers () {

    return authorizerList.iterator();
  }

  @Override
  public List<Authorizer> getAuthorizers () {

    return new LinkedList<>(authorizerList);
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

    return new LinkedList<>(listenerList);
  }

  @Override
  public void addListener (ServerChannelListener listener) {

    lifeLock.writeLock().lock();

    try {
      listenerList.add(listener);
      if (!ServerChannelListener.Weak.class.isAssignableFrom(listener.getClass())) {
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
      if (listenerList.remove(listener)) {

        boolean onlyWeak = true;

        for (ServerChannelListener item : listenerList) {
          if (!ServerChannelListener.Weak.class.isAssignableFrom(item.getClass())) {
            onlyWeak = false;
            break;
          }
        }

        if (onlyWeak) {
          checkTimeToLive();
        }
      }
    } finally {
      lifeLock.writeLock().unlock();
    }
  }

  public void onSubscribe (ServerSession serverSession, MessageGenerator messageGenerator) {

    for (ServerChannelListener listener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(listener.getClass())) {
        ((SubscriptionListener)listener).subscribed(serverSession, this, (messageGenerator == null) ? null : messageGenerator.generate());
      }
    }
  }

  public void onUnsubscribe (ServerSession serverSession, MessageGenerator messageGenerator) {

    for (ServerChannelListener listener : listenerList) {
      if (SubscriptionListener.class.isAssignableFrom(listener.getClass())) {
        ((SubscriptionListener)listener).unsubscribed(serverSession, this, messageGenerator.generate());
      }
    }
  }

  public OumuamuaPacket onMessageSent (OumuamuaTransport transport, OumuamuaPacket packet) {

    LinkedList<ExtMapLike> messageList = new LinkedList<>();

    for (MapLike mapLike : packet.getMessages()) {

      MessageGenerator messageGenerator = null;
      boolean promote = true;

      for (ServerChannelListener listener : listenerList) {
        if (MessageListener.class.isAssignableFrom(listener.getClass())) {

          Promise.Completable<Boolean> promise;

          if (messageGenerator == null) {

            BayeuxContext context = null;
            OumuamuaCarrier carrier;

            if ((carrier = packet.getSender().getCarrier()) != null) {
              context = carrier.getContext();
            }

            messageGenerator = new MapMessageGenerator(context, transport, packet.getChannelId(), mapLike, lazy);
          }
          ((MessageListener)listener).onMessage(packet.getSender(), this, messageGenerator.generate(), promise = new Promise.Completable<>());
          if (!promise.join()) {
            promote = false;
            break;
          }
        }
      }

      if (promote) {
        messageList.add(new ExtMapLike(mapLike));
      }
    }

    return new OumuamuaPacket(packet.getSender(), packet.getChannelId(), messageList.toArray(new MapLike[0]));
  }

  public boolean isSubscribed (String sessionId) {

    return subscriptionMap.containsKey(sessionId);
  }

  @Override
  // Do not call this in order to process subscriptions
  public Set<ServerSession> getSubscribers () {

    return new HashSet<>(subscriptionMap.values());
  }

  @Override
  public boolean subscribe (ServerSession serverSession) {

    return subscribe(serverSession, null);
  }

  public boolean subscribe (ServerSession serverSession, MessageGenerator messageGenerator) {

    if (isMeta()) {

      return false;
    } else {

      lifeLock.writeLock().lock();

      try {

        boolean updated = false;

        if (subscriptionMap.putIfAbsent(serverSession.getId(), (OumuamuaServerSession)serverSession) == null) {
          updated = true;

          onSubscribe(serverSession, messageGenerator);
        }

        expirationTimestamp = -1;

        return updated;
      } finally {
        lifeLock.writeLock().unlock();
      }
    }
  }

  @Override
  public boolean unsubscribe (ServerSession serverSession) {

    return subscribe(serverSession, null);
  }

  public boolean unsubscribe (ServerSession serverSession, MessageGenerator messageGenerator) {

    lifeLock.writeLock().lock();

    try {
      if (subscriptionMap.remove(serverSession.getId()) == null) {

        return false;
      } else {
        onUnsubscribe(serverSession, messageGenerator);

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
  public void remove () {

    oumuamuaServer.removeChannel(this);
  }

  @Override
  public void publish (Session from, ServerMessage.Mutable message, Promise<Boolean> promise) {

    send((OumuamuaTransport)SessionUtility.from(from).getServerTransport(), PacketUtility.wrapDeliveryPacket(from, message), new HashSet<>());
    promise.succeed(Boolean.TRUE);
  }

  @Override
  public void publish (Session from, Object data, Promise<Boolean> promise) {

    send((OumuamuaTransport)SessionUtility.from(from).getServerTransport(), PacketUtility.wrapDeliveryPacket(from, getId(), data), new HashSet<>());
    promise.succeed(Boolean.TRUE);
  }

  public void send (OumuamuaTransport transport, OumuamuaPacket packet, HashSet<String> sessionIdSet) {

    if ((packet != null) && (packet.size() > 0)) {

      OumuamuaPacket promotedPacket;
      OumuamuaPacket downstreamPacket = null;

      if ((promotedPacket = onMessageSent(transport, packet)) != null) {
        for (OumuamuaServerSession serverSession : subscriptionMap.values()) {
          if (sessionIdSet.add(serverSession.getId())) {
            if (downstreamPacket == null) {
              downstreamPacket = lazy ? new OumuamuaLazyPacket(promotedPacket, System.currentTimeMillis() + ((lazyTimeout <= 0) ? transport.getMaxLazyTimeout() : lazyTimeout)) : promotedPacket;
            }

            serverSession.send(downstreamPacket);
          }
        }
      }
    }
  }
}
