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
package org.smallmind.bayeux.oumuamua.server.spi.json;

import java.io.IOException;
import java.util.LinkedList;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.PacketWriter;

public class PacketUtility {

  public static <V extends Value<V>> Packet<V> merge (Packet<V> basePacket, Packet<V> otherPacket, Route filteredRoute) {

    LinkedList<Message<V>> otherPacketMessageList = new LinkedList<>();

    for (Message<V> otherPacketMessage : otherPacket.getMessages()) {
      if ((filteredRoute == null) || (!filteredRoute.getPath().equals(otherPacketMessage.getChannel()))) {
        otherPacketMessageList.add(otherPacketMessage);
      }
    }

    if (otherPacketMessageList.isEmpty()) {

      return basePacket;
    } else {

      Message<V>[] mergedMessages = new Message[basePacket.getMessages().length + otherPacketMessageList.size()];

      System.arraycopy(basePacket.getMessages(), 0, mergedMessages, 0, basePacket.getMessages().length);
      System.arraycopy(otherPacketMessageList.toArray(new Message[0]), 0, mergedMessages, basePacket.getMessages().length, otherPacketMessageList.size());

      return new Packet<>(basePacket.getPacketType(), basePacket.getSenderId(), basePacket.getRoute(), mergedMessages);
    }
  }

  public static <V extends Value<V>> Packet<V> freezePacket (Packet<V> packet) {

    Message<V>[] frozenMessages = new Message[packet.getMessages().length];
    int index = 0;

    for (Message<V> message : packet.getMessages()) {
      frozenMessages[index++] = new MessageDouble<V>(message);
    }

    return new Packet<V>(packet.getPacketType(), packet.getSenderId(), packet.getRoute(), frozenMessages);
  }

  public static <V extends Value<V>> String encode (Packet<V> packet)
    throws IOException {

    StringBuilder builder = new StringBuilder();

    try (PacketWriter writer = new PacketWriter(builder)) {

      Message<V>[] messages;
      boolean first = true;

      writer.write('[');

      if ((messages = packet.getMessages()) != null) {
        for (Message<V> message : messages) {
          if (!first) {
            writer.write(',');
          }
          message.encode(writer);
          first = false;
        }
      }

      writer.write(']');
    }

    return builder.toString();
  }
}
