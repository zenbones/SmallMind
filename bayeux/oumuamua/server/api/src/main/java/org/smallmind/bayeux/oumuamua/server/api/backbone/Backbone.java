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
package org.smallmind.bayeux.oumuamua.server.api.backbone;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

/**
 * Cluster messaging bus that replicates published packets to peer server nodes,
 * decoupled from any specific transport protocol.
 *
 * @param <V> concrete {@link Value} implementation used for message payloads
 */
public interface Backbone<V extends Value<V>> {

  /**
   * Starts the backbone and binds it to the given server so it can forward received packets.
   *
   * @param server server instance that will process packets received from other nodes
   * @throws Exception if the backbone cannot start or connect to the cluster
   */
  void startUp (Server<V> server)
    throws Exception;

  /**
   * Stops the backbone and releases all cluster connections and resources.
   *
   * @throws Exception if the backbone cannot cleanly shut down
   */
  void shutDown ()
    throws Exception;

  /**
   * Broadcasts a packet to all peer nodes in the cluster.
   *
   * @param packet packet to distribute across the cluster
   */
  void publish (Packet<V> packet);
}
