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
 * Offloads component creation onto a dedicated thread so that the caller can enforce a
 * timeout via {@link #abort()}.
 * <p>
 * Usage pattern: construct the worker, start it on a daemon thread, join that thread for the
 * desired timeout duration, then call {@link #abort()}. If the worker finished before the
 * timeout, {@code abort()} returns {@code false} and {@link #getComponentInstance()} returns
 * the result. If the creation was still in progress, {@code abort()} returns {@code true} and
 * the worker, once it finishes, will close any instance it managed to create and log a warning.
 * <p>
 * A three-value state machine guards the transition between completion states:
 * {@code null} (running), {@code COMPLETED}, {@code ABORTED} (caller timed out), and
 * {@code TERMINATED} (factory threw).
 *
 * @param <C> the type of component being created
 */
public class ComponentCreationWorker<C> implements Runnable {

  private enum State {COMPLETED, ABORTED, TERMINATED}

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final ComponentPool<C> componentPool;
  private final AtomicReference<State> stateRef = new AtomicReference<State>();

  private ComponentInstance<C> componentInstance;
  private Exception exception;

  /**
   * Creates a worker that will call the factory belonging to {@code componentPool}.
   *
   * @param componentPool the pool whose {@link ComponentInstanceFactory} will be invoked
   */
  public ComponentCreationWorker (ComponentPool<C> componentPool) {

    this.componentPool = componentPool;
  }

  /**
   * Returns the component instance created by this worker, or {@code null} if creation has
   * not yet finished or if it failed.
   *
   * @return the created {@link ComponentInstance}, or {@code null}
   */
  public ComponentInstance<C> getComponentInstance () {

    return componentInstance;
  }

  /**
   * Attempts to abort this worker before it delivers a result.
   * <p>
   * If the worker has not yet completed, this method transitions its state to
   * {@code ABORTED} and returns {@code true} immediately. Any instance the factory
   * subsequently produces will be closed and a warning will be logged.
   * <p>
   * If the worker has already finished (successfully or with an error) this method waits
   * for the worker thread to finish, then returns {@code false}. If the worker terminated
   * with an exception, that exception is re-thrown so the caller can propagate it.
   *
   * @return {@code true} if the abort pre-empted a successful completion; {@code false} if
   * the worker had already finished when this method was called
   * @throws Exception the exception thrown by the factory if the worker terminated with one
   *                   before the abort attempt
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
   * Invokes the pool's factory to create a component instance.
   * <p>
   * On successful creation the state is set to {@code COMPLETED}. If an
   * {@code ABORTED} signal arrived first, the newly created instance is closed
   * immediately and a warning is logged.
   * <p>
   * On factory failure the state is set to {@code TERMINATED} and the exception
   * is stored for retrieval by a subsequent call to {@link #abort()}, unless the
   * abort already occurred (in which case the exception is only logged).
   */
  @Override
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
