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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.dom4j.IllegalAddException;

public class ConnectionPinManager<C> {

   private final ReentrantReadWriteLock readWriteLock;

   private ConnectionPool<C> connectionPool;
   private ConnectionInstanceFactory<C> connectionFactory;
   private ConnectionPin[] connectionPins;
   private ConcurrentLinkedQueue<Integer> freeQueue;
   private ConcurrentLinkedQueue<Integer> emptyQueue;
   private AtomicInteger freeCount = new AtomicInteger(0);
   private AtomicInteger emptyCount;

   public ConnectionPinManager (ConnectionPool<C> connectionPool, ConnectionInstanceFactory<C> connectionFactory, int maxSize, boolean unconstrained) {

      if (maxSize < 0) {
         throw new IllegalAddException("size must be >= 0");
      }

      this.connectionPool = connectionPool;
      this.connectionFactory = connectionFactory;

      readWriteLock = (unconstrained) ? new ReentrantReadWriteLock() : null;

      connectionPins = new ConnectionPin[maxSize];
      freeQueue = new ConcurrentLinkedQueue<Integer>();

      emptyQueue = new ConcurrentLinkedQueue<Integer>();
      emptyCount = new AtomicInteger(connectionPins.length);
      for (int index = 0; index < connectionPins.length; index++) {
         emptyQueue.add(index);
      }
   }

   public ConnectionPin<C> add (long connectionTimeoutMillis, boolean testOnConnect, boolean reportLeaseTimeNanos, int maxIdleTimeSeconds, int maxLeaseTimeSeconds, int unreturnedConnectionTimeoutSeconds)
      throws Exception {

      ConnectionPin<C> connectionPin;
      ConnectionInstance<C> connectionInstance;
      Integer index;

      while ((index = emptyQueue.poll()) == null) {

         if (readWriteLock != null) {
            readWriteLock.writeLock().lock();
            try {

               ConnectionPin[] expandedArray = new ConnectionPin[(connectionPins.length == 0) ? 10 : connectionPins.length * 2];

               System.arraycopy(connectionPins, 0, expandedArray, 0, connectionPins.length);
               connectionPins = expandedArray;
               for (int expandedIndex = connectionPins.length; expandedIndex < expandedArray.length; expandedIndex++) {
                  emptyQueue.add(expandedIndex);
               }
            }
            finally {
               readWriteLock.writeLock().unlock();
            }
         }
         else {

            return null;
         }
      }

      try {
         connectionInstance = createConnection(connectionTimeoutMillis, testOnConnect);
      }
      catch (Exception exception) {
         emptyQueue.add(index);

         throw exception;
      }

      if (readWriteLock != null) {
         readWriteLock.readLock().lock();
      }

      try {
         emptyCount.decrementAndGet();
         connectionPins[index] = connectionPin = new ConnectionPin<C>(connectionPool, index, connectionInstance, reportLeaseTimeNanos, maxIdleTimeSeconds, maxLeaseTimeSeconds, unreturnedConnectionTimeoutSeconds);
      }
      finally {
         if (readWriteLock != null) {
            readWriteLock.readLock().unlock();
         }
      }

      return connectionPin;
   }

   private ConnectionInstance<C> createConnection (long connectionTimeoutMillis, boolean testOnConnect)
      throws Exception {

      ConnectionInstance<C> connectionInstance;

      if (connectionTimeoutMillis > 0) {

         ConnectionWorker<C> connectionWorker;
         Thread workerThread;
         CountDownLatch workerInitLatch = new CountDownLatch(1);

         connectionWorker = new ConnectionWorker<C>(connectionPool, connectionFactory, workerInitLatch);
         workerThread = new Thread(connectionWorker);
         workerThread.start();

         workerInitLatch.await();
         workerThread.join(connectionTimeoutMillis);
         connectionWorker.abort();

         if (connectionWorker.hasBeenAborted()) {
            throw new ConnectionCreationException("Exceeded connection timeout(%d) waiting on connection creation", connectionTimeoutMillis);
         }
         else if (connectionWorker.hasException()) {
            throw connectionWorker.getException();
         }
         else {
            connectionInstance = connectionWorker.getConnectionInstance();
         }
      }
      else {
         connectionInstance = connectionFactory.createInstance(connectionPool);
      }

      if (testOnConnect && (!connectionInstance.validate())) {
         throw new InvalidConnectionException("A new connection was required, but failed to validate");
      }

      return connectionInstance;
   }

   public void remove (ConnectionPin<C> connectionPin, boolean regenerate)
      throws Exception {

      int currentlyEmpty;

      if (freeQueue.remove(connectionPin.getOriginatingIndex())) {
         freeCount.decrementAndGet();
      }

      if (readWriteLock != null) {
         readWriteLock.readLock().lock();
      }

      try {
         connectionPins[connectionPin.getOriginatingIndex()] = null;
         connectionPin.close();
      }
      finally {
         if (readWriteLock != null) {
            readWriteLock.readLock().unlock();
         }

         connectionPin.abort();

         emptyQueue.add(connectionPin.getOriginatingIndex());
         currentlyEmpty = emptyCount.incrementAndGet();
      }

      if (regenerate && ((connectionPins.length - currentlyEmpty) < connectionPool.getMinPoolSize())) {
         connectionPool.createConnectionPin();
      }
   }

   public ConnectionPin<C> serve () {

      Integer index;

      if ((index = freeQueue.poll()) == null) {

         return null;
      }

      if (readWriteLock != null) {
         readWriteLock.readLock().lock();
      }

      try {
         freeCount.decrementAndGet();

         return connectionPins[index];
      }
      finally {
         if (readWriteLock != null) {
            readWriteLock.readLock().unlock();
         }
      }
   }

   public void release (ConnectionInstance<C> connectionInstance, boolean terminate)
      throws Exception {

      ConnectionPin<C> matchedConnectionPin = null;

      if (readWriteLock != null) {
         readWriteLock.readLock().lock();
      }

      try {
         for (ConnectionPin<C> connectionPin : connectionPins) {
            if (connectionPin.contains(connectionInstance)) {
               matchedConnectionPin = connectionPin;
               break;
            }
         }
      }
      finally {
         if (readWriteLock != null) {
            readWriteLock.readLock().unlock();
         }
      }

      if (matchedConnectionPin == null) {
         throw new ConnectionPoolException("Could not find connection (%s) within ConnectionPool (%s)", connectionInstance, connectionPool.getPoolName());
      }

      if (terminate || ((connectionPins.length - emptyCount.get()) > connectionPool.getMinPoolSize())) {
         remove(matchedConnectionPin, false);
      }
      else {
         synchronized (matchedConnectionPin) {
            if (matchedConnectionPin.isServed()) {
               matchedConnectionPin.free();

               freeQueue.add(matchedConnectionPin.getOriginatingIndex());
               freeCount.incrementAndGet();
            }
         }
      }
   }

   public void shutdown ()
      throws Exception {

      freeQueue.clear();
      freeCount.set(0);

      if (readWriteLock != null) {
         readWriteLock.readLock().lock();
      }

      try {
         for (ConnectionPin<C> connectionPin : connectionPins) {
            if (connectionPin != null) {
               remove(connectionPin, false);
            }
         }
      }
      finally {
         if (readWriteLock != null) {
            readWriteLock.readLock().unlock();
         }
      }
   }

   public int getPoolSize () {

      return connectionPins.length - emptyCount.get();
   }

   public int getFreeSize () {

      return freeQueue.size();
   }

   public int getProcessingSize () {

      return getPoolSize() - getFreeSize();
   }
}
