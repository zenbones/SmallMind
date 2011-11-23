/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.nutsnbolts.util;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CyclicRoadblock {

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition trip = lock.newCondition();
  private final int parties;

  private Generation generation = new Generation();
  private int count;

  public CyclicRoadblock (int parties) {

    if (parties <= 0) {
      throw new IllegalArgumentException();
    }

    this.parties = parties;
    this.count = parties;
  }

  public int getParties () {

    return parties;
  }

  public int await () throws InterruptedException, BrokenBarrierException {

    try {
      return dowait(false, 0L);
    }
    catch (TimeoutException timeOutException) {
      throw new Error(timeOutException); // cannot happen;
    }
  }

  public int await (long timeout, TimeUnit unit)
    throws InterruptedException, BrokenBarrierException, TimeoutException {

    return dowait(true, unit.toNanos(timeout));
  }

  public boolean isBroken () {

    final ReentrantLock lock = this.lock;

    lock.lock();
    try {

      return generation.broken;
    }
    finally {
      lock.unlock();
    }
  }

  public void breakBarrier () {

    final ReentrantLock lock = this.lock;

    lock.lock();
    try {
      generation.broken = true;
      count = parties;
      trip.signalAll();
    }
    finally {
      lock.unlock();
    }
  }

  public void reset () {

    final ReentrantLock lock = this.lock;

    lock.lock();
    try {
      breakBarrier();   // break the current generation
      nextGeneration(); // start a new generation
    }
    finally {
      lock.unlock();
    }
  }

  public int getNumberWaiting () {

    final ReentrantLock lock = this.lock;

    lock.lock();
    try {
      return parties - count;
    }
    finally {
      lock.unlock();
    }
  }

  private void nextGeneration () {

    trip.signalAll();
    count = parties;
    generation = new Generation();
  }

  private int dowait (boolean timed, long nanos)
    throws InterruptedException, BrokenBarrierException, TimeoutException {

    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
      final Generation g = generation;

      if (g.broken)
        throw new BrokenBarrierException();

      if (Thread.interrupted()) {
        breakBarrier();
        throw new InterruptedException();
      }

      int index = --count;
      if (index == 0) {  // tripped
        boolean ranAction = false;
        try {
          ranAction = true;
          nextGeneration();
          return 0;
        }
        finally {
          if (!ranAction)
            breakBarrier();
        }
      }

      // loop until tripped, broken, interrupted, or timed out
      for (; ; ) {
        try {
          if (!timed)
            trip.await();
          else if (nanos > 0L)
            nanos = trip.awaitNanos(nanos);
        }
        catch (InterruptedException ie) {
          if (g == generation && !g.broken) {
            breakBarrier();
            throw ie;
          }
          else {
            // We're about to finish waiting even if we had not
            // been interrupted, so this interrupt is deemed to
            // "belong" to subsequent execution.
            Thread.currentThread().interrupt();
          }
        }

        if (g.broken)
          throw new BrokenBarrierException();

        if (g != generation)
          return index;

        if (timed && nanos <= 0L) {
          breakBarrier();
          throw new TimeoutException();
        }
      }
    }
    finally {
      lock.unlock();
    }
  }

  private static class Generation {

    boolean broken = false;
  }
}
