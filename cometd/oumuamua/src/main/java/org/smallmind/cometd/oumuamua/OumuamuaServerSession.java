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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.bayeux.server.ServerTransport;
import org.smallmind.cometd.oumuamua.message.MapLike;
import org.smallmind.cometd.oumuamua.message.MapMessageGenerator;
import org.smallmind.cometd.oumuamua.message.MessageGenerator;
import org.smallmind.cometd.oumuamua.message.MessageUtility;
import org.smallmind.cometd.oumuamua.message.OumuamuaLazyPacket;
import org.smallmind.cometd.oumuamua.message.OumuamuaPacket;
import org.smallmind.cometd.oumuamua.message.PacketType;
import org.smallmind.cometd.oumuamua.transport.OumuamuaCarrier;
import org.smallmind.cometd.oumuamua.transport.OumuamuaTransport;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

public class OumuamuaServerSession implements ServerSession {

  private static final ThreadLocal<LinkedList<OumuamuaPacket>> BATCHED_PACKET_LIST_LOCAL = new ThreadLocal<>();
  private final ReentrantLock messagePollLock = new ReentrantLock();
  private final ConcurrentHashMap<String, Object> attributeMap = new ConcurrentHashMap<>();
  private final ConcurrentSkipListMap<Long, LinkedList<OumuamuaLazyPacket>> lazyMessageQueue = new ConcurrentSkipListMap<>();
  private final ConcurrentLinkedQueue<OumuamuaPacket> messageQueue = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Extension> extensionList = new ConcurrentLinkedQueue<>();
  // TODO: Listeners
  private final ConcurrentLinkedQueue<ServerSessionListener> listenerList = new ConcurrentLinkedQueue<>();
  private final OumuamuaServer oumuamuaServer;
  private final OumuamuaTransport serverTransport;
  private final OumuamuaCarrier carrier;
  private final LocalSession localSession;
  private final String id;
  private final int maximumMessageQueueSize;
  private final int maximumUndeliveredLazyMessageCount;
  private String[] negotiatedTransports;
  private Boolean metaConnectDeliveryOnly;
  private boolean handshook;
  private boolean connected;
  private boolean broadcastToPublisher;
  private long interval = -1;
  private long timeout = -1;
  private long maxInterval = -1;
  private int lazyQueueSize;
  private int connectQueueSize;

  public OumuamuaServerSession (OumuamuaServer oumuamuaServer, OumuamuaTransport serverTransport, OumuamuaCarrier carrier, boolean createLocalSession, String idHint, int maximumMessageQueueSize, int maximumUndeliveredLazyMessageCount) {

    this.oumuamuaServer = oumuamuaServer;
    this.serverTransport = serverTransport;
    this.carrier = carrier;
    this.maximumMessageQueueSize = maximumMessageQueueSize;
    this.maximumUndeliveredLazyMessageCount = maximumUndeliveredLazyMessageCount;

    id = (idHint == null) ? SnowflakeId.newInstance().generateHexEncoding() : SnowflakeId.newInstance().generateHexEncoding() + "-" + idHint;
    localSession = (createLocalSession) ? new OumuamuaLocalSession(this) : null;
  }

  @Override
  public String getId () {

    return id;
  }

  public OumuamuaCarrier getCarrier () {

    return carrier;
  }

  public String[] getNegotiatedTransports () {

    return negotiatedTransports;
  }

  public void setNegotiatedTransports (String[] negotiatedTransports) {

    this.negotiatedTransports = negotiatedTransports;
  }

  @Override
  public boolean isHandshook () {

    return handshook;
  }

  public void setHandshook (boolean handshook) {

    this.handshook = handshook;
  }

  @Override
  public boolean isConnected () {

    return connected;
  }

  public void setConnected (boolean connected) {

    this.connected = connected;
  }

  @Override
  public String getUserAgent () {

    return carrier.getUserAgent();
  }

  @Override
  public long getTimeout () {

    return timeout;
  }

  @Override
  public void setTimeout (long timeout) {

    this.timeout = timeout;
  }

  @Override
  public long getInterval () {

    return interval;
  }

  @Override
  public void setInterval (long interval) {

    this.interval = interval;
  }

  @Override
  public long getMaxInterval () {

    return maxInterval;
  }

  @Override
  public void setMaxInterval (long maxInterval) {

    this.maxInterval = maxInterval;

    carrier.setMaxSessionIdleTimeout(maxInterval);
  }

  @Override
  public boolean isMetaConnectDeliveryOnly () {

    return metaConnectDeliveryOnly;
  }

  @Override
  public void setMetaConnectDeliveryOnly (boolean metaConnectDeliveryOnly) {

    this.metaConnectDeliveryOnly = metaConnectDeliveryOnly;
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
  public ServerTransport getServerTransport () {

    return serverTransport;
  }

  @Override
  public boolean isLocalSession () {

    return localSession != null;
  }

  @Override
  public LocalSession getLocalSession () {

    return localSession;
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

  public Iterator<Extension> iterateExtensions () {

    return extensionList.iterator();
  }

  @Override
  public List<Extension> getExtensions () {

    return new LinkedList<>(extensionList);
  }

  @Override
  public void addExtension (Extension extension) {

    extensionList.add(extension);
  }

  @Override
  public void removeExtension (Extension extension) {

    extensionList.remove(extension);
  }

  @Override
  public void addListener (ServerSessionListener serverSessionListener) {

    listenerList.add(serverSessionListener);
  }

  @Override
  public void removeListener (ServerSessionListener serverSessionListener) {

    listenerList.remove(serverSessionListener);
  }

  @Override
  public Set<ServerChannel> getSubscriptions () {

    return oumuamuaServer.getSubscriptions(this);
  }

  public void onConnected (MessageGenerator messageGenerator) {

    for (ServerSessionListener sessionListener : listenerList) {
      if (AddedListener.class.isAssignableFrom(sessionListener.getClass())) {
        ((AddedListener)sessionListener).added(this, messageGenerator.generate());
      }
    }
  }

  public void onDisconnected (MessageGenerator messageGenerator, boolean timeout) {

    for (ServerSessionListener sessionListener : listenerList) {
      if (RemovedListener.class.isAssignableFrom(sessionListener.getClass())) {
        ((RemovedListener)sessionListener).removed(this, messageGenerator.generate(), timeout);
      }
    }
  }

  public void onMessageSent (OumuamuaServerSession sender, MessageGenerator messageGenerator, Promise<Boolean> promise) {

    for (ServerSessionListener sessionListener : listenerList) {
      if (MessageListener.class.isAssignableFrom(sessionListener.getClass())) {
        ((MessageListener)sessionListener).onMessage(this, sender, messageGenerator.generate(), promise);
      }
    }
  }

  public void onMessageEnqueued (OumuamuaServerSession sender, MessageGenerator messageGenerator) {

    for (ServerSessionListener sessionListener : listenerList) {
      if (QueueListener.class.isAssignableFrom(sessionListener.getClass())) {
        ((QueueListener)sessionListener).queued(sender, messageGenerator.generate());
      }
    }
  }

  public void onMessageDeQueued (OumuamuaServerSession sender, MessageGenerator messageGenerator) {

    for (ServerSessionListener sessionListener : listenerList) {
      if (DeQueueListener.class.isAssignableFrom(sessionListener.getClass())) {
        ((DeQueueListener)sessionListener).deQueue();
      }
    }
  }

  @Override
  public void deliver (Session sender, ServerMessage.Mutable message, Promise<Boolean> promise) {

    try {
      carrier.send(MessageUtility.wrapDeliveryPacket(sender, message));
      promise.succeed(Boolean.TRUE);
    } catch (Exception exception) {
      promise.fail(exception);
    }
  }

  @Override
  public void deliver (Session sender, String channel, Object data, Promise<Boolean> promise) {

    try {
      carrier.send(MessageUtility.wrapDeliveryPacket(sender, channel, data));
      promise.succeed(Boolean.TRUE);
    } catch (Exception exception) {
      promise.fail(exception);
    }
  }

  public OumuamuaPacket[] poll () {

    messagePollLock.lock();

    try {

      Map.Entry<Long, LinkedList<OumuamuaLazyPacket>> lazyEntry;

      if ((lazyEntry = lazyMessageQueue.pollFirstEntry()) != null) {

        OumuamuaLazyPacket[] lazyPackets = new OumuamuaLazyPacket[lazyEntry.getValue().size()];
        int index = 0;

        for (OumuamuaLazyPacket lazyPacket : lazyEntry.getValue()) {
          lazyQueueSize -= lazyPacket.size();
          lazyPackets[index++] = lazyPacket;
        }

        return lazyEntry.getValue().toArray(lazyPackets);
      } else {

        OumuamuaPacket packet;

        if ((packet = messageQueue.poll()) == null) {

          return null;
        } else {
          connectQueueSize = packet.size();

          return new OumuamuaPacket[] {packet};
        }
      }
    } finally {
      messagePollLock.unlock();
    }
  }

  public OumuamuaLazyPacket[] pollLazy (long now) {

    messagePollLock.lock();

    try {

      Long firstKey;

      if ((!lazyMessageQueue.isEmpty()) && (firstKey = lazyMessageQueue.firstKey()) <= now) {

        LinkedList<OumuamuaLazyPacket> lazyPacketList;

        if ((lazyPacketList = lazyMessageQueue.remove(firstKey)) != null) {

          OumuamuaLazyPacket[] lazyPackets = new OumuamuaLazyPacket[lazyPacketList.size()];
          int index = 0;

          for (OumuamuaLazyPacket lazyPacket : lazyPacketList) {
            lazyQueueSize -= lazyPacket.size();
            lazyPackets[index++] = lazyPacket;
          }

          return lazyPackets;
        }
      }

      return null;
    } finally {
      messagePollLock.unlock();
    }
  }

  public void send (OumuamuaPacket packet) {

    if ((packet != null) && (packet.size() > 0)) {

      LinkedList<OumuamuaPacket> batchedPacketList;

      if ((batchedPacketList = BATCHED_PACKET_LIST_LOCAL.get()) != null) {
        batchedPacketList.add(packet);
      } else if (PacketType.LAZY.equals(packet.getType())) {
        messagePollLock.lock();

        try {

          LinkedList<OumuamuaLazyPacket> enqueuingLazyPacketList;
          long lazyTimestamp;

          if ((lazyQueueSize += packet.size()) > maximumUndeliveredLazyMessageCount) {

            boolean operating = true;
            boolean lostLazyMessages = false;

            while (operating && (lazyQueueSize > maximumUndeliveredLazyMessageCount) && (!lazyMessageQueue.isEmpty())) {

              LinkedList<OumuamuaLazyPacket> overflowLazyPacketList;

              if ((overflowLazyPacketList = lazyMessageQueue.pollFirstEntry().getValue()) != null) {
                if (!overflowLazyPacketList.isEmpty()) {

                  OumuamuaLazyPacket[] overflowLazyPackets = new OumuamuaLazyPacket[overflowLazyPacketList.size()];
                  int index = 0;

                  for (OumuamuaLazyPacket overflowLazyPacket : overflowLazyPacketList) {
                    lazyQueueSize -= overflowLazyPacket.size();
                    overflowLazyPackets[index++] = overflowLazyPacket;
                  }

                  if ((metaConnectDeliveryOnly == null) ? serverTransport.isMetaConnectDeliveryOnly() : metaConnectDeliveryOnly) {
                    lostLazyMessages = true;
                  } else {
                    try {
                      carrier.send(overflowLazyPackets);
                    } catch (Exception exception) {
                      LoggerManager.getLogger(OumuamuaServerSession.class).error(exception);
                    }
                  }
                }
              } else {
                operating = false;
              }
            }

            if (lostLazyMessages) {
              LoggerManager.getLogger(OumuamuaServerSession.class).warn("Lazy messages lost due to overflow");
            }
          }

          if ((enqueuingLazyPacketList = lazyMessageQueue.get(lazyTimestamp = ((OumuamuaLazyPacket)packet).getLazyTimestamp())) == null) {
            lazyMessageQueue.put(lazyTimestamp, enqueuingLazyPacketList = new LinkedList<>());
          }

          enqueuingLazyPacketList.add((OumuamuaLazyPacket)packet);
        } finally {
          messagePollLock.unlock();
        }
      } else if ((metaConnectDeliveryOnly == null) ? serverTransport.isMetaConnectDeliveryOnly() : metaConnectDeliveryOnly) {
        messagePollLock.lock();

        try {
          if (connectQueueSize + packet.size() > maximumMessageQueueSize) {
            LoggerManager.getLogger(OumuamuaServerSession.class).warn("Queued messages lost due to overflow");
          } else {
            connectQueueSize += packet.size();
            messageQueue.add(packet);

            for (MapLike mapLike : packet.getMessages()) {
              onMessageEnqueued(packet.getSender(), new MapMessageGenerator(carrier.getContext(), serverTransport, packet.getChannelId(), mapLike, false));
            }
          }
        } finally {
          messagePollLock.unlock();
        }
      } else {
        try {
          carrier.send(packet);
        } catch (Exception exception) {
          LoggerManager.getLogger(OumuamuaServerSession.class).error(exception);
        }
      }
    }
  }

  @Override
  public void disconnect () {

    try {
      carrier.close();
    } catch (IOException ioException) {
      LoggerManager.getLogger(OumuamuaServerSession.class).error(ioException);
    }
  }

  @Override
  public void batch (Runnable batch) {

    new Thread(() -> {
      startBatch();

      try {
        batch.run();
      } finally {
        endBatch();
      }
    }).start();
  }

  @Override
  public synchronized void startBatch () {

    if (BATCHED_PACKET_LIST_LOCAL.get() == null) {
      BATCHED_PACKET_LIST_LOCAL.set(new LinkedList<>());
    }
  }

  @Override
  public synchronized boolean endBatch () {

    LinkedList<OumuamuaPacket> batchedPacketList = BATCHED_PACKET_LIST_LOCAL.get();

    if (batchedPacketList != null) {
      BATCHED_PACKET_LIST_LOCAL.remove();

      if (!batchedPacketList.isEmpty()) {
        for (OumuamuaPacket batchedPacket : batchedPacketList) {
          send(batchedPacket);
        }

        return true;
      }
    }

    return false;
  }
}
