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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Helper runnable that creates a component instance on a worker thread while allowing timeout aborts.
 *
 * @param <C> component type
 */
public class ComponentCreationWorker<C> implements Runnable {

  private enum State {COMPLETED, ABORTED, TERMINATED}

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final ComponentPool<C> componentPool;
  private final AtomicReference<State> stateRef = new AtomicReference<State>();

  private ComponentInstance<C> componentInstance;
  private Exception exception;

  /**
   * Creates a worker tied to the provided pool.
   *
   * @param componentPool owning pool
   */
  public ComponentCreationWorker (ComponentPool<C> componentPool) {

    this.componentPool = componentPool;
  }

  /**
   * Returns the component instance created by the worker, or {@code null} if none.
   *
   * @return created component instance
   */
  public ComponentInstance<C> getComponentInstance () {

    return componentInstance;
  }

  /**
   * Requests abortion of the creation process. If work already finished and failed, the cause is thrown.
   *
   * @return {@code true} if the creation was aborted before completion, {@code false} otherwise
   * @throws Exception if the worker terminated with an exception before aborting
   */
  public boolean abort ()
    throws Exception {

    if (!stateRef.compareAndSet(null, State.ABORTED)) {
      terminationLatch.await();

      if (State.TERMINATED.equals(stateRef.get())) {
        throw exception;
      }

      return false;
    }

    return true;
  }

  /**
   * Performs creation using the pool's factory and records completion/abort state.
   */
  public void run () {

    try {

      long startMilliseconds = System.currentTimeMillis();
      long totalMilliseconds;

      componentInstance = componentPool.getComponentInstanceFactory().createInstance(componentPool);
      if ((!stateRef.compareAndSet(null, State.COMPLETED)) && (componentInstance != null)) {
        totalMilliseconds = System.currentTimeMillis() - startMilliseconds;
        LoggerManager.getLogger(ComponentCreationWorker.class).error("Completed connection(%d ms) is being closed due to a request in the %s state - you may want to increase the connection wait time", totalMilliseconds, stateRef.get().name());
        componentInstance.close();
      }
    } catch (Exception exception) {
      if (!stateRef.compareAndSet(null, State.TERMINATED)) {
        LoggerManager.getLogger(ComponentCreationWorker.class).error(exception);
      } else {
        this.exception = exception;
      }
    } finally {
      terminationLatch.countDown();
    }
  }
}
