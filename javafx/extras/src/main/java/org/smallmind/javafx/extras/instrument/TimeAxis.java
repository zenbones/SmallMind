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
 * {@link ValueAxis} implementation that shows a sliding window of wall-clock time with periodic updates.
 * The axis manages its own scheduled executor to advance the time window and exposes a pause flag.
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
   * Creates a time axis showing a sliding window spanning the specified number of milliseconds.
   *
   * @param spanInMilliseconds the width of the displayed time window
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
   * Creates a time axis with a label and a sliding window spanning the specified number of milliseconds.
   *
   * @param axisLabel          axis label
   * @param spanInMilliseconds the width of the displayed time window
   */
  public TimeAxis (String axisLabel, long spanInMilliseconds) {

    this(spanInMilliseconds);

    setLabel(axisLabel);
  }

  /**
   * Recomputes the lower and upper bounds unless paused.
   */
  private void updateTime () {

    if (!getPaused()) {

      long now = System.currentTimeMillis();

      setLowerBound(now - millisecondSpan.get());
      setUpperBound(now);
    }
  }

  /**
   * @return a property controlling whether the axis updates its bounds
   */
  public BooleanProperty pausedProperty () {

    return pausedProperty;
  }

  /**
   * @return whether the axis is currently paused
   */
  public boolean getPaused () {

    return pausedProperty.get();
  }

  /**
   * Enables or disables automatic bound updates.
   *
   * @param paused {@code true} to stop updating bounds
   */
  public void setPaused (boolean paused) {

    pausedProperty.set(paused);
  }

  /**
   * No-op because range is managed internally by {@link #updateTime()}.
   */
  @Override
  protected void setRange (Object range, boolean animate) {

  }

  /**
   * @return current lower and upper bounds packaged as {@link EndPoints}
   */
  @Override
  protected Object getRange () {

    return new EndPoints<>((long)getLowerBound(), (long)getUpperBound());
  }

  /**
   * Computes major tick values at 15 second intervals within the current range.
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
   * Computes minor tick marks at 5 second intervals, excluding major ticks.
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
   * Formats tick labels using {@code HH:mm:ss} in the system time zone.
   */
  @Override
  protected String getTickMarkLabel (Long milliseconds) {

    return DATE_TIME_FORMATTER.format(LocalDateTime.ofEpochSecond(milliseconds, 0, ZONE_OFFSET));
  }

  /**
   * Cancels the scheduled updater thread.
   */
  public void stop () {

    future.cancel(false);
  }
}
