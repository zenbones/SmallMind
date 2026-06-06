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
import java.util.concurrent.TimeoutException;
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
 * Core lifecycle engine for the complex component pool.
 * <p>
 * Maintains two complementary data structures:
 * <ul>
 *   <li>A {@link HashMap} ({@code backingMap}) keyed by {@link ComponentInstance} that maps
 *       each instance to its {@link ComponentPin}. All structural mutations are serialized
 *       through a {@link ReentrantReadWriteLock}.</li>
 *   <li>A {@link LinkedBlockingQueue} ({@code freeQueue}) of {@link ComponentPin}s that are
 *       idle and available for acquisition.</li>
 * </ul>
 * The manager is also responsible for:
 * <ul>
 *   <li>Enforcing the minimum and maximum pool sizes.</li>
 *   <li>Optionally validating components on acquire ({@link ComplexPoolConfig#isTestOnAcquire()})
 *       and on creation ({@link ComplexPoolConfig#isTestOnCreate()}).</li>
 *   <li>Applying a creation timeout via a {@link ComponentCreationWorker} virtual thread when
 *       {@link ComplexPoolConfig#getCreationTimeoutMillis()} is positive.</li>
 *   <li>Replacing terminated components up to the minimum pool size.</li>
 *   <li>Emitting Claxon metrics for free size, processing size, and acquisition timeouts.</li>
 * </ul>
 *
 * @param <C> the type of component managed by the enclosing pool
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
   * @param componentPool the pool this manager serves
   */
  public ComponentPinManager (ComponentPool<C> componentPool) {

    this.componentPool = componentPool;
  }

  /**
   * Starts the manager: launches the {@link DeconstructionQueue} background worker and
   * pre-populates the pool with {@code max(minPoolSize, initialPoolSize)} components.
   * <p>
   * If another thread is concurrently starting the manager, this method polls until that
   * thread completes.
   *
   * @throws ComponentPoolException if any component cannot be created during startup or if
   *                                the startup wait is interrupted
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
   * Returns a {@link ComponentPin} to a caller, creating a new one if the free queue is
   * empty and the pool is below its maximum size, or blocking up to the configured acquire
   * wait time if the pool is full.
   * <p>
   * When {@link ComplexPoolConfig#isTestOnAcquire()} is enabled, a pin that fails validation
   * is discarded and the method continues searching. The acquire wait time is adjusted on
   * each retry to honour the original total budget.
   *
   * @return a {@link ComponentPin} whose component has passed any configured validation
   * @throws ComponentPoolException if the manager is not started, if the wait is interrupted,
   *                                or if the maximum wait time is exceeded without a component
   *                                becoming available
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
   * Attempts to create and register a new {@link ComponentPin} when the pool has capacity.
   * <p>
   * A write lock is acquired before the capacity check is re-evaluated to guard against
   * races. When {@code forced} is {@code false} creation is skipped if the pool is already
   * at or above {@code minPoolSize}.
   *
   * @param forced {@code true} to force creation regardless of the minimum-pool-size check
   * @return the newly created pin, or {@code null} if the pool is at maximum capacity
   * @throws ComponentCreationException   if the factory throws, times out, or is aborted
   * @throws ComponentValidationException if {@code testOnCreate} is enabled and the new
   *                                      instance fails validation
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
   * Invokes the {@link ComponentInstanceFactory} to create a new {@link ComponentInstance},
   * honouring the optional creation timeout and performing a post-creation validation check
   * when configured.
   * <p>
   * When a creation timeout is configured, the factory call is delegated to a
   * {@link ComponentCreationWorker} virtual thread. The calling thread joins for the timeout
   * duration; if the worker has not yet finished, {@link ComponentCreationWorker#abort()} is
   * called, which either aborts the in-progress creation (returning {@code true}) or retrieves
   * the worker's exception.
   *
   * @return the newly constructed and validated {@link ComponentInstance}
   * @throws ComponentCreationException   if the factory throws, the creation times out, or
   *                                      the creation thread is interrupted
   * @throws ComponentValidationException if {@code testOnCreate} is enabled and the new
   *                                      instance fails {@link ComponentInstance#validate()}
   */
  private ComponentInstance<C> manufactureComponentInstance ()
    throws ComponentCreationException, ComponentValidationException {

    ComponentInstance<C> componentInstance;

    try {
      if (componentPool.getComplexPoolConfig().getCreationTimeoutMillis() > 0) {

        ComponentCreationWorker<C> creationWorker;
        Thread workerThread;

        creationWorker = new ComponentCreationWorker<>(componentPool);
        workerThread = Thread.ofVirtual().name("quorum-component-creator-" + componentPool.getPoolName()).start(creationWorker);

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
   * Removes a pin from the pool and terminates the underlying component, optionally using
   * prejudice (forced termination) and optionally emitting metrics afterward.
   * <p>
   * The removal succeeds if the pin was already taken off the free queue
   * ({@code alreadyAcquired}), if it can be polled off now, or if {@code withPrejudice} is
   * {@code true} (even if the pin is not on the free queue — used to terminate a pin that
   * is currently processing).
   *
   * @param componentPin    the pin to remove
   * @param alreadyAcquired {@code true} if the caller has already removed the pin from the
   *                        free queue
   * @param withPrejudice   {@code true} to force removal even when the pin is not on the
   *                        free queue
   * @param track           {@code true} to update Claxon size metrics after removal
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
   * Terminates every component that is currently in the processing state (checked out by a
   * caller), then updates metrics.
   * <p>
   * Useful when a prejudicial processing-time fuse is not used but the caller still wants to
   * forcibly reclaim all checked-out components.
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
   * Processes the return of a {@link ComponentInstance} to the pool.
   * <p>
   * Notifies the pin that it has been freed (updating lease metrics and deconstruction timers).
   * If the pin was marked terminated while it was out, the instance is closed; otherwise it is
   * placed back on the free queue for future acquisition. Metric tracking is controlled by
   * {@code track}.
   *
   * @param componentInstance the instance being returned
   * @param track             {@code true} to update Claxon size metrics
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
   * Permanently removes a {@link ComponentInstance} from the pool, closes it, and optionally
   * creates a replacement to maintain the minimum pool size.
   * <p>
   * Acquires the write lock only to update the backing map; the close call and optional
   * replacement happen outside the lock to minimise contention. Logs any exceptions thrown
   * during close or replacement.
   *
   * @param componentInstance the instance to terminate
   * @param allowReplacement  {@code true} to attempt creating a replacement if the pool is
   *                          below its minimum size after removal
   * @param track             {@code true} to update Claxon size metrics after removal
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
   * Stops the manager: terminates all managed component instances, clears the free queue,
   * and shuts down the {@link DeconstructionQueue} background worker.
   * <p>
   * If another thread is concurrently stopping the manager, this method polls until that
   * thread completes.
   *
   * @throws ComponentPoolException if the wait for a concurrent shutdown is interrupted
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
      } catch (InterruptedException | TimeoutException exception) {
        LoggerManager.getLogger(ComponentPinManager.class).error(exception);
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
   * Returns the total number of component instances currently managed by the pool, including
   * both free (idle) and processing (checked out) instances.
   *
   * @return the total pool size
   */
  public int getPoolSize () {

    return size.get();
  }

  /**
   * Returns the number of instances currently on the free queue, available for immediate
   * acquisition without blocking or creation.
   *
   * @return the number of idle components
   */
  public int getFreeSize () {

    return freeQueue.size();
  }

  /**
   * Returns the number of instances currently checked out by callers (total pool size minus
   * free size).
   *
   * @return the number of components in the processing state
   */
  public int getProcessingSize () {

    return getPoolSize() - getFreeSize();
  }

  /**
   * Updates the Claxon free-size and processing-size speedometer metrics.
   */
  private void trackSize () {

    int freeSize;

    Instrument.with(ComponentPinManager.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("pool", componentPool.getPoolName()), new Tag("size", ClaxonTag.FREE.getDisplay())).update(freeSize = getFreeSize());
    Instrument.with(ComponentPinManager.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("pool", componentPool.getPoolName()), new Tag("size", ClaxonTag.PROCESSING.getDisplay())).update(getPoolSize() - freeSize);
  }

  /**
   * Increments the Claxon timeout counter metric when an acquisition attempt exceeds the
   * configured wait limit.
   */
  private void trackTimeout () {

    Instrument.with(ComponentPinManager.class, new TachometerBuilder(), new Tag("pool", componentPool.getPoolName()), new Tag("event", ClaxonTag.TIMEOUT.getDisplay())).update(1);
  }

  /**
   * Returns the existential stack traces of all components that are currently in the
   * processing state, when existential awareness is enabled.
   * <p>
   * Pins whose components are on the free queue are excluded. Pins with no recorded stack
   * trace (existential awareness disabled) contribute no entry.
   *
   * @return an array of {@link StackTrace} objects for each checked-out component that has
   * a recorded acquisition trace; never {@code null} but may be empty
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
