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
package org.smallmind.bayeux.oumuamua.server.api;

import java.util.Set;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

public interface Channel<V extends Value<V>> extends Attributed {

  String WILD = "*";
  String DEEP_WILD = "**";

  interface Listener<V extends Value<V>> {

    boolean isPersistent ();
  }

  interface SessionListener<V extends Value<V>> extends Listener<V> {

    void onSubscribed (Session<V> session);

    void onUnsubscribed (Session<V> session);
  }

  interface PacketListener<V extends Value<V>> extends Listener<V> {

    boolean isPersistent ();

    void onDelivery (Session<V> sender, Packet<V> packet);
  }

  void addListener (Listener<V> listener);

  void removeListener (Listener<V> listener);

  Route getRoute ();

  default boolean isWild () {

    return getRoute().isWild();
  }

  default boolean isDeepWild () {

    return getRoute().isDeepWild();
  }

  default boolean isMeta () {

    return getRoute().isMeta();
  }

  default boolean isService () {

    return getRoute().isService();
  }

  default boolean isDeliverable () {

    return getRoute().isDeliverable();
  }

  boolean isPersistent ();

  void setPersistent (boolean persistent);

  boolean isReflecting ();

  void setReflecting (boolean reflecting);

  boolean subscribe (Session<V> session);

  void unsubscribe (Session<V> session);

  boolean isRemovable (long now);

  void deliver (Session<V> sender, Packet<V> packet, Set<String> sessionIdSet);

  void publish (ObjectValue<V> data);
}
