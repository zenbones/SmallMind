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
package org.smallmind.bayeux.oumuamua.server.api;

import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Immutable carrier for one or more Bayeux messages addressed to a single route, classified
 * by its role in the request/response/delivery lifecycle.
 *
 * @param <V> concrete {@link Value} implementation used to represent message payloads
 */
public class Packet<V extends Value<V>> {

  private final Message<V>[] messages;
  private final PacketType packetType;
  private final Route route;
  private final String senderId;

  /**
   * Creates a single-message packet.
   *
   * @param packetType role this packet plays in the Bayeux exchange
   * @param senderId   session id of the originator, or {@code null} for server-originated packets
   * @param route      channel route the packet is addressed to
   * @param message    single message carried by this packet
   */
  public Packet (PacketType packetType, String senderId, Route route, Message<V> message) {

    this(packetType, senderId, route, new Message[] {message});
  }

  /**
   * Creates a multi-message packet.
   *
   * @param packetType role this packet plays in the Bayeux exchange
   * @param senderId   session id of the originator, or {@code null} for server-originated packets
   * @param route      channel route the packet is addressed to
   * @param message    messages carried by this packet
   */
  public Packet (PacketType packetType, String senderId, Route route, Message<V>[] message) {

    this.packetType = packetType;
    this.senderId = senderId;
    this.route = route;
    this.messages = message;
  }

  /**
   * Returns the role this packet plays in the Bayeux exchange.
   *
   * @return {@link PacketType#REQUEST}, {@link PacketType#RESPONSE}, or {@link PacketType#DELIVERY}
   */
  public PacketType getPacketType () {

    return packetType;
  }

  /**
   * Returns the session id of the originating client, or {@code null} for server-originated packets.
   *
   * @return sender session id, possibly {@code null}
   */
  public String getSenderId () {

    return senderId;
  }

  /**
   * Returns the route this packet is addressed to.
   *
   * @return target channel route
   */
  public Route getRoute () {

    return route;
  }

  /**
   * Returns all messages carried by this packet.
   *
   * @return array of one or more messages
   */
  public Message<V>[] getMessages () {

    return messages;
  }
}
