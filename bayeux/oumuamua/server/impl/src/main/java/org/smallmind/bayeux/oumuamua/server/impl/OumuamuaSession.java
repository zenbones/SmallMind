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

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.nutsnbolts.util.Pair;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

public class OumuamuaSession<V extends Value<V>> extends AbstractAttributed implements Session<V> {

  private final ReentrantLock longPollLock = new ReentrantLock();
  private final Condition notEmptyCondition = longPollLock.newCondition();
  private final ConcurrentLinkedDeque<Pair<Session<V>, Packet<V>>> longPollDeque = new ConcurrentLinkedDeque<>();
  private final ConcurrentLinkedQueue<Session.Listener<V>> listenerList = new ConcurrentLinkedQueue<>();
  private final AtomicReference<Connection<V>> connectionRef = new AtomicReference<>();
  private final AtomicInteger longPollQueueSize = new AtomicInteger(0);
  private final Consumer<Session<V>> onConnectedCallback;
  private final Consumer<Session<V>> onDisconnectedCallback;
  private final AtomicBoolean longPolling = new AtomicBoolean(false);
  private final String sessionId = SnowflakeId.newInstance().generateHexEncoding();
  private final long maxIdleTimeoutMilliseconds;
  private final int maxLongPollQueueSize;
  private SessionState state;
  private long lastContactTimestamp;

  public OumuamuaSession (Consumer<Session<V>> onConnectedCallback, Consumer<Session<V>> onDisconnectedCallback, Connection<V> connection, int maxLongPollQueueSize, long maxIdleTimeoutMilliseconds) {

    this.onConnectedCallback = onConnectedCallback;
    this.onDisconnectedCallback = onDisconnectedCallback;
    this.maxLongPollQueueSize = maxLongPollQueueSize;
    this.maxIdleTimeoutMilliseconds = maxIdleTimeoutMilliseconds;

    if (connection.getTransport().getProtocol().isLongPolling()) {
      longPolling.set(true);
    }

    connectionRef.set(connection);
    state = SessionState.INITIALIZED;
    lastContactTimestamp = System.currentTimeMillis();
  }

  private Packet<V> onProcessing (Session<V> sender, Packet<V> packet) {

    if (PacketType.RESPONSE.equals(packet.getPacketType()) || PacketType.DELIVERY.equals(packet.getPacketType())) {
      for (Session.Listener<V> listener : listenerList) {
        if (Session.PacketListener.class.isAssignableFrom(listener.getClass())) {
          if (PacketType.DELIVERY.equals(packet.getPacketType())) {
            if ((packet = ((Session.PacketListener<V>)listener).onDelivery(sender, packet)) == null) {
              break;
            }
          } else if ((packet = ((Session.PacketListener<V>)listener).onResponse(sender, packet)) == null) {
            break;
          }
        }
      }
    }

    return packet;
  }

  @Override
  public void addListener (Listener<V> listener) {

    listenerList.add(listener);
  }

  @Override
  public void removeListener (Listener<V> listener) {

    listenerList.remove(listener);
  }

  @Override
  public String getId () {

    return sessionId;
  }

  @Override
  public int getMaxLongPollQueueSize () {

    return maxLongPollQueueSize;
  }

  public void hijack (Connection<V> connection) {

    connectionRef.set(connection);
  }

  public void onCleanUp () {

    Connection<V> connection;

    if ((connection = connectionRef.get()) != null) {
      connection.onCleanUp();
    }
  }

  @Override
  public boolean isLocal () {

    return connectionRef.get().getTransport().isLocal();
  }

  @Override
  public boolean isLongPolling () {

    return longPolling.get();
  }

  @Override
  public void setLongPolling (boolean longPolling) {

    this.longPolling.set(longPolling);
  }

  @Override
  public synchronized SessionState getState () {

    return state;
  }

  @Override
  public synchronized void completeHandshake () {

    state = SessionState.HANDSHOOK;
  }

  @Override
  public synchronized void completeConnection () {

    state = SessionState.CONNECTED;
    onConnectedCallback.accept(this);
  }

  @Override
  public synchronized void completeDisconnect () {

    state = SessionState.DISCONNECTED;
    onDisconnectedCallback.accept(this);
  }

  public Transport<V> getTransport () {

    return connectionRef.get().getTransport();
  }

  public synchronized void contact () {

    if (!SessionState.DISCONNECTED.equals(state)) {
      lastContactTimestamp = System.currentTimeMillis();
    }
  }

  public synchronized boolean isRemovable (long now) {

    return (now - lastContactTimestamp) >= maxIdleTimeoutMilliseconds;
  }

  @Override
  public Packet<V> onResponse (Session<V> sender, Packet<V> packet) {

    return onProcessing(sender, packet);
  }

  @Override
  public void dispatch (Packet<V> packet) {

    connectionRef.get().deliver(packet);
  }

  @Override
  public Packet<V> poll (long timeout, TimeUnit unit)
    throws InterruptedException {

    long remainingNanoseconds = unit.toNanos(timeout);

    longPollLock.lock();

    try {
      Pair<Session<V>, Packet<V>> enqueuedPair;

      do {
        if ((enqueuedPair = longPollDeque.pollFirst()) == null) {
          if (remainingNanoseconds > 0) {
            remainingNanoseconds = notEmptyCondition.awaitNanos(remainingNanoseconds);
          }
        } else {

          Packet<V> frozenPacket;

          longPollQueueSize.decrementAndGet();
          frozenPacket = PacketUtility.freezePacket(enqueuedPair.getSecond());

          return onProcessing(enqueuedPair.getFirst(), frozenPacket);
        }
      } while (remainingNanoseconds > 0);

      return null;
    } finally {
      longPollLock.unlock();
    }
  }

  @Override
  public void deliver (Channel<V> fromChannel, Session<V> sender, Packet<V> packet) {

    if (fromChannel.isStreaming() && (!connectionRef.get().getTransport().getProtocol().isLongPolling())) {

      Packet<V> frozenPacket = PacketUtility.freezePacket(packet);

      if ((frozenPacket = onProcessing(sender, frozenPacket)) != null) {
        connectionRef.get().deliver(frozenPacket);
      }
    } else if (longPolling.get()) {
      longPollLock.lock();

      try {
        if (longPollQueueSize.incrementAndGet() > maxLongPollQueueSize) {
          LoggerManager.getLogger(OumuamuaSession.class).debug("Session(%s) overflowed the long poll queue", getId());

          if (longPollDeque.pollFirst() != null) {
            longPollQueueSize.decrementAndGet();
          }
        }

        longPollDeque.add(new Pair<>(sender, packet));
        notEmptyCondition.signal();
      } finally {
        longPollLock.unlock();
      }
    } else {

      Packet<V> frozenPacket = PacketUtility.freezePacket(packet);

      if ((frozenPacket = onProcessing(sender, frozenPacket)) != null) {
        connectionRef.get().deliver(frozenPacket);
      }
    }
  }
}
