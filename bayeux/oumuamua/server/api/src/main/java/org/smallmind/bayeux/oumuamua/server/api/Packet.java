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
 * Represents a bundle of Bayeux messages grouped for delivery on a route.
 *
 * @param <V> concrete {@link Value} implementation used to represent message payloads
 */
public class Packet<V extends Value<V>> {

  private final Message<V>[] messages;
  private final PacketType packetType;
  private final Route route;
  private final String senderId;

  /**
   * Creates a packet containing a single message.
   *
   * @param packetType the type of packet being sent
   * @param senderId   the session identifier of the sender
   * @param route      the target route
   * @param message    the message to include
   */
  public Packet (PacketType packetType, String senderId, Route route, Message<V> message) {

    this(packetType, senderId, route, new Message[] {message});
  }

  /**
   * Creates a packet with multiple messages.
   *
   * @param packetType the type of packet being sent
   * @param senderId   the session identifier of the sender
   * @param route      the target route
   * @param message    the messages to include
   */
  public Packet (PacketType packetType, String senderId, Route route, Message<V>[] message) {

    this.packetType = packetType;
    this.senderId = senderId;
    this.route = route;
    this.messages = message;
  }

  /**
   * @return the packet classification
   */
  public PacketType getPacketType () {

    return packetType;
  }

  /**
   * @return the identifier of the sender session, or {@code null} if not associated
   */
  public String getSenderId () {

    return senderId;
  }

  /**
   * @return the route the packet targets
   */
  public Route getRoute () {

    return route;
  }

  /**
   * @return messages contained in this packet
   */
  public Message<V>[] getMessages () {

    return messages;
  }
}
