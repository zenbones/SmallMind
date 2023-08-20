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

import java.util.concurrent.ConcurrentLinkedQueue;
import org.smallmind.bayeux.oumuamua.common.api.Codec;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;
import org.smallmind.nutsnbolts.util.SnowflakeId;

public class OumuamuaSession<V extends Value<V>> extends AbstractAttributed implements Session<V> {

  private final ConcurrentLinkedQueue<Session.Listener<V>> listenerList = new ConcurrentLinkedQueue<>();
  private final Codec<V> codec;
  private final String sessionId = SnowflakeId.newInstance().generateHexEncoding();
  private boolean handshook;
  private boolean connected;

  public OumuamuaSession (Codec<V> codec) {

    this.codec = codec;
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
  public synchronized boolean isHandshook () {

    return handshook;
  }

  public synchronized void setHandshook (boolean handshook) {

    this.handshook = handshook;
  }

  @Override
  public synchronized boolean isConnected () {

    return connected;
  }

  public synchronized void setConnected (boolean connected) {

    this.connected = connected;
  }

  @Override
  public void deliver (Packet<V> packet) {

    Packet<V> frozenPacket = PacketUtility.freezePacket(packet);
  }
}
