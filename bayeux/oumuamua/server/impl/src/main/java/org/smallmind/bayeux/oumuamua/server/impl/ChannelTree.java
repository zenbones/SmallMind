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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.bayeux.oumuamua.common.api.json.Codec;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.Segment;
import org.smallmind.bayeux.oumuamua.server.spi.StringSegment;

public class ChannelTree<V extends Value<V>> {

  private final ReentrantLock treeExpansionLock = new ReentrantLock();
  private final ReentrantReadWriteLock channelChangeLock = new ReentrantReadWriteLock();
  private final ConcurrentHashMap<Segment, ChannelTree<V>> childMap = new ConcurrentHashMap<>();
  private final ChannelTree<V> parent;
  private Channel<V> channel;

  public ChannelTree () {

    this(null, null);
  }

  public ChannelTree (ChannelTree<V> parent, Channel<V> channel) {

    this.parent = parent;
    this.channel = channel;
  }

  public Channel<V> getChannel () {

    channelChangeLock.readLock().lock();

    try {

      return channel;
    } finally {
      channelChangeLock.readLock().unlock();
    }
  }

  public ChannelTree<V> find (int index, DefaultRoute route) {

    ChannelTree<V> child;

    if ((child = childMap.get(route.getSegment(index))) == null) {

      return null;
    } else {

      return (index == route.lastIndex()) ? child : child.find(index + 1, route);
    }
  }

  public ChannelTree<V> createIfAbsent (Codec<V> codec, long timeToLive, int index, DefaultRoute route) {

    ChannelTree<V> child;
    Segment segment;

    if ((child = childMap.get(segment = route.getSegment(index))) == null) {

      treeExpansionLock.lock();

      try {
        if ((child = childMap.get(segment)) == null) {
          childMap.put(segment, child = new ChannelTree<V>(this, (index == route.lastIndex()) ? new OumuamuaChannel<V>(codec, timeToLive, route) : null));
        }
      } finally {
        treeExpansionLock.unlock();
      }
    }

    return (index == route.lastIndex()) ? child.enforceChannel(codec, timeToLive, route) : child.createIfAbsent(codec, timeToLive, index + 1, route);
  }

  private ChannelTree<V> enforceChannel (Codec<V> codec, long timeToLive, DefaultRoute route) {

    channelChangeLock.writeLock().lock();

    try {
      if (channel == null) {
        channel = new OumuamuaChannel<V>(codec, timeToLive, route);
      }

      return this;
    } finally {
      channelChangeLock.writeLock().unlock();
    }
  }

  public ChannelTree<V> removeIfPresent (int index, DefaultRoute route) {

    ChannelTree<V> child;

    if ((child = childMap.get(route.getSegment(index))) == null) {

      return null;
    } else {

      return (index == route.lastIndex()) ? child.removeChannel() : child.removeIfPresent(index + 1, route);
    }
  }

  private ChannelTree<V> removeChannel () {

    channelChangeLock.writeLock().lock();

    try {
      channel = null;

      return this;
    } finally {
      channelChangeLock.writeLock().unlock();
    }
  }

  public void deliver (int index, Packet<V> packet, Set<String> sessionIdSet) {

    if (index < packet.getRoute().lastIndex()) {

      ChannelTree<V> deepWildBranch;
      ChannelTree<V> nextBranch;

      if ((deepWildBranch = childMap.get(StringSegment.wild())) != null) {

        deepWildBranch.deliverToChannel(packet, sessionIdSet);
      }
      if ((nextBranch = childMap.get(((DefaultRoute)packet.getRoute()).getSegment(index))) != null) {
        nextBranch.deliver(index + 1, packet, sessionIdSet);
      }
    } else {
      if (parent != null) {

        ChannelTree<V> wildBranch;

        if ((wildBranch = parent.childMap.get(StringSegment.deepWild())) != null) {
          wildBranch.deliverToChannel(packet, sessionIdSet);
        }
      }

      deliverToChannel(packet, sessionIdSet);
    }
  }

  private void deliverToChannel (Packet<V> packet, Set<String> sessionIdSet) {

    channelChangeLock.readLock().lock();

    try {
      if (channel != null) {
        channel.deliver(packet, sessionIdSet);
      }
    } finally {
      channelChangeLock.readLock().unlock();
    }
  }

  public void clean () {

    clean(null);
  }

  private void clean (Segment segment) {

    // Using getServerChannel() to enforce synchronization boundary
    if ((segment != null) && (parent != null) && (getChannel() == null) && childMap.isEmpty()) {
      parent.childMap.remove(segment);
    } else {
      for (Map.Entry<Segment, ChannelTree<V>> childEntry : childMap.entrySet()) {
        childEntry.getValue().clean(childEntry.getKey());
      }
    }
  }

  public void walk (ChannelOperation operation) {

    operation.operate(this);

    for (ChannelTree<V> child : childMap.values()) {
      child.walk(operation);
    }
  }
}
