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
package org.smallmind.javafx.extras.instrument;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.chart.ValueAxis;

/**
 * A {@link ValueAxis} implementation for wall-clock epoch-millisecond values that maintains a
 * sliding window of fixed duration. The axis advances its lower and upper bounds once per second
 * via a shared daemon scheduler, producing a continuously scrolling time view. Tick labels are
 * formatted as {@code HH:mm:ss} in the system time zone. Major ticks are placed at 15-second
 * intervals and minor ticks at 5-second intervals. Advancement can be suspended via the paused
 * property.
 */
public class TimeAxis extends ValueAxis<Long> {

  private static final ZoneOffset ZONE_OFFSET;
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(2, new ThreadFactory() {

    @Override
    public Thread newThread (final Runnable runnable) {

      Thread thread = new Thread(runnable);

      thread.setDaemon(true);

      return thread;
    }
  });
  private final ScheduledFuture<?> future;
  private final AtomicLong millisecondSpan;
  private final BooleanProperty pausedProperty = new SimpleBooleanProperty(false);

  static {

    Instant instant = Instant.now();
    ZoneId systemZone = ZoneId.systemDefault();

    ZONE_OFFSET = systemZone.getRules().getOffset(instant);
  }

  /**
   * Creates a time axis whose visible window spans {@code spanInMilliseconds} milliseconds. The
   * axis begins advancing immediately, updating bounds once per second.
   *
   * @param spanInMilliseconds the fixed width of the displayed time window in milliseconds; must be positive
   */
  public TimeAxis (long spanInMilliseconds) {

    millisecondSpan = new AtomicLong(spanInMilliseconds);

    setAutoRanging(false);
    setTickLabelRotation(90);

    updateTime();

    future = SCHEDULED_EXECUTOR.scheduleAtFixedRate(new Runnable() {

      @Override
      public void run () {

        updateTime();
      }
    }, 1, 1, TimeUnit.SECONDS);
  }

  /**
   * Creates a time axis with a visible label and the given window span. The axis begins advancing
   * immediately, updating bounds once per second.
   *
   * @param axisLabel          the text label shown alongside the axis
   * @param spanInMilliseconds the fixed width of the displayed time window in milliseconds; must be positive
   */
  public TimeAxis (String axisLabel, long spanInMilliseconds) {

    this(spanInMilliseconds);

    setLabel(axisLabel);
  }

  /**
   * Recomputes the lower and upper bounds to span the current wall-clock time window. Has no
   * effect when the axis is paused.
   */
  private void updateTime () {

    if (!getPaused()) {

      long now = System.currentTimeMillis();

      setLowerBound(now - millisecondSpan.get());
      setUpperBound(now);
    }
  }

  /**
   * Returns the observable property that controls whether the axis advances its time bounds.
   * Setting this to {@code true} freezes the displayed range at the current position.
   *
   * @return the paused property; never {@code null}
   */
  public BooleanProperty pausedProperty () {

    return pausedProperty;
  }

  /**
   * Returns whether the axis is currently paused.
   *
   * @return {@code true} if time-bound updates are suspended
   */
  public boolean getPaused () {

    return pausedProperty.get();
  }

  /**
   * Suspends or resumes automatic time-bound updates.
   *
   * @param paused {@code true} to freeze the axis; {@code false} to resume advancing
   */
  public void setPaused (boolean paused) {

    pausedProperty.set(paused);
  }

  /**
   * No-op; the range is managed internally by the scheduled {@link #updateTime()} calls and is
   * not restored from an external range object.
   *
   * @param range   ignored
   * @param animate ignored
   */
  @Override
  protected void setRange (Object range, boolean animate) {

  }

  /**
   * Returns the current lower and upper bounds as an {@link EndPoints} of epoch-millisecond values.
   *
   * @return the current axis range; never {@code null}
   */
  @Override
  protected Object getRange () {

    return new EndPoints<>((long)getLowerBound(), (long)getUpperBound());
  }

  /**
   * Computes major tick positions at 15-second boundaries within the supplied range.
   *
   * @param length the pixel length of the axis (unused)
   * @param range  the current range as an {@link EndPoints} of epoch-millisecond values
   * @return an ordered list of epoch-millisecond values at which major ticks should appear
   */
  @Override
  protected List<Long> calculateTickValues (double length, Object range) {

    LinkedList<Long> ticks = new LinkedList<Long>();
    LocalDateTime origin = LocalDateTime.ofInstant(Instant.ofEpochMilli(((EndPoints<Long>)range).low()), ZoneId.systemDefault());
    LocalDateTime bound = LocalDateTime.ofInstant(Instant.ofEpochMilli(((EndPoints<Long>)range).high()), ZoneId.systemDefault());
    int majorInterval;

    if ((majorInterval = (origin.getSecond() / 15) + 1) < 4) {
      origin = origin.withSecond(majorInterval * 15);
    } else {
      origin = origin.plusMinutes(1).withSecond(0);
    }

    while (origin.isBefore(bound)) {
      ticks.add(origin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
      origin = origin.plusSeconds(15);
    }

    return ticks;
  }

  /**
   * Computes minor tick positions at 5-second boundaries, excluding positions that coincide with
   * a major tick (i.e. multiples of 15 seconds).
   *
   * @return an ordered list of epoch-millisecond values at which minor ticks should appear
   */
  @Override
  protected List<Long> calculateMinorTickMarks () {

    LinkedList<Long> ticks = new LinkedList<Long>();
    Object range = getRange();
    LocalDateTime origin = LocalDateTime.ofInstant(Instant.ofEpochMilli(((EndPoints<Long>)range).low()), ZoneId.systemDefault());
    LocalDateTime bound = LocalDateTime.ofInstant(Instant.ofEpochMilli(((EndPoints<Long>)range).high()), ZoneId.systemDefault());
    int minorInterval;

    if ((minorInterval = (origin.getSecond() / 5) + 1) < 12) {
      origin = origin.withSecond(minorInterval * 5);
    } else {
      origin = origin.plusMinutes(1).withSecond(0);
    }

    while (origin.isBefore(bound)) {
      if ((origin.getSecond() % 15) != 0) {
        ticks.add(origin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
      }

      origin = origin.plusSeconds(5);
    }

    return ticks;
  }

  /**
   * Converts an epoch-millisecond tick value to a display string formatted as {@code HH:mm:ss}
   * using the system time zone offset determined at class load time.
   *
   * @param milliseconds the epoch-millisecond value of the tick
   * @return formatted time string; never {@code null}
   */
  @Override
  protected String getTickMarkLabel (Long milliseconds) {

    return DATE_TIME_FORMATTER.format(LocalDateTime.ofEpochSecond(milliseconds, 0, ZONE_OFFSET));
  }

  /**
   * Cancels the scheduled time-update task, stopping the axis from advancing. Should be called
   * when the owning chart is no longer needed to release the background thread.
   */
  public void stop () {

    future.cancel(false);
  }
}
