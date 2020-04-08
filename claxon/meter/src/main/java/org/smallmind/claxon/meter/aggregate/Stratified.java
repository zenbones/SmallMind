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
import java.util.concurrent.locks.ReentrantLock;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.smallmind.nutsnbolts.time.StintUtility;

public class Stratified extends AbstractAggregate {

  private final ReentrantLock updateLock = new ReentrantLock();
  private final ReentrantLock flipLock = new ReentrantLock(true);
  private final double nanosecondsInVelocity;
  private Recorder writeRecorder;
  private Recorder readRecorder;
  private Histogram readHistogram;
  private Histogram writeHistogram;
  private long markTime;

  public Stratified () {

    this(null, 1, 3600000L, 2, null);
  }

  public Stratified (TimeUnit velocityTimeUnit) {

    this(null, 1, 3600000L, 2, velocityTimeUnit);
  }

  public Stratified (String name) {

    this(name, 1, 3600000L, 2, null);
  }

  public Stratified (String name, TimeUnit velocityTimeUnit) {

    this(name, 1, 3600000L, 2, velocityTimeUnit);
  }

  public Stratified (long lowestDiscernibleValue, long highestTrackableValue, int numberOfSignificantValueDigits, TimeUnit velocityTimeUnit) {

    this(null, lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits, velocityTimeUnit);
  }

  public Stratified (String name, long lowestDiscernibleValue, long highestTrackableValue, int numberOfSignificantValueDigits, TimeUnit velocityTimeUnit) {

    super(name);

    nanosecondsInVelocity = (velocityTimeUnit == null) ? 0 : StintUtility.convertToDouble(1, velocityTimeUnit, TimeUnit.NANOSECONDS);
    writeRecorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
    readRecorder = (velocityTimeUnit == null) ? writeRecorder : new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);

    markTime = System.nanoTime();
  }

  @Override
  public void update (long value) {

    if (nanosecondsInVelocity > 0) {
      checkForReset();
    }

    writeRecorder.recordValue(value);
  }

  private void checkForReset () {

    if (updateLock.tryLock()) {
      try {

        long now = System.nanoTime();

        if ((now - markTime) > nanosecondsInVelocity) {

          Recorder recorder = readRecorder;
          Histogram histogram = readHistogram;

          flipLock.lock();
          try {
            readRecorder = writeRecorder;
            readHistogram = writeHistogram;
          } finally {
            flipLock.unlock();
          }

          recorder.reset();
          writeRecorder = recorder;
          writeHistogram = histogram;

          markTime = now;
        }
      } finally {
        updateLock.unlock();
      }
    }
  }

  public Histogram get () {

    if (nanosecondsInVelocity > 0) {
      checkForReset();
    }

    flipLock.lock();
    try {

      return readHistogram = readRecorder.getIntervalHistogram(readHistogram);
    } finally {
      flipLock.unlock();
    }
  }
}
