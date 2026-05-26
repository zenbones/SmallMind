/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.claxon.registry.aggregate;

import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantLock;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AveragedTest {

  public void testEmptyWindowReturnsNaN () {

    Assert.assertTrue(Double.isNaN(new Averaged().getAverage()));
  }

  public void testSingleValueAverageIsThatValue () {

    Averaged averaged = new Averaged();

    averaged.update(42);

    Assert.assertEquals(averaged.getAverage(), 42.0);
  }

  public void testArithmeticMeanOfMultipleValues () {

    Averaged averaged = new Averaged();

    averaged.update(2);
    averaged.update(4);
    averaged.update(6);

    Assert.assertEquals(averaged.getAverage(), 4.0);
  }

  public void testNegativeAndPositiveValuesAreAveraged () {

    Averaged averaged = new Averaged();

    averaged.update(-10);
    averaged.update(10);

    Assert.assertEquals(averaged.getAverage(), 0.0);
  }

  public void testGetAverageResetsAccumulators () {

    Averaged averaged = new Averaged();

    averaged.update(100);
    averaged.getAverage();

    Assert.assertTrue(Double.isNaN(averaged.getAverage()));
  }

  public void testRecordingAfterResetStartsFresh () {

    Averaged averaged = new Averaged();

    averaged.update(99);
    averaged.getAverage();

    averaged.update(3);
    averaged.update(5);

    Assert.assertEquals(averaged.getAverage(), 4.0);
  }

  public void testQueuedUpdateIsIncludedInSubsequentAverage ()
    throws Exception {

    Averaged averaged = new Averaged();
    Field lockField = Averaged.class.getDeclaredField("lock");

    lockField.setAccessible(true);

    ReentrantLock lock = (ReentrantLock)lockField.get(averaged);

    lock.lock();
    try {

      Thread updater = new Thread(() -> averaged.update(20));

      updater.setDaemon(true);
      updater.start();
      updater.join(1000);
    } finally {
      lock.unlock();
    }

    Assert.assertEquals(averaged.getAverage(), 20.0);
  }
}
