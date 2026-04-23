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
import org.smallmind.bayeux.oumuamua.server.api.ChannelStateException;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * {@link ChannelOperation} that terminates and removes a channel from its branch when the channel
 * reports itself removable at the reference timestamp.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class IdleChannelOperation<V extends Value<V>> implements ChannelOperation<V> {

  private final Consumer<Channel<V>> channelCallback;
  private final Level idleChannelLogLevel;
  private final long now;

  /**
   * Creates an operation that will prune channels whose idle period exceeds their TTL relative to
   * the given reference time.
   *
   * @param now                 the epoch millisecond timestamp passed to
   *                            {@link Channel#isRemovable(long)}
   * @param idleChannelLogLevel log level at which channel termination events are recorded
   * @param channelCallback     invoked with each channel that is removed; forwarded to
   *                            {@link ChannelBranch#removeChannel(java.util.function.Consumer)}
   */
  public IdleChannelOperation (long now, Level idleChannelLogLevel, Consumer<Channel<V>> channelCallback) {

    this.now = now;
    this.idleChannelLogLevel = idleChannelLogLevel;
    this.channelCallback = channelCallback;
  }

  /**
   * Checks the channel at the given branch and removes it when it is removable; logs the
   * termination event and silently absorbs any {@link ChannelStateException} (which would indicate
   * a now-persistent channel that should not be removed).
   *
   * @param channelBranch the branch to inspect; no-op if the branch carries no channel
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
