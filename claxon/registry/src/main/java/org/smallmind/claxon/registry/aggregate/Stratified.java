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
 * Sliding-window {@link Aggregate} backed by two HdrHistogram {@link Recorder} instances that
 * are swapped on each read to provide wait-free writes and consistent snapshots.
 *
 * <p>Values are written to {@link #writeRecorder} at any time via {@link #update(long)}.
 * When {@link #get()} is called the two recorders are swapped: the former write recorder
 * becomes the read recorder and is queried for an interval histogram, while the former read
 * recorder (now reset) becomes the new write recorder. This guarantees that the snapshot
 * returned by {@link #get()} is not modified by concurrent writers after the swap.</p>
 *
 * <p>The {@link HistogramTime} returned by {@link #get()} carries a {@code timeFactor} that
 * encodes how much real time elapsed between the two most recent calls, allowing consumers
 * to normalise counts to the configured window duration.</p>
 */
public class Stratified implements Aggregate {

  /**
   * Source of monotonic timestamps used to compute the time factor on each {@link #get()} call.
   */
  private final Clock clock;

  /**
   * The configured window duration expressed in nanoseconds, used as the normalisation denominator.
   */
  private final double nanosecondsInWindow;

  /**
   * The currently active write recorder; receives all {@link #update(long)} calls.
   * Declared {@code volatile} so the swap performed inside the {@code synchronized} {@link #get()}
   * is visible to unsynchronised writers immediately.
   */
  private volatile Recorder writeRecorder;

  /**
   * The idle recorder that is reset between windows and queried for a snapshot during {@link #get()}.
   * Only accessed from within the {@code synchronized} {@link #get()} method.
   */
  private Recorder readRecorder;

  /**
   * Monotonic timestamp (nanoseconds) of the most recent {@link #get()} call (or construction).
   */
  private long markTime;

  /**
   * Constructs a {@code Stratified} aggregate with default HdrHistogram bounds
   * ({@code [1, 3600000]} with two significant digits) and a one-second window.
   *
   * @param clock source of monotonic time used to compute elapsed intervals
   */
  public Stratified (Clock clock) {

    this(clock, 1, 3600000L, 2, new Stint(1, TimeUnit.SECONDS));
  }

  /**
   * Constructs a {@code Stratified} aggregate with default HdrHistogram bounds
   * ({@code [1, 3600000]} with two significant digits) and a custom window.
   *
   * @param clock       source of monotonic time used to compute elapsed intervals
   * @param windowStint duration of the normalisation window; must be positive
   */
  public Stratified (Clock clock, Stint windowStint) {

    this(clock, 1, 3600000L, 2, windowStint);
  }

  /**
   * Constructs a {@code Stratified} aggregate with custom HdrHistogram bounds and precision,
   * using a one-second window.
   *
   * @param clock                          source of monotonic time used to compute elapsed intervals
   * @param lowestDiscernibleValue         smallest value the histogram can distinguish; must be {@code >= 1}
   * @param highestTrackableValue          largest value the histogram will track without overflow
   * @param numberOfSignificantValueDigits number of significant decimal digits of precision
   *                                       (typically 1–5)
   */
  public Stratified (Clock clock, long lowestDiscernibleValue, long highestTrackableValue, int numberOfSignificantValueDigits) {

    this(clock, lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits, new Stint(1, TimeUnit.SECONDS));
  }

  /**
   * Constructs a {@code Stratified} aggregate with full control over HdrHistogram bounds,
   * precision, and the normalisation window.
   *
   * @param clock                          source of monotonic time used to compute elapsed intervals
   * @param lowestDiscernibleValue         smallest value the histogram can distinguish; must be {@code >= 1}
   * @param highestTrackableValue          largest value the histogram will track without overflow
   * @param numberOfSignificantValueDigits number of significant decimal digits of precision
   *                                       (typically 1–5)
   * @param windowStint                    duration of the normalisation window; must be positive
   */
  public Stratified (Clock clock, long lowestDiscernibleValue, long highestTrackableValue, int numberOfSignificantValueDigits, Stint windowStint) {

    this.clock = clock;

    nanosecondsInWindow = StintUtility.convertToDouble(windowStint.getTime(), windowStint.getTimeUnit(), TimeUnit.NANOSECONDS);
    writeRecorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
    readRecorder = new Recorder(lowestDiscernibleValue, highestTrackableValue, numberOfSignificantValueDigits);
    markTime = clock.monotonicTime();
  }

  /**
   * Records {@code value} into the currently active write recorder.
   *
   * <p>This method is wait-free; it delegates directly to
   * {@link Recorder#recordValue(long)} on the volatile {@link #writeRecorder} reference.</p>
   *
   * @param value the measurement to record into the histogram
   */
  @Override
  public void update (long value) {

    writeRecorder.recordValue(value);
  }

  /**
   * Swaps the read and write recorders, captures an interval histogram, and returns it
   * together with a time-normalisation factor.
   *
   * <p>The swap is performed under {@code this} monitor so that concurrent readers always
   * see a consistent view. After the swap, the newly retired recorder (now {@link #readRecorder})
   * is queried for its interval histogram via {@link Recorder#getIntervalHistogram()}, and
   * the formerly idle recorder (now {@link #writeRecorder}) is reset and ready to accept
   * new writes.</p>
   *
   * @return a {@link HistogramTime} pairing the interval histogram with the ratio of actual
   * elapsed nanoseconds to the configured window duration; never {@code null}
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

    timeFactor = nanosecondsInWindow / (now - markTime);
    markTime = now;

    return new HistogramTime(readRecorder.getIntervalHistogram(), timeFactor);
  }
}
