/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.progress;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class ProgressTimer implements Runnable {

  private final CountDownLatch exitLatch;
  private final CountDownLatch pulseLatch;
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final ProgressPanel progressPanel;
  private final long pulseTime;

  public ProgressTimer (ProgressPanel progressPanel, long pulseTime) {

    this.progressPanel = progressPanel;
    this.pulseTime = pulseTime;

    pulseLatch = new CountDownLatch(1);
    exitLatch = new CountDownLatch(1);
  }

  public void finish ()
    throws InterruptedException {

    if (finished.compareAndSet(false, true)) {
      pulseLatch.countDown();
    }

    exitLatch.await();
  }

  public void run () {

    try {
      while (!finished.get()) {
        progressPanel.setProgress();

        try {
          pulseLatch.await(pulseTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
          LoggerManager.getLogger(ProgressTimer.class).error(interruptedException);
        }
      }
    } finally {
      exitLatch.countDown();
    }
  }
}
