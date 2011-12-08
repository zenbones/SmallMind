/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.pool.connection;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.scribe.pen.LoggerManager;

public class ConnectionPinManager<C> {

  private static enum State {STOPPED, STARTING, STARTED, STOPPING}

  private final ConnectionPool<C> connectionPool;
  private final HashMap<ConnectionInstance<C>, ConnectionPin<C>> backingMap = new HashMap<ConnectionInstance<C>, ConnectionPin<C>>();
  private final LinkedBlockingQueue<ConnectionPin<C>> freeQueue = new LinkedBlockingQueue<ConnectionPin<C>>();
  private final ReentrantReadWriteLock backingLock = new ReentrantReadWriteLock();
  private final DeconstructionQueue deconstructionQueue = new DeconstructionQueue();
  private final AtomicReference<State> stateRef = new AtomicReference<State>(State.STOPPED);
  private final AtomicInteger size = new AtomicInteger(0);

  public ConnectionPinManager (ConnectionPool<C> connectionPool) {

    this.connectionPool = connectionPool;
  }

  public void startup ()
    throws ConnectionPoolException {

    if (stateRef.compareAndSet(State.STOPPED, State.STARTING)) {
      deconstructionQueue.startup();

      backingLock.writeLock().lock();
      try {
        while (backingMap.size() < Math.max(connectionPool.getConnectionPoolConfig().getMinPoolSize(), connectionPool.getConnectionPoolConfig().getInitialPoolSize())) {

          ConnectionPin<C> connectionPin;
          ConnectionInstance<C> connectionInstance;

          backingMap.put(connectionInstance = connectionPool.getConnectionInstanceFactory().createInstance(connectionPool), connectionPin = new ConnectionPin<C>(connectionPool, deconstructionQueue, connectionInstance));
          freeQueue.put(connectionPin);
        }

        size.set(backingMap.size());
        stateRef.set(State.STARTED);
      }
      catch (Exception exception) {
        freeQueue.clear();
        backingMap.clear();
        size.set(0);
        stateRef.set(State.STOPPED);

        throw new ConnectionPoolException(exception);
      }
      finally {
        backingLock.writeLock().unlock();
      }
    }
    else {
      try {
        while (State.STARTING.equals(stateRef.get())) {
          Thread.sleep(100);
        }
      }
      catch (InterruptedException interruptedException) {
        throw new ConnectionPoolException(interruptedException);
      }
    }
  }

  public ConnectionPin<C> serve ()
    throws ConnectionPoolException {

    if (!State.STARTED.equals(stateRef.get())) {
      throw new ConnectionPoolException("ConnectionPool has not been started");
    }

    ConnectionPin<C> connectionPin;

    if ((connectionPin = freeQueue.poll()) != null) {

      if (connectionPool.getConnectionPoolConfig().isTestOnAcquire() && (!connectionPin.getConnectionInstance().validate())) {
        throw new ConnectionValidationException("A free connection was acquired, but failed to validate");
      }

      return connectionPin;
    }

    if ((connectionPin = addConnectionPin(true)) != null) {

      return connectionPin;
    }

    try {
      if ((connectionPin = freeQueue.poll(connectionPool.getConnectionPoolConfig().getAcquireWaitTimeMillis(), TimeUnit.MILLISECONDS)) != null) {

        if (connectionPool.getConnectionPoolConfig().isTestOnAcquire() && (!connectionPin.getConnectionInstance().validate())) {
          throw new ConnectionValidationException("A free connection was acquired, but failed to validate");
        }

        return connectionPin;
      }
    }
    catch (InterruptedException interruptedException) {
      throw new ConnectionPoolException(interruptedException);
    }

    throw new ConnectionPoolException("Exceeded the maximum acquire wait time(%d)", connectionPool.getConnectionPoolConfig().getAcquireWaitTimeMillis());
  }

  private ConnectionPin<C> addConnectionPin (boolean forced)
    throws ConnectionCreationException, ConnectionValidationException {

    if (State.STARTED.equals(stateRef.get())) {

      int minPoolSize = connectionPool.getConnectionPoolConfig().getMinPoolSize();
      int maxPoolSize = connectionPool.getConnectionPoolConfig().getMaxPoolSize();
      int currentSize = size.get();

      if ((forced || (currentSize > minPoolSize)) && ((maxPoolSize == 0) || (currentSize < maxPoolSize))) {
        backingLock.writeLock().lock();
        try {
          currentSize = size.get();

          if ((forced || (currentSize > minPoolSize)) && ((maxPoolSize == 0) || (currentSize < maxPoolSize))) {

            ConnectionPin<C> connectionPin;
            ConnectionInstance<C> connectionInstance;

            backingMap.put(connectionInstance = manufactureConnectionInstance(), connectionPin = new ConnectionPin<C>(connectionPool, deconstructionQueue, connectionInstance));
            size.incrementAndGet();

            return connectionPin;
          }
        }
        finally {
          backingLock.writeLock().unlock();
        }
      }
    }

    return null;
  }

  private ConnectionInstance<C> manufactureConnectionInstance ()
    throws ConnectionCreationException, ConnectionValidationException {

    ConnectionInstance<C> connectionInstance;

    try {
      if (connectionPool.getConnectionPoolConfig().getConnectionTimeoutMillis() > 0) {

        ConnectionWorker<C> connectionWorker;
        Thread workerThread;

        connectionWorker = new ConnectionWorker<C>(connectionPool);
        workerThread = new Thread(connectionWorker);
        workerThread.start();

        workerThread.join(connectionPool.getConnectionPoolConfig().getConnectionTimeoutMillis());
        if (connectionWorker.abort()) {
          throw new ConnectionCreationException("Exceeded connection timeout(%d) waiting on connection creation", connectionPool.getConnectionPoolConfig().getConnectionTimeoutMillis());
        }
        else {
          connectionInstance = connectionWorker.getConnectionInstance();
        }
      }
      else {
        connectionInstance = connectionPool.getConnectionInstanceFactory().createInstance(connectionPool);
      }
    }
    catch (ConnectionCreationException connectionCreationException) {
      throw connectionCreationException;
    }
    catch (Exception exception) {
      throw new ConnectionCreationException(exception);
    }

    if (connectionPool.getConnectionPoolConfig().isTestOnConnect() && (!connectionInstance.validate())) {
      throw new ConnectionValidationException("A new connection was required, but failed to validate");
    }

    return connectionInstance;
  }

  public void remove (ConnectionPin<C> connectionPin, boolean withPrejudice) {

    if (freeQueue.remove(connectionPin) || withPrejudice) {
      terminate(connectionPin.getConnectionInstance());
    }
  }

  public void process (ConnectionInstance<C> connectionInstance) {

    ConnectionPin<C> connectionPin;

    backingLock.readLock().lock();
    try {
      connectionPin = backingMap.get(connectionInstance);
    }
    finally {
      backingLock.readLock().unlock();
    }

    if (connectionPin != null) {
      connectionPin.free();

      if (connectionPin.isTerminated()) {
        terminate(connectionPin.getConnectionInstance());
      }
      else {
        if (State.STARTED.equals(stateRef.get())) {
          try {
            freeQueue.put(connectionPin);
          }
          catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(ConnectionPinManager.class).error(interruptedException);
          }
        }
      }
    }
  }

  public void terminate (ConnectionInstance<C> connectionInstance) {

    ConnectionPin<C> connectionPin;

    backingLock.writeLock().lock();
    try {
      connectionPin = backingMap.remove(connectionInstance);
    }
    finally {
      backingLock.writeLock().unlock();
    }

    if (connectionPin != null) {
      size.decrementAndGet();
      connectionPin.fizzle();

      try {
        connectionPin.getConnectionInstance().close();
      }
      catch (Exception exception) {
        LoggerManager.getLogger(ConnectionPinManager.class).error(exception);
      }

      try {

        ConnectionPin<C> replacementConnectionPin;

        if ((replacementConnectionPin = addConnectionPin(false)) != null) {
          freeQueue.put(replacementConnectionPin);
        }
      }
      catch (Exception exception) {
        LoggerManager.getLogger(ConnectionPinManager.class).error(exception);
      }
    }
  }

  public void shutdown ()
    throws ConnectionPoolException {

    if (stateRef.compareAndSet(State.STARTED, State.STOPPING)) {

      while (size.get() > 0) {

        ConnectionInstance<C>[] activeConnections;

        backingLock.readLock().lock();
        try {

          Set<ConnectionInstance<C>> keys;

          keys = backingMap.keySet();
          activeConnections = new ConnectionInstance[keys.size()];
          keys.toArray(activeConnections);
        }
        finally {
          backingLock.readLock().unlock();
        }

        for (ConnectionInstance<C> activeConnection : activeConnections) {
          terminate(activeConnection);
        }
      }

      freeQueue.clear();

      try {
        deconstructionQueue.shutdown();
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ConnectionPinManager.class).error(interruptedException);
      }

      stateRef.set(State.STOPPED);
    }
    else {
      try {
        while (State.STOPPING.equals(stateRef.get())) {
          Thread.sleep(100);
        }
      }
      catch (InterruptedException interruptedException) {
        throw new ConnectionPoolException(interruptedException);
      }
    }
  }

  public int getPoolSize () {

    return size.get();
  }

  public int getFreeSize () {

    return freeQueue.size();
  }

  public int getProcessingSize () {

    return getPoolSize() - getFreeSize();
  }

  public StackTrace[] getExistentialStackTraces () {

    LinkedList<StackTrace> stackTraceList = new LinkedList<StackTrace>();
    StackTrace[] stackTraces;

    backingLock.readLock().lock();
    try {
      for (ConnectionPin<C> connectionPin : backingMap.values()) {
        if (!freeQueue.contains(connectionPin)) {
          stackTraceList.add(new StackTrace(connectionPin.getExistentialStackTrace()));
        }
      }
    }
    finally {
      backingLock.readLock().unlock();
    }

    stackTraces = new StackTrace[stackTraceList.size()];
    stackTraceList.toArray(stackTraces);

    return stackTraces;
  }
}
