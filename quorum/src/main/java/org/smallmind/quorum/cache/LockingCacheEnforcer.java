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
package org.smallmind.quorum.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.terracotta.annotations.InstrumentedClass;

@InstrumentedClass
public abstract class LockingCacheEnforcer<K, V> implements LockingCache<K, V> {

  private static final ForcedUnlockTimer FORCED_UNLOCK_TIMER;

  private final ConcurrentHashMap<K, KeyCondition<K>> keyConditionMap;
  private final ReentrantLock[] stripeLocks;

  private long lockTimeout;

  static {

    Thread forcedUnlockTimerThread;

    FORCED_UNLOCK_TIMER = new ForcedUnlockTimer();
    forcedUnlockTimerThread = new Thread(FORCED_UNLOCK_TIMER);
    forcedUnlockTimerThread.setDaemon(true);
    forcedUnlockTimerThread.start();
  }

  public LockingCacheEnforcer (ReentrantLock[] stripeLocks, long lockTimeout)
    throws CacheException {

    if (stripeLocks.length % 2 != 0) {
      throw new CacheException("Concurrency level(%d) must be an even power of 2", stripeLocks.length);
    }

    this.stripeLocks = stripeLocks;
    this.lockTimeout = lockTimeout;

    keyConditionMap = new ConcurrentHashMap<K, KeyCondition<K>>(stripeLocks.length, .75F, stripeLocks.length);
  }

  public long getLockTimeout () {

    return lockTimeout;
  }

  public <R> R executeLockedCallback (KeyLock keyLock, LockedCallback<K, R> callback) {

    ReentrantLock stripeLock;

    stripeLock = lockStripe(callback.getKey());
    try {
      gateKey(keyLock, callback.getKey());

      return callback.execute();
    }
    finally {
      stripeLock.unlock();
    }
  }

  public KeyLock lock (KeyLock keyLock, K key) {

    ReentrantLock stripeLock;
    KeyCondition<K> keyCondition;

    stripeLock = lockStripe(key);
    try {
      if ((keyCondition = keyConditionMap.get(key)) == null) {
        keyConditionMap.put(key, keyCondition = new KeyCondition<K>(this, key, lockTimeout));

        return keyCondition.getOwningKeyLock();
      }
      else if ((keyLock != null) && keyCondition.isOwnedByKeyLock(keyLock)) {
        return keyCondition.inc();
      }
      else {
        while ((keyCondition = keyConditionMap.get(key)) != null) {
          try {
            keyCondition.getCondition().await();
          }
          catch (InterruptedException interruptedException) {
            throw new CacheLockException(interruptedException, "Interrupted while awaiting a lock opportunity on key(%s)", key.toString());
          }
        }

        keyConditionMap.put(key, keyCondition = new KeyCondition<K>(this, key, lockTimeout));

        return keyCondition.getOwningKeyLock();
      }
    }
    finally {
      stripeLock.unlock();
    }
  }

  public void unlock (KeyLock keyLock, K key) {

    ReentrantLock stripeLock;
    KeyCondition keyCondition;

    stripeLock = lockStripe(key);
    try {
      if ((keyCondition = keyConditionMap.get(key)) == null) {
        throw new CacheLockException("Attempt to unlock key(%s), but the lock has already expired - try increasing the lock time out", key.toString());
      }
      else if (!keyCondition.isOwnedByKeyLock(keyLock)) {
        throw new CacheLockException("Attempt to unlock key(%s) owned by lock(%s) by a non-owning lock(%s)", key.toString(), keyCondition.getOwningKeyLock().getName(), (keyLock == null) ? "null" : keyLock.getName());
      }
      else {
        keyCondition.dec();
      }
    }
    finally {
      stripeLock.unlock();
    }
  }

  protected ReentrantLock[] getStripeLockArray () {

    return stripeLocks;
  }

  protected ReentrantLock lockStripe (K key) {

    ReentrantLock stripeLock = stripeLocks[Math.abs(key.hashCode() % stripeLocks.length)];

    stripeLock.lock();

    return stripeLock;
  }

  protected void gateKey (KeyLock keyLock, K key) {

    KeyCondition keyCondition;

    while (((keyCondition = keyConditionMap.get(key)) != null) && (!keyCondition.isOwnedByKeyLock(keyLock))) {
      try {
        keyCondition.getCondition().await();
      }
      catch (InterruptedException interruptedException) {
        throw new CacheLockException(interruptedException, "Interrupted while awaiting the stripeLock on key(%s)", key.toString());
      }
    }
  }

  private Condition createCondition (K key) {

    return stripeLocks[Math.abs(key.hashCode() % stripeLocks.length)].newCondition();
  }

  private KeyCondition<K> retrieveGatedKeyCondition (K key) {

    return keyConditionMap.remove(key);
  }

  @InstrumentedClass
  private static class KeyCondition<K> implements Delayed {

    private KeyLock keyLock;
    private LockingCacheEnforcer<K, ?> lockingCache;
    private K key;
    private Condition condition;
    private AtomicBoolean terminated = new AtomicBoolean(false);
    private boolean timed;
    private long unlockTarget;
    private int lockCount = 1;

    public KeyCondition (LockingCacheEnforcer<K, ?> lockingCache, K key, long externalLockTimeout) {

      this.lockingCache = lockingCache;
      this.key = key;

      keyLock = new KeyLock();
      condition = lockingCache.createCondition(key);

      if (timed = (externalLockTimeout > 0)) {
        unlockTarget = System.currentTimeMillis() + externalLockTimeout;
        FORCED_UNLOCK_TIMER.add(this);
      }
    }

    public KeyLock getOwningKeyLock () {

      return keyLock;
    }

    public boolean isOwnedByKeyLock (KeyLock keyLock) {

      return this.keyLock.equals(keyLock);
    }

    public Condition getCondition () {

      return condition;
    }

    public long getUnlockTarget () {

      if (!timed) {
        throw new UnsupportedOperationException("No external lock timeout was specified");
      }

      return unlockTarget;
    }

    public long getDelay (TimeUnit unit) {

      if (!timed) {
        throw new UnsupportedOperationException("No external lock timeout was specified");
      }

      return unit.convert(unlockTarget - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public int compareTo (Delayed delayed) {

      if (!timed) {
        throw new UnsupportedOperationException("No external lock timeout was specified");
      }

      return (int)(unlockTarget - ((KeyCondition)delayed).getUnlockTarget());
    }

    public KeyLock inc () {

      lockCount++;

      return keyLock;
    }

    public void dec () {

      if (--lockCount == 0) {
        if (timed) {
          FORCED_UNLOCK_TIMER.remove(this);
        }

        terminate();
      }
    }

    public void terminate () {

      if (terminated.compareAndSet(false, true)) {

        ReentrantLock stripeLock;

        stripeLock = lockingCache.lockStripe(key);
        try {
          lockingCache.retrieveGatedKeyCondition(key).getCondition().signalAll();
        }
        finally {
          stripeLock.unlock();
        }
      }
    }
  }

  private static class ForcedUnlockTimer implements Runnable {

    private CountDownLatch exitLatch;
    private DelayQueue<KeyCondition> forcedUnlockQueue;
    private AtomicBoolean finished = new AtomicBoolean(false);

    public ForcedUnlockTimer () {

      forcedUnlockQueue = new DelayQueue<KeyCondition>();
      exitLatch = new CountDownLatch(1);
    }

    public void add (KeyCondition keyCondition) {

      forcedUnlockQueue.add(keyCondition);
    }

    public void remove (KeyCondition keyCondition) {

      forcedUnlockQueue.remove(keyCondition);
    }

    public void finish ()
      throws InterruptedException {

      finished.set(true);
      exitLatch.await();
    }

    public void run () {

      KeyCondition keyCondition;

      try {
        while (!finished.get()) {
          try {
            if ((keyCondition = forcedUnlockQueue.poll(500, TimeUnit.MILLISECONDS)) != null) {
              keyCondition.terminate();
            }
          }
          catch (InterruptedException interruptedException) {
            finished.set(true);
          }
        }

        for (KeyCondition exitKeyCondition : forcedUnlockQueue) {
          exitKeyCondition.terminate();
        }
      }
      finally {
        exitLatch.countDown();
      }
    }
  }
}
