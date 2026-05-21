/*
 * Copyright (c) 2007 through 2026 David Berkman
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

/**
 * Static helpers for composing, freezing, and JSON-encoding {@link Packet} instances.
 */
public class PacketUtility {

  /**
   * Merges the messages from {@code otherPacket} into {@code basePacket}, optionally excluding messages
   * whose channel matches {@code filteredRoute} and optionally inserting the merged messages immediately
   * after the first base message rather than appending them at the end.
   *
   * @param basePacket    packet whose metadata (type, sender, route) governs the result
   * @param otherPacket   packet supplying additional messages to merge in
   * @param filteredRoute channel path to exclude from {@code otherPacket}, or {@code null} to include all messages
   * @param prepend       when {@code true}, inserts other messages after position 0 of base; when {@code false}, appends them
   * @param <V>           value type
   * @return a new merged packet, or {@code basePacket} unchanged when all other messages were filtered out
   */
  public static <V extends Value<V>> Packet<V> merge (Packet<V> basePacket, Packet<V> otherPacket, Route filteredRoute, boolean prepend) {

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

      if (prepend) {

        int prologLength = Math.min(basePacket.getMessages().length, 1);

        System.arraycopy(basePacket.getMessages(), 0, mergedMessages, 0, prologLength);
        System.arraycopy(otherPacketMessageList.toArray(new Message[0]), 0, mergedMessages, prologLength, otherPacketMessageList.size());
        if (basePacket.getMessages().length > 1) {
          System.arraycopy(basePacket.getMessages(), 1, mergedMessages, otherPacketMessageList.size() + 1, basePacket.getMessages().length - 1);
        }
      } else {
        System.arraycopy(basePacket.getMessages(), 0, mergedMessages, 0, basePacket.getMessages().length);
        System.arraycopy(otherPacketMessageList.toArray(new Message[0]), 0, mergedMessages, basePacket.getMessages().length, otherPacketMessageList.size());
      }

      return new Packet<>(basePacket.getPacketType(), basePacket.getSenderId(), basePacket.getRoute(), mergedMessages);
    }
  }

  /**
   * Produces a new packet whose messages are each wrapped in a {@link MessageDouble}, preventing
   * downstream consumers from mutating the originals while still allowing overlay reads.
   *
   * @param packet the packet whose messages should be frozen
   * @param <V>    value type
   * @return new packet with the same metadata as {@code packet} but with immutability-shielded messages
   */
  public static <V extends Value<V>> Packet<V> freezePacket (Packet<V> packet) {

    Message<V>[] frozenMessages = new Message[packet.getMessages().length];
    int index = 0;

    for (Message<V> message : packet.getMessages()) {
      frozenMessages[index++] = new MessageDouble<V>(message);
    }

    return new Packet<V>(packet.getPacketType(), packet.getSenderId(), packet.getRoute(), frozenMessages);
  }

  /**
   * Serializes all messages in {@code packet} to a JSON array string.
   *
   * @param packet the packet whose messages are to be serialized
   * @param <V>    value type
   * @return JSON array string containing every message in the packet
   * @throws IOException if encoding any message to the output fails
   */
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
