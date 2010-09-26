/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.quorum.pool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionWorker<C> implements Runnable {

   private ConnectionPool<C> connectionPool;
   private ConnectionInstanceFactory<C> connectionFactory;
   private ConnectionInstance<C> connectionInstance;
   private ConnectionPoolException exception;
   private Thread runnableThread;
   private CountDownLatch exitLatch;
   private CountDownLatch workerInitLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private boolean aborted = false;

   public ConnectionWorker (ConnectionPool<C> connectionPool, ConnectionInstanceFactory<C> connectionFactory, CountDownLatch workerInitLatch) {

      this.connectionPool = connectionPool;
      this.connectionFactory = connectionFactory;
      this.workerInitLatch = workerInitLatch;

      exitLatch = new CountDownLatch(1);
   }

   public boolean hasBeenAborted () {

      return aborted;
   }

   public boolean hasException () {

      return exception != null;
   }

   public ConnectionPoolException getException () {

      return exception;
   }

   public ConnectionInstance<C> getConnectionInstance () {

      return connectionInstance;
   }

   public void abort ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
         runnableThread.interrupt();
      }

      exitLatch.await();
   }

   public void run () {

      runnableThread = Thread.currentThread();
      workerInitLatch.countDown();

      try {
         connectionInstance = connectionFactory.createInstance(connectionPool);
      }
      catch (InterruptedException interruptedException) {
         aborted = true;
      }
      catch (ConnectionPoolException connectionPoolException) {
         exception = connectionPoolException;
      }
      catch (Exception otherException) {
         exception = new ConnectionPoolException(otherException);
      }

      exitLatch.countDown();
   }
}