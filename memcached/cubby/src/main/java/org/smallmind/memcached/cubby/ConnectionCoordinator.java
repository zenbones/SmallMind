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
 * Coordinates a set of connections to memcached hosts, handling lifecycle and routing updates.
 */
public class ConnectionCoordinator {

  private final AtomicReference<ComponentStatus> status = new AtomicReference<>(ComponentStatus.STOPPED);
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final CubbyConfiguration configuration;
  private final ServerPool serverPool;
  private final HashMap<String, CubbyConnection> connectionMap = new HashMap<>();
  private ServerDefibrillator serverDefibrillator;

  /**
   * Builds a coordinator for the provided hosts and configuration.
   *
   * @param configuration  runtime configuration including routing and connection settings
   * @param memcachedHosts target hosts to manage
   */
  public ConnectionCoordinator (CubbyConfiguration configuration, MemcachedHost... memcachedHosts) {

    this.configuration = configuration;

    serverPool = new ServerPool(memcachedHosts);
  }

  /**
   * Starts the coordinator by constructing connections and launching the defibrillator monitor.
   *
   * @throws InterruptedException    if interrupted while connecting
   * @throws IOException             if sockets cannot be opened
   * @throws CubbyOperationException if initialization fails
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
   * Stops the coordinator, shutting down health monitoring and active connections.
   *
   * @throws InterruptedException if interrupted while closing
   * @throws IOException          if a connection cannot be closed
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
   * Sends a command over the connection mapped to its key.
   *
   * @param command        command to dispatch
   * @param timeoutSeconds optional timeout in seconds; {@code null} uses configured default
   * @return response returned by the server
   * @throws InterruptedException    if interrupted while waiting
   * @throws IOException             if network communication fails
   * @throws CubbyOperationException if routing fails or the server reports an error
   */
  public Response send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException {

    return getConnection(command).send(command, timeoutSeconds);
  }

  /**
   * Resolves the connection associated with the supplied command key.
   *
   * @param command command whose key determines routing
   * @return an active connection for the key
   * @throws IOException             if routing table access fails
   * @throws CubbyOperationException if the coordinator is stopped or a connection is missing
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
   * Marks the host as inactive and triggers routing table recalculation.
   *
   * @param memcachedHost host to mark disconnected
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
   * Reconnects a host, rebuilding its connection and re-enabling routing.
   *
   * @param memcachedHost host to reconnect
   * @throws InterruptedException    if interrupted while reconnecting
   * @throws IOException             if network I/O fails
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
   * Builds a new connection to the host, replacing any existing connection.
   *
   * @param memcachedHost host to connect to
   * @throws InterruptedException    if interrupted while opening
   * @throws IOException             if socket creation fails
   * @throws CubbyOperationException if initialization fails
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
