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

import java.util.concurrent.TimeUnit;
import org.HdrHistogram.Recorder;
import org.smallmind.claxon.registry.Clock;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.time.StintUtility;

/**
 * Sliding-window aggregate backed by HdrHistogram that produces time-normalized histograms.
 */
public class Stratified implements Aggregate {

  private final Clock clock;
  private final double nanosecondsInWindow;
  private volatile Recorder writeRecorder;
  private Recorder readRecorder;
  private long markTime;

  /**
   * Creates a stratified histogram with default histogram bounds and a one-second window.
   *
   * @param clock clock providing monotonic time
   */
  public Stratified (Clock clock) {

    this(clock, 1, 3600000L, 2, new Stint(1, TimeUnit.SECONDS));
  }

  /**
   * Creates a stratified histogram with default bounds and a custom window.
   *
   * @param clock       clock providing monotonic time
   * @param windowStint window duration
   */
  public Stratified (Clock clock, Stint windowStint) {

    this(clock, 1, 3600000L, 2, windowStint);
  }

  /**
   * Creates a stratified histogram with custom histogram bounds and precision using a one-second window.
   *
   * @param clock                           clock providing monotonic time
   * @param lowestDiscernibleValue          smallest value to track
   * @param highestTrackableValue           largest value to track
   * @param numberOfSignificantValueDigits  histogram precision
   */
  public Stratified (Clock clock, long lowestDiscernibleValue, long highestTrackableValue, int numberOfSignificantValueDigits) {

    this(clock, lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits, new Stint(1, TimeUnit.SECONDS));
  }

  /**
   * Creates a stratified histogram with full customization of bounds, precision, and window.
   *
   * @param clock                           clock providing monotonic time
   * @param lowestDiscernibleValue          smallest value to track
   * @param highestTrackableValue           largest value to track
   * @param numberOfSignificantValueDigits  histogram precision
   * @param windowStint                     window duration
   */
  public Stratified (Clock clock, long lowestDiscernibleValue, long highestTrackableValue, int numberOfSignificantValueDigits, Stint windowStint) {

    this.clock = clock;

    nanosecondsInWindow = StintUtility.convertToDouble(windowStint.getTime(), windowStint.getTimeUnit(), TimeUnit.NANOSECONDS);
    writeRecorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
    readRecorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
    markTime = clock.monotonicTime();
  }

  /**
   * Records a value into the current histogram.
   *
   * @param value value to record
   */
  @Override
  public void update (long value) {

    writeRecorder.recordValue(value);
  }

  /**
   * Returns the histogram for the last window and resets tracking for the next interval.
   *
   * @return histogram with time-scaling information
   */
  public synchronized HistogramTime get () {

    Recorder recorder;
    double timeFactor;
    long now;

    recorder = readRecorder;
    readRecorder = writeRecorder;
    recorder.reset();

    now = clock.monotonicTime();
    writeRecorder = recorder;

    timeFactor = (now - markTime) / nanosecondsInWindow;
    markTime = now;

    return new HistogramTime(readRecorder.getIntervalHistogram(), timeFactor);
  }
}
