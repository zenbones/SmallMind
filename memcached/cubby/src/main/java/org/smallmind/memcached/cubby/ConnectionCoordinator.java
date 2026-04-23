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
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.memcached.cubby.command.Command;
import org.smallmind.memcached.cubby.connection.CubbyConnection;
import org.smallmind.memcached.cubby.connection.NIOCubbyConnection;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Manages the full lifecycle of one logical set of connections to a memcached cluster.
 *
 * <p>{@code ConnectionCoordinator} owns a {@link ServerPool} and maintains one
 * {@link CubbyConnection} per host. It is responsible for:</p>
 * <ul>
 *   <li>Starting and stopping individual host connections.</li>
 *   <li>Installing and refreshing the routing table in the configured
 *       {@link org.smallmind.memcached.cubby.locator.KeyLocator} as hosts join or leave.</li>
 *   <li>Launching the {@link ServerDefibrillator} daemon thread that probes offline hosts and
 *       triggers reconnection.</li>
 *   <li>Routing outbound {@link Command} objects to the correct connection based on key
 *       affinity.</li>
 * </ul>
 *
 * <p>All routing and connection-map mutations are protected by a {@link ReentrantReadWriteLock}.
 * Lifecycle transitions ({@link #start()} and {@link #stop()}) are additionally synchronized
 * and guarded by an {@link AtomicReference} holding a {@link ComponentStatus}.</p>
 *
 * <p>A {@link ConnectionMultiplexer} typically owns one or more {@code ConnectionCoordinator}
 * instances, distributing load across them.</p>
 */
public class ConnectionCoordinator {

  private final AtomicReference<ComponentStatus> status = new AtomicReference<>(ComponentStatus.STOPPED);
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final CubbyConfiguration configuration;
  private final ServerPool serverPool;
  private final HashMap<String, CubbyConnection> connectionMap = new HashMap<>();
  private ServerDefibrillator serverDefibrillator;

  /**
   * Constructs a coordinator for the given hosts using the supplied configuration.
   *
   * @param configuration  runtime settings covering routing, codec and connection parameters
   * @param memcachedHosts the hosts this coordinator will manage
   */
  public ConnectionCoordinator (CubbyConfiguration configuration, MemcachedHost... memcachedHosts) {

    this.configuration = configuration;

    serverPool = new ServerPool(memcachedHosts);
  }

  /**
   * Starts the coordinator: installs the initial routing table, launches the
   * {@link ServerDefibrillator} daemon, and opens a {@link NIOCubbyConnection} to each host.
   * This method is idempotent — subsequent calls while already started are silently ignored.
   *
   * @throws InterruptedException    if the calling thread is interrupted while opening connections
   * @throws IOException             if a socket cannot be opened to one of the hosts
   * @throws CubbyOperationException if connection initialization fails
   */
  public synchronized void start ()
    throws InterruptedException, IOException, CubbyOperationException {

    if (status.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING)) {
      lock.writeLock().lock();

      try {

        Thread defibrillatorThread;

        configuration.getKeyLocator().installRouting(serverPool);

        defibrillatorThread = new Thread(serverDefibrillator = new ServerDefibrillator(this, configuration, serverPool));
        defibrillatorThread.setDaemon(true);
        defibrillatorThread.start();

        for (HostControl hostControl : serverPool.values()) {
          LoggerManager.getLogger(ConnectionCoordinator.class).info("Connecting to memcached host(%s=%s)", hostControl.getMemcachedHost().getName(), hostControl.getMemcachedHost().getAddress());
          constructConnection(hostControl.getMemcachedHost());
        }

        status.set(ComponentStatus.STARTED);
      } finally {
        lock.writeLock().unlock();
      }
    }
  }

  /**
   * Stops the coordinator: signals the {@link ServerDefibrillator} to terminate and closes all
   * active connections. This method is idempotent — subsequent calls while already stopped are
   * silently ignored.
   *
   * @throws InterruptedException if the calling thread is interrupted while awaiting shutdown
   * @throws IOException          if closing a connection fails
   */
  public synchronized void stop ()
    throws InterruptedException, IOException {

    if (status.compareAndSet(ComponentStatus.STARTED, ComponentStatus.STOPPING)) {
      lock.writeLock().lock();

      try {
        serverDefibrillator.stop();

        for (HostControl hostControl : serverPool.values()) {

          CubbyConnection connection;

          if ((connection = connectionMap.get(hostControl.getMemcachedHost().getName())) != null) {
            connection.stop();
          }
        }

        status.set(ComponentStatus.STOPPED);
      } finally {
        lock.writeLock().unlock();
      }
    }
  }

  /**
   * Routes a command to the connection for the host determined by the key locator and sends it.
   *
   * @param command        the command to dispatch
   * @param timeoutSeconds optional timeout override in seconds; {@code null} defers to the
   *                       configured default
   * @return the server's parsed response
   * @throws InterruptedException    if the calling thread is interrupted while waiting for a reply
   * @throws IOException             if network communication fails
   * @throws CubbyOperationException if the coordinator is not started, routing fails, or the
   *                                 server returns an error
   */
  public Response send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException {

    return getConnection(command).send(command, timeoutSeconds);
  }

  /**
   * Resolves the {@link CubbyConnection} responsible for the key embedded in the command.
   *
   * @param command the command whose key determines host selection
   * @return the active connection for the target host
   * @throws IOException             if the key locator encounters an I/O error
   * @throws CubbyOperationException if the coordinator is stopped or no connection exists for
   *                                 the resolved host
   */
  private CubbyConnection getConnection (Command command)
    throws IOException, CubbyOperationException {

    lock.readLock().lock();
    try {
      if (!ComponentStatus.STARTED.equals(status.get())) {
        throw new CubbyOperationException("The connection has been stopped");
      } else {

        CubbyConnection cubbyConnection;
        MemcachedHost memcachedHost = configuration.getKeyLocator().find(serverPool, command.getKey());

        if ((cubbyConnection = connectionMap.get(memcachedHost.getName())) == null) {
          throw new CubbyOperationException("Missing connection(%s)", memcachedHost.getName());
        } else {

          return cubbyConnection;
        }
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Marks a host as inactive and refreshes the routing table to exclude it from future requests.
   * Called by a {@link CubbyConnection} when it detects that its host has become unreachable.
   *
   * @param memcachedHost the host that has gone offline
   */
  public void disconnect (MemcachedHost memcachedHost) {

    lock.writeLock().lock();

    try {

      HostControl hostControl;

      if ((hostControl = serverPool.get(memcachedHost.getName())) == null) {
        LoggerManager.getLogger(ConnectionCoordinator.class).info("Missing control entry for memcached host(%s=%s)", memcachedHost.getName(), memcachedHost.getAddress());
      } else {
        hostControl.setActive(false);
        configuration.getKeyLocator().updateRouting(serverPool);
        LoggerManager.getLogger(ConnectionCoordinator.class).info("Disconnected memcached host(%s=%s)", memcachedHost.getName(), memcachedHost.getAddress());
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Rebuilds the connection to a host that has been confirmed reachable by the
   * {@link ServerDefibrillator}, marks it active, and refreshes the routing table to include it.
   *
   * @param memcachedHost the host to reconnect
   * @throws InterruptedException    if the calling thread is interrupted while opening the connection
   * @throws IOException             if a network error occurs during connection setup
   * @throws CubbyOperationException if connection construction fails
   */
  public void reconnect (MemcachedHost memcachedHost)
    throws InterruptedException, IOException, CubbyOperationException {

    lock.writeLock().lock();

    try {

      HostControl hostControl;

      if ((hostControl = serverPool.get(memcachedHost.getName())) == null) {
        LoggerManager.getLogger(ConnectionCoordinator.class).info("Missing control entry for memcached host(%s=%s)", memcachedHost.getName(), memcachedHost.getAddress());
      } else {
        constructConnection(hostControl.getMemcachedHost());
        hostControl.setActive(true);
        configuration.getKeyLocator().updateRouting(serverPool);
        LoggerManager.getLogger(ConnectionCoordinator.class).info("Reconnected memcached host(%s=%s)", memcachedHost.getName(), memcachedHost.getAddress());
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Creates a new {@link NIOCubbyConnection} for the given host, stops any prior connection
   * registered under the same name, and starts the new connection's I/O loop in a daemon thread.
   *
   * @param memcachedHost the host for which to build a connection
   * @throws InterruptedException    if interrupted while opening the connection
   * @throws IOException             if socket creation fails
   * @throws CubbyOperationException if connection initialization fails
   */
  private void constructConnection (MemcachedHost memcachedHost)
    throws InterruptedException, IOException, CubbyOperationException {

    CubbyConnection constructedConnection;
    CubbyConnection priorConnection;

    if ((priorConnection = connectionMap.get(memcachedHost.getName())) != null) {
      priorConnection.stop();
    }

    connectionMap.put(memcachedHost.getName(), constructedConnection = new NIOCubbyConnection(this, configuration, memcachedHost));
    constructedConnection.start();
    new Thread(constructedConnection).start();
  }
}
