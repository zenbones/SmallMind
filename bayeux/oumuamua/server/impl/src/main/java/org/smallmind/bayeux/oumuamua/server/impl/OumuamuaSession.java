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

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.bayeux.oumuamua.server.spi.Connection;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;
import org.smallmind.nutsnbolts.util.SnowflakeId;

public class OumuamuaSession<V extends Value<V>> extends AbstractAttributed implements Session<V> {

  private final ConcurrentLinkedDeque<Packet<V>> longPollQueue = new ConcurrentLinkedDeque<>();
  private final ConcurrentLinkedQueue<Session.Listener<V>> listenerList = new ConcurrentLinkedQueue<>();
  private final AtomicInteger longPollQueueSize = new AtomicInteger(0);
  private final Connection<V> connection;
  private final String sessionId = SnowflakeId.newInstance().generateHexEncoding();
  private final boolean longPolling;
  private final int maxLongPollQueueSize;
  private SessionState state;

  public OumuamuaSession (Connection<V> connection, int maxLongPollQueueSize) {

    this.connection = connection;
    this.maxLongPollQueueSize = maxLongPollQueueSize;

    longPolling = connection.getTransport().getProtocol().isLongPolling();
    state = SessionState.INITIALIZED;
  }

  private void onDelivery (Packet<V> packet) {

    if (PacketType.RESPONSE.equals(packet.getPacketType()) || PacketType.DELIVERY.equals(packet.getPacketType())) {
      for (Session.Listener<V> listener : listenerList) {
        if (Session.PacketListener.class.isAssignableFrom(listener.getClass())) {
          if (PacketType.DELIVERY.equals(packet.getPacketType())) {
            ((Session.PacketListener<V>)listener).onDelivery(packet);
          } else {
            ((Session.PacketListener<V>)listener).onResponse(packet);
          }
        }
      }
    }
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

  @Override
  public boolean isLongPolling () {

    return longPolling;
  }

  @Override
  public synchronized SessionState getState () {

    return state;
  }

  @Override
  public void completeHandshake () {

    state = SessionState.HANDSHOOK;
  }

  @Override
  public void completeConnection () {

    state = SessionState.CONNECTED;
  }

  @Override
  public void completeClose () {

    state = SessionState.CLOSED;
  }

  @Override
  public Packet<V> poll () {

    Packet<V> enqueuedPacket;

    if ((enqueuedPacket = longPollQueue.pollFirst()) == null) {

      return null;
    } else {

      Packet<V> frozenPacket;

      longPollQueueSize.decrementAndGet();
      frozenPacket = PacketUtility.freezePacket(enqueuedPacket);

      onDelivery(frozenPacket);

      return frozenPacket;
    }
  }

  @Override
  public void deliver (Packet<V> packet) {

    if (longPolling) {
      if (longPollQueueSize.incrementAndGet() > maxLongPollQueueSize) {
        if (longPollQueue.pollLast() != null) {
          longPollQueueSize.decrementAndGet();
        }
      }

      longPollQueue.add(packet);
    } else {

      Packet<V> frozenPacket = PacketUtility.freezePacket(packet);

      onDelivery(frozenPacket);

      connection.deliver(frozenPacket);
    }
  }
}
