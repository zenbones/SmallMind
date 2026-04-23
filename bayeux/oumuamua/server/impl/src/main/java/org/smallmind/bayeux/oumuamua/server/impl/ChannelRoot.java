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

import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Thin facade over {@link Server} that gives {@link OumuamuaChannel} instances narrowly-scoped
 * access to backbone, codec, routing configuration, and the forward operation without exposing
 * the full server surface area.
 *
 * @param <V> the concrete {@link Value} type used throughout message processing
 */
public class ChannelRoot<V extends Value<V>> {

  private final Server<V> server;

  /**
   * Wraps the given server so that channels can delegate to it through this facade.
   *
   * @param server the server instance to delegate to; must not be {@code null}
   */
  public ChannelRoot (Server<V> server) {

    this.server = server;
  }

  /**
   * Returns the backbone used to distribute messages across cluster nodes.
   *
   * @return the configured {@link Backbone}, or {@code null} if no backbone is in use
   */
  public Backbone<V> getBackbone () {

    return server.getBackbone();
  }

  /**
   * Returns the codec used to construct and serialize Bayeux messages.
   *
   * @return the server's codec; never {@code null}
   */
  public Codec<V> getCodec () {

    return server.getCodec();
  }

  /**
   * Indicates whether published messages on the given route should be echoed back to the
   * publishing session.
   *
   * @param route the channel route to check
   * @return {@code true} if reflection is enabled for the route
   */
  public boolean isReflecting (Route route) {

    return server.isReflecting(route);
  }

  /**
   * Indicates whether messages on the given route should bypass the long-poll queue and be pushed
   * directly over the active connection.
   *
   * @param route the channel route to check
   * @return {@code true} if streaming delivery is enabled for the route
   */
  public boolean isStreaming (Route route) {

    return server.isStreaming(route);
  }

  /**
   * Forwards the packet through the server's {@link Server#forward} path, delivering it to the
   * channel's local subscribers and publishing it to the backbone.
   *
   * @param channel the channel on which the packet originates
   * @param packet  the packet to forward; must carry a non-{@code null} route
   */
  public void forward (Channel<V> channel, Packet<V> packet) {

    server.forward(channel, packet);
  }
}
