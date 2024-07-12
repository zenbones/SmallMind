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

public class ConnectionCoordinator {

  private final AtomicReference<ComponentStatus> status = new AtomicReference<>(ComponentStatus.STOPPED);
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final CubbyConfiguration configuration;
  private final ServerPool serverPool;
  private final HashMap<String, CubbyConnection> connectionMap = new HashMap<>();
  private ServerDefibrillator serverDefibrillator;

  public ConnectionCoordinator (CubbyConfiguration configuration, MemcachedHost... memcachedHosts) {

    this.configuration = configuration;

    serverPool = new ServerPool(memcachedHosts);
  }

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

  public Response send (Command command, Long timeoutSeconds)
    throws InterruptedException, IOException, CubbyOperationException {

    return getConnection(command).send(command, timeoutSeconds);
  }

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
