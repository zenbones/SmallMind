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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.ChannelStateException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.Segment;
import org.smallmind.bayeux.oumuamua.server.spi.StringSegment;

public class ChannelBranch<V extends Value<V>> {

  private final ReentrantReadWriteLock channelChangeLock = new ReentrantReadWriteLock();
  private final ConcurrentHashMap<Segment, ChannelBranch<V>> childMap = new ConcurrentHashMap<>();
  private final ChannelBranch<V> parent;
  private Channel<V> channel;

  public ChannelBranch (ChannelBranch<V> parent) {

    this.parent = parent;
  }

  public Channel<V> getChannel () {

    channelChangeLock.readLock().lock();

    try {

      return channel;
    } finally {
      channelChangeLock.readLock().unlock();
    }
  }

  public Channel<V> find (int index, DefaultRoute route) {

    ChannelBranch<V> child;

    if ((child = childMap.get(route.getSegment(index))) == null) {

      return null;
    } else {

      return (index == route.lastIndex()) ? child.getChannel() : child.find(index + 1, route);
    }
  }

  protected Channel<V> addChannelAsNecessary (long timeToLive, int index, DefaultRoute route, Consumer<Channel<V>> channelCallback, ChannelInitializer... initializers) {

    ChannelBranch<V> child;
    Segment segment;

    if ((child = childMap.get(segment = route.getSegment(index))) == null) {
      childMap.put(segment, child = new ChannelBranch<V>(this));
    }

    return (index == route.lastIndex()) ? child.initializeChannel(timeToLive, route, channelCallback, initializers) : child.addChannelAsNecessary(timeToLive, index + 1, route, channelCallback, initializers);
  }

  private Channel<V> initializeChannel (long timeToLive, DefaultRoute route, Consumer<Channel<V>> channelCallback, ChannelInitializer... initializers) {

    channelChangeLock.writeLock().lock();

    try {
      if (channel == null) {
        channel = new OumuamuaChannel<>(timeToLive, route);

        if (initializers != null) {
          for (ChannelInitializer initializer : initializers) {
            initializer.accept(channel);
          }
        }

        channelCallback.accept(channel);
      }

      return channel;
    } finally {
      channelChangeLock.writeLock().unlock();
    }
  }

  public ChannelBranch<V> removeChannelIfPresent (int index, DefaultRoute route, Consumer<Channel<V>> channelCallback)
    throws ChannelStateException {

    ChannelBranch<V> child;

    if ((child = childMap.get(route.getSegment(index))) == null) {

      return null;
    } else {

      return (index == route.lastIndex()) ? child.removeChannel(channelCallback) : child.removeChannelIfPresent(index + 1, route, channelCallback);
    }
  }

  public ChannelBranch<V> removeChannel (Consumer<Channel<V>> channelCallback)
    throws ChannelStateException {

    channelChangeLock.writeLock().lock();

    try {
      if (channel != null) {
        if (channel.isPersistent()) {
          throw new ChannelStateException("Attempt to remove persistent channel(%s)", ((OumuamuaChannel<V>)channel).getRoute().getPath());
        } else {
          channelCallback.accept(((OumuamuaChannel<V>)channel).terminate());

          channel = null;
        }
      }

      return this;
    } finally {
      channelChangeLock.writeLock().unlock();
    }
  }

  public void deliver (int index, Packet<V> packet, Set<String> sessionIdSet) {

    if (index < packet.getRoute().size()) {

      ChannelBranch<V> deepWildBranch;
      ChannelBranch<V> nextBranch;

      if ((deepWildBranch = childMap.get(StringSegment.deepWild())) != null) {

        deepWildBranch.deliverToChannel(packet, sessionIdSet);
      }
      if ((nextBranch = childMap.get(((DefaultRoute)packet.getRoute()).getSegment(index))) != null) {
        nextBranch.deliver(index + 1, packet, sessionIdSet);
      }
    } else if (parent != null) {

      ChannelBranch<V> wildBranch;

      if ((wildBranch = parent.childMap.get(StringSegment.wild())) != null) {
        wildBranch.deliverToChannel(packet, sessionIdSet);
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

  protected void removeDeadLeaves (Segment segment) {

    if ((segment != null) && (parent != null) && (getChannel() == null) && childMap.isEmpty()) {
      parent.childMap.remove(segment);
    } else {
      for (Map.Entry<Segment, ChannelBranch<V>> childEntry : childMap.entrySet()) {
        childEntry.getValue().removeDeadLeaves(childEntry.getKey());
      }
    }
  }

  public void walk (ChannelOperation<V> operation) {

    operation.operate(this);

    for (ChannelBranch<V> child : childMap.values()) {
      child.walk(operation);
    }
  }
}
