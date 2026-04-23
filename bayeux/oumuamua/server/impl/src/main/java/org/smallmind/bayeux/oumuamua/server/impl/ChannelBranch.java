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
 * Single node in the channel path hierarchy, holding an optional {@link Channel} and a map of
 * child nodes keyed by path segment; supports concurrent reads with exclusive writes via a
 * read/write lock on the channel reference.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class ChannelBranch<V extends Value<V>> {

  private final ReentrantReadWriteLock channelChangeLock = new ReentrantReadWriteLock();
  private final ConcurrentHashMap<Segment, ChannelBranch<V>> childMap = new ConcurrentHashMap<>();
  private final ChannelBranch<V> parent;
  private Channel<V> channel;

  /**
   * Allocates a new branch node with the given parent reference.
   *
   * @param parent the branch one level up in the hierarchy, or {@code null} when this node is the
   *               top-level sentinel used by {@link ChannelTree}
   */
  public ChannelBranch (ChannelBranch<V> parent) {

    this.parent = parent;
  }

  /**
   * Returns the channel stored at this branch node under a read lock.
   *
   * @return the channel, or {@code null} if no channel has been placed here yet
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
   * Traverses the child map segment-by-segment to locate the channel at the given route.
   *
   * @param index the current position within {@code route}; incremented on each recursive call
   * @param route the full route whose segments drive the traversal
   * @return the channel at the terminal segment, or {@code null} if any intermediate segment is
   * absent
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
   * Navigates to the terminal segment of the route, creating any absent intermediate branch nodes
   * along the way, then delegates to {@link #initializeChannel} at the leaf.
   *
   * @param timeToLive             TTL in milliseconds for a newly created channel
   * @param index                  the current segment index; incremented on each recursive call
   * @param route                  the full route being resolved
   * @param root                   server facade passed through to the created channel
   * @param channelCallback        invoked with the new channel immediately after creation
   * @param onSubscribedCallback   forwarded to the channel for subscription events
   * @param onUnsubscribedCallback forwarded to the channel for unsubscription events
   * @param initializerQueue       initializers to apply to a newly created channel; may be
   *                               {@code null}
   * @return the existing or newly created channel at the route's terminal position
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
   * Creates the channel on this branch under an exclusive write lock if one does not already
   * exist, runs all initializers, and fires the creation callback.
   *
   * @param timeToLive             TTL in milliseconds assigned to the new channel
   * @param route                  the route the new channel will be registered under
   * @param root                   server facade passed to the {@link OumuamuaChannel} constructor
   * @param channelCallback        invoked with the channel after all initializers have run
   * @param onSubscribedCallback   forwarded to the channel for subscription events
   * @param onUnsubscribedCallback forwarded to the channel for unsubscription events
   * @param initializerQueue       ordered set of initializers to apply; may be {@code null}
   * @return the existing channel if already present, or the newly constructed one
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
   * Traverses the route to find the target branch and removes its channel.
   *
   * @param index           the current segment index; incremented on each recursive call
   * @param route           the full route of the channel to remove
   * @param channelCallback invoked with the terminated channel if one was found and removed
   * @return the branch that held the channel, or {@code null} if the route was not found
   * @throws ChannelStateException if the target channel is persistent and removal is not permitted
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
   * Terminates and nulls out the channel on this branch under an exclusive write lock.
   *
   * @param channelCallback invoked with the terminated {@link OumuamuaChannel} before this method
   *                        returns; only called if a channel was present
   * @return this branch instance, allowing callers to chain inspection of the now-empty branch
   * @throws ChannelStateException if the channel is marked persistent; the channel is left intact
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
   * Routes the packet down the tree, matching literal segments, the single-level wildcard
   * ({@code *}), and the deep wildcard ({@code **}) according to Bayeux routing rules.
   *
   * @param sender       the originating session, or {@code null} for server-side publishes
   * @param index        the current position within the packet's route; incremented on each
   *                     recursive call
   * @param packet       the packet to deliver to matching channel branches
   * @param sessionIdSet accumulates subscriber ids already delivered to, preventing duplicate
   *                     delivery when multiple wildcard patterns match the same subscriber
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
   * Forwards the packet to this branch's channel under a read lock; no-op if the channel is absent.
   *
   * @param sender       the originating session
   * @param packet       the packet to forward to the channel's subscribers
   * @param sessionIdSet the deduplication set forwarded to {@link Channel#deliver}
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
   * Recursively removes childless, channel-free branches from the tree, or removes this branch
   * from its parent when it qualifies.
   *
   * @param segment the key under which this branch is stored in the parent's child map; pass
   *                {@code null} for the root call so the root itself is never removed
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
   * Performs a depth-first traversal of this branch and all descendants, invoking the operation
   * on every node including this one.
   *
   * @param operation the action to perform at each branch; called with this branch first, then
   *                  recursively with each child
   */
  public void walk (ChannelOperation<V> operation) {

    operation.operate(this);

    for (ChannelBranch<V> child : childMap.values()) {
      child.walk(operation);
    }
  }
}
