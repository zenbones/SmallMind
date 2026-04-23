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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Background daemon that periodically probes offline memcached hosts and triggers reconnection
 * when a host becomes reachable again.
 *
 * <p>{@code ServerDefibrillator} runs as a daemon thread launched by {@link ConnectionCoordinator}
 * during startup. On each iteration it sleeps for the configured
 * {@link CubbyConfiguration#getResuscitationSeconds() resuscitation interval}, then iterates the
 * {@link ServerPool} looking for hosts whose {@link HostControl#isActive()} flag is {@code false}.
 * For each inactive host it opens a plain TCP socket to verify reachability. If the connection
 * succeeds the host is added to a reconnection queue; after all inactive hosts have been probed the
 * queue is drained by calling {@link ConnectionCoordinator#reconnect(MemcachedHost)} for each
 * candidate.</p>
 *
 * <p>A fresh {@link InetSocketAddress} is constructed for each probe so that DNS changes
 * (e.g., a load balancer rotating its backend address) are picked up automatically. The resolved
 * address is stored back into the {@link MemcachedHost} via
 * {@link MemcachedHost#regenerate(InetSocketAddress)} before reconnection.</p>
 *
 * <p>Shutdown is cooperative: {@link #stop()} signals the loop via a {@link CountDownLatch} and
 * blocks until the run loop confirms termination via a second latch.</p>
 */
public class ServerDefibrillator implements Runnable {

  private final CountDownLatch finishedLatch = new CountDownLatch(1);
  private final CountDownLatch terminatedLatch = new CountDownLatch(1);
  private final ConnectionCoordinator connectionCoordinator;
  private final ServerPool serverPool;
  private final long resuscitationSeconds;
  private final int connectionTimeoutMilliseconds;
  private final int readTimeoutMilliseconds;

  /**
   * Constructs a defibrillator bound to the given coordinator and pool.
   *
   * @param connectionCoordinator the coordinator used to rebuild connections for recovered hosts
   * @param configuration         runtime configuration supplying timeout and interval values
   * @param serverPool            the pool of hosts to monitor for inactive entries
   */
  public ServerDefibrillator (ConnectionCoordinator connectionCoordinator, CubbyConfiguration configuration, ServerPool serverPool) {

    this.connectionCoordinator = connectionCoordinator;
    this.serverPool = serverPool;

    this.connectionTimeoutMilliseconds = (int)configuration.getConnectionTimeoutMilliseconds();
    this.readTimeoutMilliseconds = (int)configuration.getReadTimeoutMilliseconds();
    this.resuscitationSeconds = configuration.getResuscitationSeconds();
  }

  /**
   * Signals the monitoring loop to stop and waits until the run loop has fully terminated.
   *
   * @throws InterruptedException if the calling thread is interrupted while awaiting termination
   */
  public void stop ()
    throws InterruptedException {

    finishedLatch.countDown();
    terminatedLatch.await();
  }

  /**
   * Executes the host-monitoring loop. On each iteration the thread sleeps for the configured
   * resuscitation interval, then probes all inactive hosts via a plain TCP connection attempt.
   * Hosts that respond successfully are reconnected through the {@link ConnectionCoordinator}.
   *
   * <p>The loop exits when {@link #stop()} is called or the thread is interrupted. In both cases
   * {@link #terminatedLatch} is counted down to unblock any caller waiting in {@link #stop()}.</p>
   */
  @Override
  public void run () {

    try {
      while (!finishedLatch.await(resuscitationSeconds, TimeUnit.SECONDS)) {

        LinkedList<MemcachedHost> reconnectionList = new LinkedList<>();

        for (HostControl hostControl : serverPool.values()) {
          if (!hostControl.isActive()) {
            try (Socket socket = new Socket()) {

              // In case disconnection was due to a change in downstream load balancing
              InetSocketAddress constructedAddress;

              socket.setSoTimeout(readTimeoutMilliseconds);
              socket.connect(constructedAddress = hostControl.getMemcachedHost().constructAddress(), connectionTimeoutMilliseconds);
              reconnectionList.add(hostControl.getMemcachedHost().regenerate(constructedAddress));
            } catch (IOException ioException) {
              // do nothing
            }
          }
        }

        if (!reconnectionList.isEmpty()) {
          for (MemcachedHost memcachedHost : reconnectionList) {
            try {
              connectionCoordinator.reconnect(memcachedHost);
            } catch (IOException | CubbyOperationException exception) {
              LoggerManager.getLogger(ServerDefibrillator.class).error(exception);
            }
          }
        }
      }
    } catch (InterruptedException interruptedException) {
      finishedLatch.countDown();
      LoggerManager.getLogger(ServerDefibrillator.class).error(interruptedException);
    } finally {
      LoggerManager.getLogger(ServerDefibrillator.class).info("%s stopping...", ServerDefibrillator.class.getName());
      terminatedLatch.countDown();
    }
  }
}
