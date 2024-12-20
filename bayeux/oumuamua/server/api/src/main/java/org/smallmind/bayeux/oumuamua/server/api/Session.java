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
package org.smallmind.bayeux.oumuamua.server.api;

import java.util.concurrent.TimeUnit;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

public interface Session<V extends Value<V>> extends Attributed {

  interface Listener<V extends Value<V>> {

  }

  // Messages are frozen when delivered from the channel to the session, guaranteeing changes generated here are seen only in the sending session
  interface PacketListener<V extends Value<V>> extends Listener<V> {

    // For responses from META commands delivered to the sender
    Packet<V> onResponse (Session<V> sender, Packet<V> packet);

    // For published messages delivered to receivers
    Packet<V> onDelivery (Session<V> sender, Packet<V> packet);
  }

  void addListener (Listener<V> listener);

  void removeListener (Listener<V> listener);

  String getId ();

  boolean isLocal ();

  boolean isLongPolling ();

  void setLongPolling (boolean longPolling);

  int getMaxLongPollQueueSize ();

  SessionState getState ();

  void completeHandshake ();

  void completeConnection ();

  void completeDisconnect ();

  Packet<V> onResponse (Session<V> sender, Packet<V> packet);

  void dispatch (Packet<V> packet);

  Packet<V> poll (long timeout, TimeUnit unit)
    throws InterruptedException;

  void deliver (Channel<V> fromChannel, Session<V> sender, Packet<V> packet);
}
