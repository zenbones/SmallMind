/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.quorum.pool.complex;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.PoolManager;
import org.smallmind.quorum.pool.instrument.MetricInteraction;
import org.smallmind.quorum.pool.instrument.MetricSize;
import org.smallmind.scribe.pen.LoggerManager;

public class ComponentPinManager<C> {

  private static enum State {STOPPED, STARTING, STARTED, STOPPING}

  private final ComponentPool<C> componentPool;
  private final HashMap<ComponentInstance<C>, ComponentPin<C>> backingMap = new HashMap<>();
  private final LinkedBlockingQueue<ComponentPin<C>> freeQueue = new LinkedBlockingQueue<>();
  private final ReentrantReadWriteLock backingLock = new ReentrantReadWriteLock();
  private final DeconstructionQueue deconstructionQueue = new DeconstructionQueue();
  private final AtomicReference<State> stateRef = new AtomicReference<>(State.STOPPED);
  private final AtomicInteger size = new AtomicInteger(0);

  public ComponentPinManager (ComponentPool<C> componentPool) {

    this.componentPool = componentPool;
  }

  public void startup ()
    throws ComponentPoolException {

    if (stateRef.compareAndSet(State.STOPPED, State.STARTING)) {
      deconstructionQueue.startup();

      backingLock.writeLock().lock();
      try {
        while (backingMap.size() < Math.max(componentPool.getComplexPoolConfig().getMinPoolSize(), componentPool.getComplexPoolConfig().getInitialPoolSize())) {

          ComponentPin<C> componentPin;
          ComponentInstance<C> componentInstance;

          backingMap.put(componentInstance = componentPool.getComponentInstanceFactory().createInstance(componentPool), componentPin = new ComponentPin<C>(componentPool, deconstructionQueue, componentInstance));
          freeQueue.put(componentPin);
        }

        size.set(backingMap.size());
        stateRef.set(State.STARTED);

        trackSize();
      } catch (Exception exception) {
        freeQueue.clear();
        backingMap.clear();
        size.set(0);
        stateRef.set(State.STOPPED);

        throw new ComponentPoolException(exception);
      } finally {
        backingLock.writeLock().unlock();
      }
    } else {
      try {
        while (State.STARTING.equals(stateRef.get())) {
          Thread.sleep(100);
        }
      } catch (InterruptedException interruptedException) {
        throw new ComponentPoolException(interruptedException);
      }
    }
  }

  public ComponentPin<C> serve ()
    throws ComponentPoolException {

    if (!State.STARTED.equals(stateRef.get())) {
      throw new ComponentPoolException("%s is not in the 'started' state", ComponentPool.class.getSimpleName());
    }

    ComponentPin<C> componentPin;

    while ((componentPin = freeQueue.poll()) != null) {
      if (componentPool.getComplexPoolConfig().isTestOnAcquire() && (!componentPin.getComponentInstance().validate())) {
        remove(componentPin, true);
      } else {
        trackSize();

        return componentPin;
      }
    }

    if ((componentPin = addComponentPin(true)) != null) {
      trackSize();

      return componentPin;
    }

    try {

      long acquireWaitTimeMillis = componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis();
      long start = System.currentTimeMillis();

      while ((acquireWaitTimeMillis > 0) && (componentPin = freeQueue.poll(acquireWaitTimeMillis, TimeUnit.MILLISECONDS)) != null) {
        if (componentPool.getComplexPoolConfig().isTestOnAcquire() && (!componentPin.getComponentInstance().validate())) {
          remove(componentPin, true);
          acquireWaitTimeMillis = componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis() - (System.currentTimeMillis() - start);
        } else {
          trackSize();

          return componentPin;
        }
      }
    } catch (InterruptedException interruptedException) {
      throw new ComponentPoolException(interruptedException);
    }

    trackTimeout();
    throw new ComponentPoolException("Exceeded the maximum acquire wait time(%d)", componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis());
  }

  private ComponentPin<C> addComponentPin (boolean forced)
    throws ComponentCreationException, ComponentValidationException {

    if (State.STARTED.equals(stateRef.get())) {

      int minPoolSize = componentPool.getComplexPoolConfig().getMinPoolSize();
      int maxPoolSize = componentPool.getComplexPoolConfig().getMaxPoolSize();
      int currentSize = size.get();

      if ((forced || (currentSize < minPoolSize)) && ((maxPoolSize == 0) || (currentSize < maxPoolSize))) {
        backingLock.writeLock().lock();
        try {
          currentSize = size.get();

          if ((forced || (currentSize < minPoolSize)) && ((maxPoolSize == 0) || (currentSize < maxPoolSize))) {

            ComponentPin<C> componentPin;
            ComponentInstance<C> componentInstance;

            backingMap.put(componentInstance = manufactureComponentInstance(), componentPin = new ComponentPin<>(componentPool, deconstructionQueue, componentInstance));
            size.incrementAndGet();

            return componentPin;
          }
        } finally {
          backingLock.writeLock().unlock();
        }
      }
    }

    return null;
  }

  private ComponentInstance<C> manufactureComponentInstance ()
    throws ComponentCreationException, ComponentValidationException {

    ComponentInstance<C> componentInstance;

    try {
      if (componentPool.getComplexPoolConfig().getCreationTimeoutMillis() > 0) {

        ComponentCreationWorker<C> creationWorker;
        Thread workerThread;

        creationWorker = new ComponentCreationWorker<>(componentPool);
        workerThread = new Thread(creationWorker);
        workerThread.setDaemon(true);
        workerThread.start();

        workerThread.join(componentPool.getComplexPoolConfig().getCreationTimeoutMillis());
        if (creationWorker.abort()) {
          throw new ComponentCreationException("Exceeded timeout(%d) waiting on element creation (pool size = %d, free size = %d)", componentPool.getComplexPoolConfig().getCreationTimeoutMillis(), getPoolSize(), getFreeSize());
        } else {
          componentInstance = creationWorker.getComponentInstance();
        }
      } else {
        componentInstance = componentPool.getComponentInstanceFactory().createInstance(componentPool);
      }
    } catch (ComponentCreationException componentCreationException) {
      throw componentCreationException;
    } catch (Exception exception) {
      throw new ComponentCreationException(exception);
    }

    if (componentPool.getComplexPoolConfig().isTestOnCreate() && (!componentInstance.validate())) {
      throw new ComponentValidationException("A new element was required, but failed to validate");
    }

    return componentInstance;
  }

  public void remove (ComponentPin<C> componentPin, boolean withPrejudice) {

    if (freeQueue.remove(componentPin) || withPrejudice) {
      terminate(componentPin.getComponentInstance());
      trackSize();
    }
  }

  public void process (ComponentInstance<C> componentInstance) {

    ComponentPin<C> componentPin;

    backingLock.readLock().lock();
    try {
      componentPin = backingMap.get(componentInstance);
    } finally {
      backingLock.readLock().unlock();
    }

    if (componentPin != null) {
      componentPin.free();

      if (componentPin.isTerminated()) {
        terminate(componentPin.getComponentInstance());
      } else {
        if (State.STARTED.equals(stateRef.get())) {
          try {
            freeQueue.put(componentPin);
          } catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(ComponentPinManager.class).error(interruptedException);
          }
        }
      }

      trackSize();
    }
  }

  public void terminate (ComponentInstance<C> componentInstance) {

    ComponentPin<C> componentPin;

    backingLock.writeLock().lock();
    try {
      componentPin = backingMap.remove(componentInstance);
    } finally {
      backingLock.writeLock().unlock();
    }

    if (componentPin != null) {
      size.decrementAndGet();
      componentPin.fizzle();

      try {
        componentPin.getComponentInstance().close();
      } catch (Exception exception) {
        LoggerManager.getLogger(ComponentPinManager.class).error(exception);
      }

      try {

        ComponentPin<C> replacementComponentPin;

        if ((replacementComponentPin = addComponentPin(false)) != null) {
          freeQueue.put(replacementComponentPin);
        }
      } catch (Exception exception) {
        LoggerManager.getLogger(ComponentPinManager.class).error(exception);
      }

      trackSize();
    }
  }

  public void shutdown ()
    throws ComponentPoolException {

    if (stateRef.compareAndSet(State.STARTED, State.STOPPING)) {

      while (size.get() > 0) {

        ComponentInstance<C>[] activeComponents;

        backingLock.readLock().lock();
        try {

          Set<ComponentInstance<C>> keys;

          keys = backingMap.keySet();
          activeComponents = new ComponentInstance[keys.size()];
          keys.toArray(activeComponents);
        } finally {
          backingLock.readLock().unlock();
        }

        for (ComponentInstance<C> activeComponent : activeComponents) {
          terminate(activeComponent);
        }
      }

      freeQueue.clear();

      try {
        deconstructionQueue.shutdown();
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ComponentPinManager.class).error(interruptedException);
      }

      stateRef.set(State.STOPPED);
    } else {
      try {
        while (State.STOPPING.equals(stateRef.get())) {
          Thread.sleep(100);
        }
      } catch (InterruptedException interruptedException) {
        throw new ComponentPoolException(interruptedException);
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

  private void trackSize () {

    int freeSize;

    InstrumentationManager.instrumentWithSpeedometer(PoolManager.getPool(), freeSize = freeQueue.size(), new MetricProperty("pool", componentPool.getPoolName()), new MetricProperty("size", MetricSize.FREE.getDisplay()));
    InstrumentationManager.instrumentWithSpeedometer(PoolManager.getPool(), getPoolSize() - freeSize, new MetricProperty("pool", componentPool.getPoolName()), new MetricProperty("size", MetricSize.PROCESSING.getDisplay()));
  }

  private void trackTimeout () {

    InstrumentationManager.instrumentWithGauge(PoolManager.getPool(), new MetricProperty("pool", componentPool.getPoolName()), new MetricProperty("pool", componentPool.getPoolName()), new MetricProperty("event", MetricInteraction.TIMEOUT.getDisplay()));
  }

  public StackTrace[] getExistentialStackTraces () {

    LinkedList<StackTrace> stackTraceList = new LinkedList<StackTrace>();
    StackTrace[] stackTraces;

    backingLock.readLock().lock();
    try {
      for (ComponentPin<C> componentPin : backingMap.values()) {
        if (!freeQueue.contains(componentPin)) {
          stackTraceList.add(new StackTrace(componentPin.getExistentialStackTrace()));
        }
      }
    } finally {
      backingLock.readLock().unlock();
    }

    stackTraces = new StackTrace[stackTraceList.size()];
    stackTraceList.toArray(stackTraces);

    return stackTraces;
  }
}
