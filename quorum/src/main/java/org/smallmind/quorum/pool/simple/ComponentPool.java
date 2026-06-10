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
package org.smallmind.quorum.pool.simple;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.Pool;

/**
 * A bounded, synchronized object pool backed by two linked lists — one for in-use components
 * and one for free components.
 * <p>
 * When a component is requested and the free list is empty, the pool either creates a new
 * instance (if the total pool size is below {@link SimplePoolConfig#getMaxPoolSize()}, or if
 * the pool is unbounded), or it blocks the caller for up to
 * {@link SimplePoolConfig#getAcquireWaitTimeMillis()} milliseconds waiting for a component to
 * be returned. A wait time of {@code 0} causes the pool to throw immediately when no free
 * component is available and the size cap has been reached.
 * <p>
 * When a component is returned and the combined free+used count already equals the size cap,
 * the returned component is terminated rather than re-pooled, allowing the pool to shrink
 * after its configuration has been tightened at runtime.
 * <p>
 * {@link #close()} marks the pool closed, wakes all blocked waiters, waits for every
 * borrowed component to be returned, terminates everything on the free list, and then
 * returns. Concurrent callers of {@code close()} all block on an exit latch and return
 * together once the single winner completes the shutdown.
 *
 * @param <T> the type of {@link PooledComponent} managed by this pool
 */
public class ComponentPool<T extends PooledComponent> extends Pool {

  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ComponentFactory<T> componentFactory;
  private final LinkedList<T> usedList;
  private final LinkedList<T> freeList;

  private SimplePoolConfig simplePoolConfig = new SimplePoolConfig();

  /**
   * Creates a pool that uses default configuration values (max pool size 10, no acquire wait).
   *
   * @param componentFactory the factory used to construct new component instances when the free
   *                         list is empty and the pool has not yet reached its size cap
   */
  public ComponentPool (ComponentFactory<T> componentFactory) {

    this.componentFactory = componentFactory;

    usedList = new LinkedList<T>();
    freeList = new LinkedList<T>();
  }

  /**
   * Creates a pool with an explicit configuration.
   *
   * @param componentFactory the factory used to construct new component instances when the free
   *                         list is empty and the pool has not yet reached its size cap
   * @param simplePoolConfig configuration controlling the size cap and acquire wait time
   */
  public ComponentPool (ComponentFactory<T> componentFactory, SimplePoolConfig simplePoolConfig) {

    this(componentFactory);

    this.simplePoolConfig = simplePoolConfig;
  }

  /**
   * Acquires a component from the pool.
   * <p>
   * The following acquisition strategy is used in order:
   * <ol>
   *   <li>If the free list is non-empty, the first free component is returned immediately.</li>
   *   <li>If the free list is empty and the pool is below its size cap (or unbounded), a new
   *       component is created via the factory.</li>
   *   <li>If the free list is empty and the size cap has been reached, the caller blocks for up
   *       to {@link SimplePoolConfig#getAcquireWaitTimeMillis()} milliseconds; a wait time of
   *       {@code 0} does not block and fails immediately. If a free component becomes available
   *       within the window, it is returned, otherwise a {@link ComponentPoolException} is thrown.</li>
   * </ol>
   *
   * @return a component instance ready for use; the caller must eventually pass it to
   * {@link #returnComponent(PooledComponent)} when finished
   * @throws ComponentPoolException if the pool has been closed, if the factory throws while
   *                                creating a new component, if the acquire wait is interrupted,
   *                                or if the wait timeout expires before a component is returned
   */
  public synchronized T getComponent ()
    throws ComponentPoolException {

    if (closed.get()) {
      throw new ComponentPoolException("Pool has been closed");
    }

    T component = null;

    if (freeList.isEmpty()) {
      if ((simplePoolConfig.getMaxPoolSize() == 0) || (usedList.size() < simplePoolConfig.getMaxPoolSize())) {
        try {
          component = componentFactory.createComponent();
        } catch (Exception e) {
          throw new ComponentPoolException(e);
        }
      } else {

        long acquireWaitTimeMillis = simplePoolConfig.getAcquireWaitTimeMillis();

        // A zero acquire-wait means do not block: the pool is at capacity, so fail fast. Otherwise
        // block for the configured window and take a component if one is returned within it.
        if (acquireWaitTimeMillis == 0) {
          throw new ComponentPoolException("ComponentPool(%s) is completely booked", componentFactory.getClass().getSimpleName());
        } else {
          try {
            wait(acquireWaitTimeMillis);

            if (closed.get()) {
              throw new ComponentPoolException("Pool has been closed");
            } else if (freeList.isEmpty()) {
              throw new ComponentPoolException("ComponentPool(%s) is completely booked", componentFactory.getClass().getSimpleName());
            } else {
              component = freeList.removeFirst();
            }
          } catch (InterruptedException interruptedException) {
            throw new ComponentPoolException(interruptedException);
          }
        }
      }
    } else {
      component = freeList.removeFirst();
    }

    usedList.add(component);

    return component;
  }

  /**
   * Returns a previously acquired component to the pool.
   * <p>
   * If the combined count of in-use and free components is already at the configured size cap,
   * the component is terminated via {@link PooledComponent#terminate()} rather than re-pooled,
   * allowing the pool to shrink when the configuration has been tightened. Otherwise the
   * component is placed on the free list and a waiting thread (if any) is notified.
   * <p>
   * If the pool has been closed and this return empties the in-use list, the termination latch
   * is released so that {@link #close()} can proceed to drain the free list.
   *
   * @param component the component to return; must have been obtained from this pool via
   *                  {@link #getComponent()}
   */
  public synchronized void returnComponent (T component) {

    usedList.remove(component);

    if ((usedList.size() + freeList.size()) < simplePoolConfig.getMaxPoolSize()) {
      freeList.add(component);

      if (simplePoolConfig.getMaxPoolSize() > 0) {
        notify();
      }
    } else {
      component.terminate();
    }

    if (closed.get() && usedList.isEmpty()) {
      terminationLatch.countDown();
    }
  }

  /**
   * Returns the total number of components currently tracked by the pool, including both
   * in-use and free components.
   *
   * @return the sum of the in-use count and the free count
   */
  public synchronized int poolSize () {

    return freeList.size() + usedList.size();
  }

  /**
   * Returns the number of components currently sitting on the free list, available for
   * immediate acquisition without creation or blocking.
   *
   * @return the number of free (idle) components
   */
  public synchronized int freeSize () {

    return freeList.size();
  }

  /**
   * Closes the pool and waits for all borrowed components to be returned before terminating
   * the remaining free components.
   * <p>
   * The first thread to call this method marks the pool closed and wakes all threads blocked
   * in {@link #getComponent()} so they receive a {@link ComponentPoolException}. It then waits
   * for the in-use list to drain (each {@link #returnComponent} call counts down the
   * termination latch when the list becomes empty), terminates every component on the free
   * list, and finally counts down an exit latch so concurrent callers can return together.
   * <p>
   * Subsequent concurrent callers of {@code close()} bypass the shutdown logic and simply
   * block on the exit latch until the first caller completes the shutdown sequence.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for
   *                              borrowed components to be returned or for the exit latch
   */
  public void close ()
    throws InterruptedException {

    if (closed.compareAndSet(false, true)) {
      try {
        synchronized (this) {
          notifyAll();

          if (usedList.isEmpty()) {
            terminationLatch.countDown();
          }
        }

        terminationLatch.await();

        for (T component : freeList) {
          component.terminate();
        }
      } finally {
        exitLatch.countDown();
      }
    }

    exitLatch.await();
  }
}
