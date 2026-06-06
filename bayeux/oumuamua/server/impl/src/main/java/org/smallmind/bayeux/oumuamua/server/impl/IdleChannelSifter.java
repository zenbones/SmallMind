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

import java.util.function.Consumer;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Background {@link Runnable} that applies an {@link IdleChannelOperation} to the entire channel
 * tree to prune expired channels, then compacts the tree by removing empty branches. Scheduled on a
 * fixed cadence by {@link OumuamuaServer}, which owns its lifecycle.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class IdleChannelSifter<V extends Value<V>> implements Runnable {

  private final ChannelTree<V> channelTree;
  private final Consumer<Channel<V>> channelCallback;
  private final Level idleChannelLogLevel;

  /**
   * Constructs the sifter with the given removal callback.
   *
   * @param idleChannelLogLevel log level at which channel removal events are recorded
   * @param channelTree         the tree to walk on each scan
   * @param channelCallback     forwarded to {@link IdleChannelOperation} and invoked for each
   *                            channel that is removed
   */
  public IdleChannelSifter (Level idleChannelLogLevel, ChannelTree<V> channelTree, Consumer<Channel<V>> channelCallback) {

    this.idleChannelLogLevel = idleChannelLogLevel;
    this.channelTree = channelTree;
    this.channelCallback = channelCallback;
  }

  /**
   * Performs a single scan: walks the channel tree to prune expired channels and remove dead
   * branches. Any failure of the pass is logged so that it cannot cancel future runs.
   */
  @Override
  public void run () {

    try {
      channelTree.walk(new IdleChannelOperation<V>(System.currentTimeMillis(), idleChannelLogLevel, channelCallback));
      channelTree.clean();
    } catch (Exception exception) {
      LoggerManager.getLogger(IdleChannelSifter.class).error(exception);
    }
  }
}
