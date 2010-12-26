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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.dom4j.IllegalAddException;

public class ConnectionPinManager<C> {

   private final ReentrantReadWriteLock readWriteLock;

   private ConnectionPool<C> connectionPool;
   private ConnectionInstanceFactory<C> connectionFactory;
   private ConnectionPin[] connectionPins;
   private LinkedBlockingQueue<Integer> freeQueue;
   private ConcurrentLinkedQueue<Integer> emptyQueue;
   private AtomicInteger freeCount = new AtomicInteger(0);
   private AtomicInteger emptyCount;
   private int maxSize;

   public ConnectionPinManager (ConnectionPool<C> connectionPool, ConnectionInstanceFactory<C> connectionFactory, int maxSize) {

      if (maxSize < 0) {
         throw new IllegalAddException("size must be >= 0");
      }

      this.connectionPool = connectionPool;
      this.connectionFactory = connectionFactory;
      this.maxSize = maxSize;

      readWriteLock = (maxSize == 0) ? new ReentrantReadWriteLock() : null;

      connectionPins = new ConnectionPin[maxSize];
      freeQueue = new LinkedBlockingQueue<Integer>();

      emptyQueue = new ConcurrentLinkedQueue<Integer>();
      emptyCount = new AtomicInteger(connectionPins.length);
      for (int index = 0; index < connectionPins.length; index++) {
         emptyQueue.add(index);
      }
   }

   public void initialize (ConnectionPin<C> connectionPin) {

      freeQueue.add(connectionPin.getOriginatingIndex());
      freeCount.incrementAndGet();
   }

   public ConnectionPin<C> create (long connectionTimeoutMillis, boolean testOnConnect, boolean reportLeaseTimeNanos, int maxIdleTimeSeconds, int maxLeaseTimeSeconds, int unreturnedConnectionTimeoutSeconds)
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
         connectionInstance = createConnection(index, connectionTimeoutMillis, testOnConnect);
      }
      catch (Exception exception) {
         emptyQueue.add(index);

         throw exception;
      }

      if (readWriteLock != null) {
         readWriteLock.readLock().lock();
      }

      try {
         connectionPins[index] = connectionPin = new ConnectionPin<C>(connectionPool, index, connectionInstance, reportLeaseTimeNanos, maxIdleTimeSeconds, maxLeaseTimeSeconds, unreturnedConnectionTimeoutSeconds);
      }
      finally {
         if (readWriteLock != null) {
            readWriteLock.readLock().unlock();
         }
      }

      emptyCount.decrementAndGet();

      return connectionPin;
   }

   private ConnectionInstance<C> createConnection (Integer originatingIndex, long connectionTimeoutMillis, boolean testOnConnect)
      throws Exception {

      ConnectionInstance<C> connectionInstance;

      if (connectionTimeoutMillis > 0) {

         ConnectionWorker<C> connectionWorker;
         Thread workerThread;
         CountDownLatch workerInitLatch = new CountDownLatch(1);

         connectionWorker = new ConnectionWorker<C>(connectionPool, connectionFactory, originatingIndex, workerInitLatch);
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
         connectionInstance = connectionFactory.createInstance(connectionPool, originatingIndex);
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
         synchronized (connectionPin) {
            connectionPin.close();
         }
      }
      finally {
         if (readWriteLock != null) {
            readWriteLock.readLock().unlock();
         }

         synchronized (connectionPin) {
            connectionPin.abort();
         }

         emptyQueue.add(connectionPin.getOriginatingIndex());
         currentlyEmpty = emptyCount.incrementAndGet();

         if (regenerate && ((connectionPins.length - currentlyEmpty) < connectionPool.getMinPoolSize())) {
            connectionPool.createConnectionPin();
         }
      }
   }

   public ConnectionPin<C> serve (long acquireWaitTimeMillis, boolean immediate)
      throws InterruptedException {

      Integer index;

      if ((index = (immediate || (maxSize == 0)) ? freeQueue.poll() : (acquireWaitTimeMillis == 0) ? freeQueue.take() : freeQueue.poll(acquireWaitTimeMillis, TimeUnit.MILLISECONDS)) == null) {

         return null;
      }

      freeCount.decrementAndGet();

      if (readWriteLock != null) {
         readWriteLock.readLock().lock();
      }

      try {

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

      ConnectionPin<C> connectionPin;

      if (readWriteLock != null) {
         readWriteLock.readLock().lock();
      }

      try {
         if (!(connectionPin = connectionPins[connectionInstance.getOriginatingIndex()]).contains(connectionInstance)) {
            throw new ConnectionPoolException("Could not find connection (%s) within ConnectionPool (%s)", connectionInstance, connectionPool.getPoolName());
         }
      }
      finally {
         if (readWriteLock != null) {
            readWriteLock.readLock().unlock();
         }
      }

      if (terminate || ((connectionPins.length - emptyCount.get()) > connectionPool.getMinPoolSize())) {
         remove(connectionPin, false);
      }
      else {
         synchronized (connectionPin) {
            if (connectionPin.isServed()) {
               connectionPin.free();

               freeQueue.add(connectionPin.getOriginatingIndex());
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
