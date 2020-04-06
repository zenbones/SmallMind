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
package org.smallmind.claxon.meter.aggregate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.smallmind.nutsnbolts.time.DurationUtility;

public class Clocked extends AbstractAggregate {

  private final ReentrantLock lock = new ReentrantLock();
  private final AtomicLong countInWindow = new AtomicLong();
  private final double nanosecondsInWindow;
  private double velocity = 0;
  private long markTime;

  public Clocked (String name, TimeUnit velocityTimeUnit) {

    super(name);

    nanosecondsInWindow = DurationUtility.convertToDouble(1, velocityTimeUnit, TimeUnit.NANOSECONDS);
    markTime = System.nanoTime();
  }

  public void inc () {

    add(1);
  }

  public void dec () {

    add(-1);
  }

  public void add (long delta) {

    long countInWindowRightNow;
    long now;
    long transpired;

    countInWindowRightNow = countInWindow.addAndGet(delta);
    now = System.nanoTime();

    if ((transpired = (now - markTime)) > nanosecondsInWindow) {
      if (lock.tryLock()) {
        try {
          velocity = countInWindowRightNow / (transpired / nanosecondsInWindow);

          markTime = now;
          countInWindow.addAndGet(-countInWindowRightNow);
        } finally {
          lock.unlock();
        }
      }
    }
  }

  @Override
  public void update (long value) {

    add(value);
  }

  public double getVelocity () {

    return velocity;
  }
}
