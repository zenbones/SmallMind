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
package org.smallmind.javafx.extras;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class ConsolidatingChangeListener<T> implements ChangeListener<T> {

  private final ConsolidationWorker consolidationWorker;

  public ConsolidatingChangeListener (long consolidationTimeMillis) {

    Thread consolidationThread;

    consolidationThread = new Thread(consolidationWorker = new ConsolidationWorker(consolidationTimeMillis));
    consolidationThread.setDaemon(true);
    consolidationThread.start();
  }

  public abstract void consolidatedChange (ObservableValue<? extends T> observableValue, T t1, T t2);

  @Override
  public final void changed (ObservableValue<? extends T> observableValue, T t1, T t2) {

    consolidationWorker.update(observableValue, t1, t2);
  }

  @Override
  protected void finalize ()
    throws Throwable {

    super.finalize();
    consolidationWorker.abort();
  }

  private class ConsolidationWorker implements Runnable {

    private final CountDownLatch exitLatch = new CountDownLatch(1);
    private final AtomicBoolean aborted = new AtomicBoolean(false);
    private final long consolidationTimeMillis;
    private ObservableValue<? extends T> observableValue;
    private T initialValue;
    private T currentValue;
    private boolean initial = true;
    private long startTime;

    private ConsolidationWorker (long consolidationTimeMillis) {

      this.consolidationTimeMillis = consolidationTimeMillis;
    }

    public void abort ()
      throws InterruptedException {

      aborted.set(true);
      exitLatch.await();
    }

    public synchronized void update (ObservableValue<? extends T> observableValue, T initialValue, T currentValue) {

      if (initial) {
        this.observableValue = observableValue;
        this.initialValue = initialValue;

        initial = false;
        notify();
      }

      startTime = System.currentTimeMillis();
      this.currentValue = currentValue;
    }

    @Override
    public void run () {

      try {
        while (!aborted.get()) {
          synchronized (this) {
            if (initial) {
              wait();
            }
            else {
              long currentlyPassed;

              while ((currentlyPassed = System.currentTimeMillis() - startTime) < consolidationTimeMillis) {
                wait(consolidationTimeMillis - currentlyPassed);
              }

              consolidatedChange(observableValue, initialValue, currentValue);
              initial = true;
            }
          }
        }
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ConsolidatingChangeListener.class).error(interruptedException);
      }
      finally {
        exitLatch.countDown();
      }
    }
  }
}
