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
package org.smallmind.memcached.cubby;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import org.smallmind.memcached.cubby.command.Command;
import org.smallmind.memcached.cubby.response.Response;

/**
 * Spreads memcached commands across a fixed pool of {@link ConnectionCoordinator} instances to
 * increase throughput under concurrent load.
 *
 * <p>The number of coordinators is determined by
 * {@link CubbyConfiguration#getConnectionsPerHost()}, which effectively controls the number of
 * independent TCP connections maintained to each host. When more than one coordinator exists, a
 * coordinator is chosen at random for each outbound command using
 * {@link ThreadLocalRandom}, avoiding contention on a single shared connection.</p>
 *
 * <p>{@link CubbyMemcachedClient} holds exactly one {@code ConnectionMultiplexer}.</p>
 */
public class ConnectionMultiplexer {

  private final ConnectionCoordinator[] connectionCoordinators;

  /**
   * Creates a multiplexer that owns one {@link ConnectionCoordinator} per configured connection
   * slot. Each coordinator independently manages its own set of connections to every host.
   *
   * @param configuration  runtime settings; {@link CubbyConfiguration#getConnectionsPerHost()}
   *                       determines the number of coordinators
   * @param memcachedHosts the hosts forming the memcached cluster; passed through to each
   *                       coordinator
   */
  public ConnectionMultiplexer (CubbyConfiguration configuration, MemcachedHost... memcachedHosts) {

    connectionCoordinators = new ConnectionCoordinator[configuration.getConnectionsPerHost()];

    for (int index = 0; index < connectionCoordinators.length; index++) {
      connectionCoordinators[index] = new ConnectionCoordinator(configuration, memcachedHosts);
    }
  }

  /**
   * Starts every managed {@link ConnectionCoordinator}, opening their respective connections and
   * launching background health-monitoring threads.
   *
   * @throws InterruptedException    if the calling thread is interrupted while starting a coordinator
   * @throws IOException             if a socket cannot be opened during startup
   * @throws CubbyOperationException if a coordinator fails to initialize
   */
  public synchronized void start ()
    throws InterruptedException, IOException, CubbyOperationException {

    for (ConnectionCoordinator connectionCoordinator : connectionCoordinators) {
      connectionCoordinator.start();
    }
  }

  /**
   * Stops every managed {@link ConnectionCoordinator}, closing all open connections and
   * terminating background health-monitoring threads.
   *
   * @throws InterruptedException if the calling thread is interrupted while awaiting shutdown
   * @throws IOException          if closing a connection fails
   */
  public synchronized void stop ()
    throws InterruptedException, IOException {

    for (ConnectionCoordinator connectionCoordinator : connectionCoordinators) {
      connectionCoordinator.stop();
    }
  }

  /**
   * Dispatches a command to a randomly selected {@link ConnectionCoordinator}.
   *
   * <p>When only one coordinator exists the random selection is skipped. For multiple
   * coordinators, {@link ThreadLocalRandom} is used to pick an index, distributing load without
   * introducing shared state contention.</p>
   *
   * @param command        the command to send to the cluster
   * @param timeoutSeconds optional per-request timeout in seconds; {@code null} defers to the
   *                       configured default
   * @return the server's parsed response
   * @throws InterruptedException    if the calling thread is interrupted while awaiting a response
   * @throws IOException             if a network error occurs during transmission
   * @throws CubbyOperationException if routing fails or the server returns an error
   */
  public Response send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException {

    int index = 0;

    if (connectionCoordinators.length > 1) {
      index = ThreadLocalRandom.current().nextInt(connectionCoordinators.length);
    }

    return connectionCoordinators[index].send(command, timeoutSeconds);
  }
}
