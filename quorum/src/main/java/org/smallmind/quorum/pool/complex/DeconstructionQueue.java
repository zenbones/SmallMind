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
package org.smallmind.quorum.pool.complex;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.scribe.pen.LoggerManager;

public class DeconstructionQueue {

  private final ConcurrentSkipListMap<IgnitionKey, DeconstructionFuse> fuseMap = new ConcurrentSkipListMap<IgnitionKey, DeconstructionFuse>();
  private final AtomicInteger ordinal = new AtomicInteger(0);

  private IgnitionWorker ignitionWorker;

  public void startup () {

    Thread ignitionThread;

    ignitionThread = new Thread(ignitionWorker = new IgnitionWorker());
    ignitionThread.setDaemon(true);
    ignitionThread.start();
  }

  public int nextOrdinal () {

    return ordinal.incrementAndGet();
  }

  public void add (DeconstructionFuse deconstructionFuse) {

    fuseMap.put(new IgnitionKey(deconstructionFuse), deconstructionFuse);
  }

  public void remove (DeconstructionFuse deconstructionFuse) {

    fuseMap.remove(new IgnitionKey(deconstructionFuse));
  }

  public void shutdown ()
    throws InterruptedException {

    ignitionWorker.shutdown();
  }

  private class IgnitionWorker implements Runnable {

    private final CountDownLatch terminationLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    public void shutdown ()
      throws InterruptedException {

      terminationLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!terminationLatch.await(1, TimeUnit.SECONDS)) {

          Map.Entry<IgnitionKey, DeconstructionFuse> firstEntry;
          long now = System.currentTimeMillis();

          while (((firstEntry = fuseMap.firstEntry()) != null) && (firstEntry.getKey().getIgnitionTime() <= now)) {

            DeconstructionFuse firstFuse;

            if ((firstFuse = fuseMap.remove(firstEntry.getKey())) != null) {
              firstFuse.ignite();
            }
          }
        }
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(DeconstructionQueue.class).error(interruptedException);
      }

      exitLatch.countDown();
    }
  }

  private class IgnitionKey implements Comparable<IgnitionKey> {

    private final long ignitionTime;
    private final int ordinal;

    private IgnitionKey (DeconstructionFuse deconstructionFuse) {

      this(deconstructionFuse.getOrdinal(), deconstructionFuse.getIgnitionTime());
    }

    private IgnitionKey (int ordinal, long ignitionTime) {

      this.ordinal = ordinal;
      this.ignitionTime = ignitionTime;
    }

    public int getOrdinal () {

      return ordinal;
    }

    public long getIgnitionTime () {

      return ignitionTime;
    }

    @Override
    public int compareTo (IgnitionKey key) {

      long comparison;

      if ((comparison = ignitionTime - key.getIgnitionTime()) == 0) {

        return ordinal - key.getOrdinal();
      }

      return (int)comparison;
    }

    @Override
    public int hashCode () {

      return ordinal;
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof IgnitionKey) && (ordinal == ((IgnitionKey)obj).getOrdinal());
    }
  }
}