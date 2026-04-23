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

import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelInitializer;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;

/**
 * Top-level entry point for the channel hierarchy, adding a tree-wide exclusive lock around
 * structural mutations so that channel creation and dead-branch removal are always consistent.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class ChannelTree<V extends Value<V>> extends ChannelBranch<V> {

  private final ReentrantLock treeChangeLock = new ReentrantLock();
  private final ChannelRoot<V> root;

  /**
   * Creates the tree with the given server adapter as the shared root for all channels.
   *
   * @param root the server-level facade that channels use for codec, backbone, and configuration access
   */
  public ChannelTree (ChannelRoot<V> root) {

    super(null);

    this.root = root;
  }

  /**
   * Returns the channel at the given route, creating it and any missing intermediate branches
   * under an exclusive tree-change lock.
   *
   * @param timeToLive             TTL in milliseconds assigned to a newly created channel
   * @param index                  the starting route segment index (pass {@code 0} from callers)
   * @param route                  the full route identifying the target channel
   * @param channelCallback        invoked with the channel immediately after it is created
   * @param onSubscribedCallback   forwarded to the new channel for subscription events
   * @param onUnsubscribedCallback forwarded to the new channel for unsubscription events
   * @param initializerQueue       initializers applied to the channel on first creation; may be
   *                               {@code null}
   * @return the existing or newly created channel; never {@code null}
   */
  public Channel<V> createIfAbsent (long timeToLive, int index, DefaultRoute route, Consumer<Channel<V>> channelCallback, BiConsumer<Channel<V>, Session<V>> onSubscribedCallback, BiConsumer<Channel<V>, Session<V>> onUnsubscribedCallback, Queue<ChannelInitializer<V>> initializerQueue) {

    treeChangeLock.lock();

    try {

      return addChannelAsNecessary(timeToLive, index, route, root, channelCallback, onSubscribedCallback, onUnsubscribedCallback, initializerQueue);
    } finally {
      treeChangeLock.unlock();
    }
  }

  /**
   * Prunes branches that have no channel and no children, compacting the tree after idle-channel
   * removal; runs under an exclusive tree-change lock.
   */
  public void clean () {

    treeChangeLock.lock();

    try {
      removeDeadLeaves(null);
    } finally {
      treeChangeLock.unlock();
    }
  }
}
