/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.scribe.pen.LoggerManager;

public class ConnectionWorker<C> implements Runnable {

  private static enum State {COMPLETED, ABORTED, TERMINATED}

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final ConnectionPool<C> connectionPool;
  private final AtomicReference<State> stateRef = new AtomicReference<State>();

  private ConnectionInstance<C> connectionInstance;
  private Exception exception;

  public ConnectionWorker (ConnectionPool<C> connectionPool) {

    this.connectionPool = connectionPool;
  }

  public ConnectionInstance<C> getConnectionInstance () {

    return connectionInstance;
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
      connectionInstance = connectionPool.getConnectionInstanceFactory().createInstance(connectionPool);
      if ((!stateRef.compareAndSet(null, State.COMPLETED)) && (connectionInstance != null)) {
        connectionInstance.close();
      }
    }
    catch (Exception exception) {
      if (!stateRef.compareAndSet(null, State.TERMINATED)) {
        LoggerManager.getLogger(ConnectionWorker.class).error(exception);
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