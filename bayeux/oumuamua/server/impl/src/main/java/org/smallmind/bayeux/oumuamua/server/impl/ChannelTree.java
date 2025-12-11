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
 * Rooted channel tree that coordinates concurrent channel creation and cleanup.
 *
 * @param <V> value representation
 */
public class ChannelTree<V extends Value<V>> extends ChannelBranch<V> {

  private final ReentrantLock treeChangeLock = new ReentrantLock();
  private final ChannelRoot<V> root;

  /**
   * Creates a channel tree anchored to the provided root.
   *
   * @param root shared server adapter
   */
  public ChannelTree (ChannelRoot<V> root) {

    super(null);

    this.root = root;
  }

  /**
   * Lazily creates the channel defined by the route, including intermediate branches.
   *
   * @param timeToLive             channel ttl in milliseconds
   * @param index                  starting route index
   * @param route                  target route
   * @param channelCallback        callback invoked when a channel is created
   * @param onSubscribedCallback   callback invoked on subscription
   * @param onUnsubscribedCallback callback invoked on unsubscription
   * @param initializerQueue       channel initializers to run
   * @return created or existing channel
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
   * Removes empty branches from the tree.
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
