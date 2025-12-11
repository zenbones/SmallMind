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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.ChannelStateException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Segment;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.StringSegment;

/**
 * Node in the channel tree that holds a channel and any child branches.
 *
 * @param <V> value representation
 */
public class ChannelBranch<V extends Value<V>> {

  private final ReentrantReadWriteLock channelChangeLock = new ReentrantReadWriteLock();
  private final ConcurrentHashMap<Segment, ChannelBranch<V>> childMap = new ConcurrentHashMap<>();
  private final ChannelBranch<V> parent;
  private Channel<V> channel;

  /**
   * Creates a branch with an optional parent branch.
   *
   * @param parent parent branch in the channel hierarchy, or {@code null} for the root
   */
  public ChannelBranch (ChannelBranch<V> parent) {

    this.parent = parent;
  }

  /**
   * Retrieves the channel associated with this branch.
   *
   * @return the channel, or {@code null} when not yet created
   */
  public Channel<V> getChannel () {

    channelChangeLock.readLock().lock();

    try {

      return channel;
    } finally {
      channelChangeLock.readLock().unlock();
    }
  }

  /**
   * Recursively finds a channel using the provided route segments.
   *
   * @param index current segment index
   * @param route route to traverse
   * @return channel at the route location, or {@code null} if not found
   */
  public Channel<V> find (int index, DefaultRoute route) {

    ChannelBranch<V> child;

    if ((child = childMap.get(route.getSegment(index))) == null) {

      return null;
    } else {

      return (index == route.lastIndex()) ? child.getChannel() : child.find(index + 1, route);
    }
  }

  /**
   * Creates intermediate branches and the channel as necessary to satisfy the route.
   *
   * @param timeToLive             channel ttl in milliseconds
   * @param index                  current segment index
   * @param route                  route being constructed
   * @param root                   root container for all channels
   * @param channelCallback        callback invoked when a channel is created
   * @param onSubscribedCallback   callback invoked on subscription
   * @param onUnsubscribedCallback callback invoked on unsubscription
   * @param initializerQueue       initializers applied to a newly created channel
   * @return resulting channel instance
   */
  protected Channel<V> addChannelAsNecessary (long timeToLive, int index, DefaultRoute route, ChannelRoot<V> root, Consumer<Channel<V>> channelCallback, BiConsumer<Channel<V>, Session<V>> onSubscribedCallback, BiConsumer<Channel<V>, Session<V>> onUnsubscribedCallback, Queue<ChannelInitializer<V>> initializerQueue) {

    ChannelBranch<V> child;
    Segment segment;

    if ((child = childMap.get(segment = route.getSegment(index))) == null) {
      childMap.put(segment, child = new ChannelBranch<>(this));
    }

    return (index == route.lastIndex()) ? child.initializeChannel(timeToLive, route, root, channelCallback, onSubscribedCallback, onUnsubscribedCallback, initializerQueue) : child.addChannelAsNecessary(timeToLive, index + 1, route, root, channelCallback, onSubscribedCallback, onUnsubscribedCallback, initializerQueue);
  }

  /**
   * Initializes a channel on this branch if absent.
   *
   * @param timeToLive             channel ttl in milliseconds
   * @param route                  channel route
   * @param root                   root container
   * @param channelCallback        callback invoked after creation
   * @param onSubscribedCallback   callback invoked on subscription
   * @param onUnsubscribedCallback callback invoked on unsubscription
   * @param initializerQueue       initializers to apply to the channel
   * @return created or existing channel
   */
  private Channel<V> initializeChannel (long timeToLive, DefaultRoute route, ChannelRoot<V> root, Consumer<Channel<V>> channelCallback, BiConsumer<Channel<V>, Session<V>> onSubscribedCallback, BiConsumer<Channel<V>, Session<V>> onUnsubscribedCallback, Queue<ChannelInitializer<V>> initializerQueue) {

    channelChangeLock.writeLock().lock();

    try {
      if (channel == null) {
        channel = new OumuamuaChannel<>(onSubscribedCallback, onUnsubscribedCallback, timeToLive, route, root);

        if (initializerQueue != null) {
          for (ChannelInitializer<V> initializer : initializerQueue) {
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

  /**
   * Removes the channel at the route if present, returning the branch holding it.
   *
   * @param index           current segment index
   * @param route           route to remove
   * @param channelCallback callback invoked when a channel is removed
   * @return branch containing the removed channel, or {@code null} if not found
   * @throws ChannelStateException if a persistent channel is targeted for removal
   */
  public ChannelBranch<V> removeChannelIfPresent (int index, Route route, Consumer<Channel<V>> channelCallback)
    throws ChannelStateException {

    ChannelBranch<V> child;

    if ((child = childMap.get(route.getSegment(index))) == null) {

      return null;
    } else {

      return (index == route.lastIndex()) ? child.removeChannel(channelCallback) : child.removeChannelIfPresent(index + 1, route, channelCallback);
    }
  }

  /**
   * Removes the channel from this branch, respecting persistence rules.
   *
   * @param channelCallback callback invoked after channel termination
   * @return this branch instance
   * @throws ChannelStateException if the channel is persistent and cannot be removed
   */
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

  /**
   * Delivers a packet to matching child channels based on the route.
   *
   * @param sender       originating session
   * @param index        current route index
   * @param packet       packet to deliver
   * @param sessionIdSet set used to track delivery to avoid duplicates
   */
  public void deliver (Session<V> sender, int index, Packet<V> packet, Set<String> sessionIdSet) {

    if (index < packet.getRoute().size()) {

      ChannelBranch<V> deepWildBranch;
      ChannelBranch<V> nextBranch;

      if ((deepWildBranch = childMap.get(StringSegment.deepWild())) != null) {

        deepWildBranch.deliverToChannel(sender, packet, sessionIdSet);
      }
      if ((nextBranch = childMap.get(((DefaultRoute)packet.getRoute()).getSegment(index))) != null) {
        nextBranch.deliver(sender, index + 1, packet, sessionIdSet);
      }
    } else if (parent != null) {

      ChannelBranch<V> wildBranch;

      if ((wildBranch = parent.childMap.get(StringSegment.wild())) != null) {
        wildBranch.deliverToChannel(sender, packet, sessionIdSet);
      }

      deliverToChannel(sender, packet, sessionIdSet);
    }
  }

  /**
   * Delivers the packet to the channel at this branch if present.
   *
   * @param sender       originating session
   * @param packet       packet to deliver
   * @param sessionIdSet set tracking delivered session ids
   */
  private void deliverToChannel (Session<V> sender, Packet<V> packet, Set<String> sessionIdSet) {

    channelChangeLock.readLock().lock();

    try {
      if (channel != null) {
        channel.deliver(sender, packet, sessionIdSet);
      }
    } finally {
      channelChangeLock.readLock().unlock();
    }
  }

  /**
   * Removes empty branches from the tree to keep the structure compact.
   *
   * @param segment segment used to remove this branch from the parent
   */
  protected void removeDeadLeaves (Segment segment) {

    if ((segment != null) && (parent != null) && (getChannel() == null) && childMap.isEmpty()) {
      parent.childMap.remove(segment);
    } else {
      for (Map.Entry<Segment, ChannelBranch<V>> childEntry : childMap.entrySet()) {
        childEntry.getValue().removeDeadLeaves(childEntry.getKey());
      }
    }
  }

  /**
   * Walks the branch hierarchy depth-first, invoking the supplied operation.
   *
   * @param operation callback applied to each branch
   */
  public void walk (ChannelOperation<V> operation) {

    operation.operate(this);

    for (ChannelBranch<V> child : childMap.values()) {
      child.walk(operation);
    }
  }
}
