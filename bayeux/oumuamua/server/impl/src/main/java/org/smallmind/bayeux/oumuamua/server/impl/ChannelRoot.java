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
 * Adapter that exposes server-level behavior to channel instances.
 *
 * @param <V> value representation
 */
public class ChannelRoot<V extends Value<V>> {

  private final Server<V> server;

  /**
   * Creates a new root facade for the given server.
   *
   * @param server owning server
   */
  public ChannelRoot (Server<V> server) {

    this.server = server;
  }

  /**
   * @return backbone used to distribute messages across nodes
   */
  public Backbone<V> getBackbone () {

    return server.getBackbone();
  }

  /**
   * @return codec used to create and manipulate messages
   */
  public Codec<V> getCodec () {

    return server.getCodec();
  }

  /**
   * Determines whether the route should reflect messages to the publishing session.
   *
   * @param route channel route
   * @return {@code true} if reflections are enabled
   */
  public boolean isReflecting (Route route) {

    return server.isReflecting(route);
  }

  /**
   * Determines whether the route should stream data to subscribers.
   *
   * @param route channel route
   * @return {@code true} if streaming is enabled
   */
  public boolean isStreaming (Route route) {

    return server.isStreaming(route);
  }

  /**
   * Forwards a packet to the server backbone for delivery.
   *
   * @param channel channel originating the packet
   * @param packet  packet to forward
   */
  public void forward (Channel<V> channel, Packet<V> packet) {

    server.forward(channel, packet);
  }
}
