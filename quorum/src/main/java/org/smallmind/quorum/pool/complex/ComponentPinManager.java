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
package org.smallmind.quorum.pool.complex;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.claxon.registry.meter.TachometerBuilder;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Manages the lifecycle of {@link ComponentPin}s, including creation, validation, leasing,
 * and deconstruction scheduling for the complex pool.
 *
 * @param <C> component type managed
 */
public class ComponentPinManager<C> {

  private final ComponentPool<C> componentPool;
  private final HashMap<ComponentInstance<C>, ComponentPin<C>> backingMap = new HashMap<>();
  private final LinkedBlockingQueue<ComponentPin<C>> freeQueue = new LinkedBlockingQueue<>();
  private final ReentrantReadWriteLock backingLock = new ReentrantReadWriteLock();
  private final DeconstructionQueue deconstructionQueue = new DeconstructionQueue();
  private final AtomicReference<ComponentStatus> statusRef = new AtomicReference<>(ComponentStatus.STOPPED);
  private final AtomicInteger size = new AtomicInteger(0);

  /**
   * Creates a manager for the given pool.
   *
   * @param componentPool owning pool
   */
  public ComponentPinManager (ComponentPool<C> componentPool) {

    this.componentPool = componentPool;
  }

  /**
   * Starts the manager by creating initial pins and enabling deconstruction scheduling.
   *
   * @throws ComponentPoolException if initialization fails
   */
  public void startup ()
    throws ComponentPoolException {

    if (statusRef.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING)) {
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
        statusRef.set(ComponentStatus.STARTED);

        trackSize();
      } catch (Exception exception) {
        freeQueue.clear();
        backingMap.clear();
        size.set(0);
        statusRef.set(ComponentStatus.STOPPED);

        throw new ComponentPoolException(exception);
      } finally {
        backingLock.writeLock().unlock();
      }
    } else {
      try {
        while (ComponentStatus.STARTING.equals(statusRef.get())) {
          Thread.sleep(100);
        }
      } catch (InterruptedException interruptedException) {
        throw new ComponentPoolException(interruptedException);
      }
    }
  }

  /**
   * Serves a pin to a caller, validating on acquire and respecting size/wait constraints.
   *
   * @return a component pin ready for use
   * @throws ComponentPoolException if the pool is not started, validation fails, or waits timeout
   */
  public ComponentPin<C> serve ()
    throws ComponentPoolException {

    if (!ComponentStatus.STARTED.equals(statusRef.get())) {
      throw new ComponentPoolException("%s is not in the 'started' state", ComponentPool.class.getSimpleName());
    }

    try {

      ComponentPin<C> componentPin;

      while ((componentPin = freeQueue.poll()) != null) {
        if (componentPool.getComplexPoolConfig().isTestOnAcquire() && (!componentPin.getComponentInstance().validate())) {
          remove(componentPin, true, false, false);
        } else {

          return componentPin;
        }
      }

      if ((componentPin = addComponentPin(true)) != null) {

        return componentPin;
      }

      try {

        long acquireWaitTimeMillis = componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis();
        long start = System.currentTimeMillis();

        while ((acquireWaitTimeMillis > 0) && (componentPin = freeQueue.poll(acquireWaitTimeMillis, TimeUnit.MILLISECONDS)) != null) {
          if (componentPool.getComplexPoolConfig().isTestOnAcquire() && (!componentPin.getComponentInstance().validate())) {
            remove(componentPin, true, false, false);
            acquireWaitTimeMillis = componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis() - (System.currentTimeMillis() - start);
          } else {

            return componentPin;
          }
        }
      } catch (InterruptedException interruptedException) {
        throw new ComponentPoolException(interruptedException);
      }
    } finally {
      trackSize();
    }

    trackTimeout();
    throw new ComponentPoolException("Exceeded the maximum acquire wait time(%d)", componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis());
  }

  /**
   * Attempts to add a new pin, honoring min/max limits. May force creation.
   *
   * @param forced whether creation should bypass the minimum size check
   * @return newly created pin or {@code null} if limits prevent creation
   * @throws ComponentCreationException   if creation fails
   * @throws ComponentValidationException if validation fails
   */
  private ComponentPin<C> addComponentPin (boolean forced)
    throws ComponentCreationException, ComponentValidationException {

    if (ComponentStatus.STARTED.equals(statusRef.get())) {

      int minPoolSize = componentPool.getComplexPoolConfig().getMinPoolSize();
      int maxPoolSize = componentPool.getComplexPoolConfig().getMaxPoolSize();
      int currentSize = getPoolSize();

      if ((forced || (currentSize < minPoolSize)) && ((maxPoolSize == 0) || (currentSize < maxPoolSize))) {
        backingLock.writeLock().lock();
        try {
          currentSize = getPoolSize();

          if ((forced || (currentSize < minPoolSize)) && ((maxPoolSize == 0) || (currentSize < maxPoolSize))) {

            ComponentPin<C> componentPin;
            ComponentInstance<C> componentInstance;

            if (backingMap.put(componentInstance = manufactureComponentInstance(), componentPin = new ComponentPin<>(componentPool, deconstructionQueue, componentInstance)) == null) {
              size.incrementAndGet();
            }

            return componentPin;
          }
        } finally {
          backingLock.writeLock().unlock();
        }
      }
    }

    return null;
  }

  /**
   * Creates and validates a new component instance, optionally enforcing a timeout.
   *
   * @return new component instance
   * @throws ComponentCreationException   if creation fails or times out
   * @throws ComponentValidationException if validation fails
   */
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

  /**
   * Removes a pin from service, optionally terminating with prejudice and tracking metrics.
   *
   * @param componentPin    pin to remove
   * @param alreadyAcquired whether the pin has been removed from the free queue already
   * @param withPrejudice   whether to terminate regardless of queue state
   * @param track           whether to update metrics after removal
   */
  public void remove (ComponentPin<C> componentPin, boolean alreadyAcquired, boolean withPrejudice, boolean track) {

    // order here matters as alreadyAcquired means it's been removed from the queue, otherwise we try to remove,
    // otherwise we would like to terminate anyway because this component *is* going away in any case
    if (alreadyAcquired || freeQueue.remove(componentPin) || withPrejudice) {
      try {
        terminate(componentPin.getComponentInstance(), true, false);
      } finally {
        if (track) {
          trackSize();
        }
      }
    }
  }

  /**
   * Terminates all components currently processing (i.e., checked out).
   */
  public void killAllProcessing () {

    backingLock.writeLock().lock();
    try {
      for (Map.Entry<ComponentInstance<C>, ComponentPin<C>> backingEntry : backingMap.entrySet()) {
        if (!freeQueue.contains(backingEntry.getValue())) {
          terminate(backingEntry.getKey(), true, false);
        }
      }
    } finally {
      backingLock.writeLock().unlock();
    }

    trackSize();
  }

  /**
   * Processes the return of a component instance, re-queuing or terminating as necessary.
   *
   * @param componentInstance instance being returned
   * @param track             whether to update metrics
   */
  public void process (ComponentInstance<C> componentInstance, boolean track) {

    try {

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
          terminate(componentPin.getComponentInstance(), ComponentStatus.STARTED.equals(statusRef.get()), false);
        } else {
          if (ComponentStatus.STARTED.equals(statusRef.get())) {
            try {
              freeQueue.put(componentPin);
            } catch (InterruptedException interruptedException) {
              LoggerManager.getLogger(ComponentPinManager.class).error(interruptedException);
            }
          }
        }
      }
    } finally {
      if (track) {
        trackSize();
      }
    }
  }

  /**
   * Terminates a component instance and optionally replaces it with a new one.
   *
   * @param componentInstance instance to terminate
   * @param allowReplacement  whether to create a replacement
   * @param track             whether to update metrics
   */
  public void terminate (ComponentInstance<C> componentInstance, boolean allowReplacement, boolean track) {

    try {

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

        if (allowReplacement) {
          try {

            ComponentPin<C> replacementComponentPin;

            if ((replacementComponentPin = addComponentPin(false)) != null) {
              freeQueue.put(replacementComponentPin);
            }
          } catch (Exception exception) {
            LoggerManager.getLogger(ComponentPinManager.class).error(exception);
          }
        }
      }
    } finally {
      if (track) {
        trackSize();
      }
    }
  }

  /**
   * Stops the manager, terminating all components and shutting down deconstruction.
   *
   * @throws ComponentPoolException if shutdown is interrupted
   */
  public void shutdown ()
    throws ComponentPoolException {

    if (statusRef.compareAndSet(ComponentStatus.STARTED, ComponentStatus.STOPPING)) {

      while (getPoolSize() > 0) {

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
          terminate(activeComponent, false, false);
        }
      }

      freeQueue.clear();

      try {
        deconstructionQueue.shutdown();
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ComponentPinManager.class).error(interruptedException);
      }

      statusRef.set(ComponentStatus.STOPPED);
    } else {
      try {
        while (ComponentStatus.STOPPING.equals(statusRef.get())) {
          Thread.sleep(100);
        }
      } catch (InterruptedException interruptedException) {
        throw new ComponentPoolException(interruptedException);
      }
    }
  }

  /**
   * Current total pool size (free + processing).
   *
   * @return pool size
   */
  public int getPoolSize () {

    return size.get();
  }

  /**
   * Current number of free pins.
   *
   * @return number of available pins
   */
  public int getFreeSize () {

    return freeQueue.size();
  }

  /**
   * Current number of pins in processing state.
   *
   * @return processing count
   */
  public int getProcessingSize () {

    return getPoolSize() - getFreeSize();
  }

  /**
   * Updates Claxon metrics for pool size and processing counts.
   */
  private void trackSize () {

    int freeSize;

    Instrument.with(ComponentPinManager.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("pool", componentPool.getPoolName()), new Tag("size", ClaxonTag.FREE.getDisplay())).update(freeSize = getFreeSize());
    Instrument.with(ComponentPinManager.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("pool", componentPool.getPoolName()), new Tag("size", ClaxonTag.PROCESSING.getDisplay())).update(getPoolSize() - freeSize);
  }

  /**
   * Updates the timeout metric when acquisition waits are exceeded.
   */
  private void trackTimeout () {

    Instrument.with(ComponentPinManager.class, new TachometerBuilder(), new Tag("pool", componentPool.getPoolName()), new Tag("event", ClaxonTag.TIMEOUT.getDisplay())).update(1);
  }

  /**
   * Returns stack traces for components currently in processing state when existential tracking is enabled.
   *
   * @return array of stack traces for borrowed components
   */
  public StackTrace[] getExistentialStackTraces () {

    LinkedList<StackTrace> stackTraceList = new LinkedList<>();

    backingLock.readLock().lock();
    try {
      for (ComponentPin<C> componentPin : backingMap.values()) {
        if (!freeQueue.contains(componentPin)) {

          StackTraceElement[] stackTraceElements;

          if ((stackTraceElements = componentPin.getExistentialStackTrace()) != null) {
            stackTraceList.add(new StackTrace(stackTraceElements));
          }
        }
      }
    } finally {
      backingLock.readLock().unlock();
    }

    return stackTraceList.toArray(new StackTrace[0]);
  }
}
