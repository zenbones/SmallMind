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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.function.Consumer;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.ChannelStateException;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * {@link ChannelOperation} that prunes idle, non-persistent channels from the tree.
 *
 * @param <V> value representation
 */
public class IdleChannelOperation<V extends Value<V>> implements ChannelOperation<V> {

  private final Consumer<Channel<V>> channelCallback;
  private final Level idleChannelLogLevel;
  private final long now;

  /**
   * Creates an operation that removes idle channels older than the supplied timestamp.
   *
   * @param now reference time used to determine idleness
   * @param idleChannelLogLevel log level for termination events
   * @param channelCallback callback invoked when a channel is removed
   */
  public IdleChannelOperation (long now, Level idleChannelLogLevel, Consumer<Channel<V>> channelCallback) {

    this.now = now;
    this.idleChannelLogLevel = idleChannelLogLevel;
    this.channelCallback = channelCallback;
  }

  /**
   * Removes the channel from the supplied branch when it has expired.
   *
   * @param channelBranch branch to inspect
   */
  @Override
  public void operate (ChannelBranch<V> channelBranch) {

    Channel<V> channel;

    if (((channel = channelBranch.getChannel()) != null) && channel.isRemovable(now)) {
      try {
        LoggerManager.getLogger(IdleChannelOperation.class).log(idleChannelLogLevel, "Idle channel termination(%s)", channel.getRoute().getPath());

        channelBranch.removeChannel(channelCallback);
      } catch (ChannelStateException channelStateException) {
        LoggerManager.getLogger(IdleChannelOperation.class).error(channelStateException);
      }
    }
  }
}
