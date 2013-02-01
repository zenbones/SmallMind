/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.quorum.pool.simple;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.quorum.pool.ComponentPoolException;

public class ComponentPool<T extends PooledComponent> {

  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ComponentFactory<T> componentFactory;
  private final LinkedList<T> usedList;
  private final LinkedList<T> freeList;

  private SimplePoolConfig simplePoolConfig = new SimplePoolConfig();

  public ComponentPool (ComponentFactory<T> componentFactory) {

    this.componentFactory = componentFactory;

    usedList = new LinkedList<T>();
    freeList = new LinkedList<T>();
  }

  public ComponentPool (ComponentFactory<T> componentFactory, SimplePoolConfig simplePoolConfig) {

    this(componentFactory);

    this.simplePoolConfig = simplePoolConfig;
  }

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
        }
        catch (Exception e) {
          throw new ComponentPoolException(e);
        }
      }
      else {
        try {
          do {
            wait(simplePoolConfig.getAcquireWaitTimeMillis());

            if (closed.get()) {
              throw new ComponentPoolException("Pool has been closed");
            }
            else if (!freeList.isEmpty()) {
              component = freeList.removeFirst();
            }
            else if (simplePoolConfig.getAcquireWaitTimeMillis() > 0) {
              throw new ComponentPoolException("ComponentPool(%s) is completely booked", componentFactory.getClass().getSimpleName());
            }
          } while (component == null);
        }
        catch (InterruptedException i) {
          throw new ComponentPoolException(i);
        }
      }
    }
    else {
      component = freeList.removeFirst();
    }

    usedList.add(component);

    return component;
  }

  public synchronized void returnComponent (T component) {

    usedList.remove(component);

    if ((usedList.size() + freeList.size()) < simplePoolConfig.getMaxPoolSize()) {
      freeList.add(component);

      if (simplePoolConfig.getMaxPoolSize() > 0) {
        notify();
      }
    }
    else {
      component.terminate();
    }

    if (closed.get() && usedList.isEmpty()) {
      terminationLatch.countDown();
    }
  }

  public synchronized int poolSize () {

    return freeList.size() + usedList.size();
  }

  public synchronized int freeSize () {

    return freeList.size();
  }

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
      }
      finally {
        exitLatch.countDown();
      }
    }

    exitLatch.await();
  }
}
