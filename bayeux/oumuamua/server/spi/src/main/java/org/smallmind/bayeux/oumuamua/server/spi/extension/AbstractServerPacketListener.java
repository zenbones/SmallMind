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
package org.smallmind.bayeux.oumuamua.server.spi.extension;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * No-op server packet listener that simply forwards packets unchanged.
 *
 * @param <V> concrete value type used in messages
 */
public abstract class AbstractServerPacketListener<V extends Value<V>> implements Server.PacketListener<V> {

  /**
   * Returns the request packet unchanged.
   *
   * @param sender originating session
   * @param packet request packet
   * @return the same packet instance
   */
  @Override
  public Packet<V> onRequest (Session<V> sender, Packet<V> packet) {

    return packet;
  }

  /**
   * Returns the response packet unchanged.
   *
   * @param sender originating session
   * @param packet response packet
   * @return the same packet instance
   */
  @Override
  public Packet<V> onResponse (Session<V> sender, Packet<V> packet) {

    return packet;
  }

  /**
   * Returns the delivery packet unchanged.
   *
   * @param sender originating session
   * @param packet delivery packet
   * @return the same packet instance
   */
  @Override
  public Packet<V> onDelivery (Session<V> sender, Packet<V> packet) {

    return packet;
  }
}
