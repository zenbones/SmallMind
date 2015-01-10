/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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

public class ComponentCreationWorker<C> implements Runnable {

  private static enum State {COMPLETED, ABORTED, TERMINATED}

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final ComponentPool<C> componentPool;
  private final AtomicReference<State> stateRef = new AtomicReference<State>();

  private ComponentInstance<C> componentInstance;
  private Exception exception;

  public ComponentCreationWorker (ComponentPool<C> componentPool) {

    this.componentPool = componentPool;
  }

  public ComponentInstance<C> getComponentInstance () {

    return componentInstance;
  }

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

  public void run () {

    try {
      componentInstance = componentPool.getComponentInstanceFactory().createInstance(componentPool);
      if ((!stateRef.compareAndSet(null, State.COMPLETED)) && (componentInstance != null)) {
        LoggerManager.getLogger(ComponentCreationWorker.class).error("Completed connection is being closed due to a request in the %s state - you may want to increase the connection wait time", stateRef.get().name());
        componentInstance.close();
      }
    }
    catch (Exception exception) {
      if (!stateRef.compareAndSet(null, State.TERMINATED)) {
        LoggerManager.getLogger(ComponentCreationWorker.class).error(exception);
      }
      else {
        this.exception = exception;
      }
    }
    finally {
      terminationLatch.countDown();
    }
  }
}