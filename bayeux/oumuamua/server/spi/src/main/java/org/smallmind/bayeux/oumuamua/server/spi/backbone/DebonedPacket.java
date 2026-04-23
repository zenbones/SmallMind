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
package org.smallmind.bayeux.oumuamua.server.spi.backbone;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Carrier that pairs a deserialized {@link Packet} with the name of the cluster node that
 * originally published it, allowing recipients to skip local re-delivery.
 *
 * @param <V> concrete {@link Value} type carried in Bayeux messages
 */
public class DebonedPacket<V extends Value<V>> {

  private final Packet<V> packet;
  private final String nodeName;

  /**
   * Combines a source node name with its associated packet.
   *
   * @param nodeName unique identifier of the cluster node that serialized the packet
   * @param packet   the deserialized packet received from the backbone
   */
  public DebonedPacket (String nodeName, Packet<V> packet) {

    this.nodeName = nodeName;
    this.packet = packet;
  }

  /**
   * Returns the packet received from the backbone.
   *
   * @return deserialized packet ready for local delivery
   */
  public Packet<V> getPacket () {

    return packet;
  }

  /**
   * Returns the identifier of the cluster node that originally published this packet.
   *
   * @return source node name; used to suppress self-delivery of echoed messages
   */
  public String getNodeName () {

    return nodeName;
  }
}
