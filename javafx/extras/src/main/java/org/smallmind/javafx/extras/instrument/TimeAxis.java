/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

  public TimeAxis (String axisLabel, long spanInMilliseconds) {

    this(spanInMilliseconds);

    setLabel(axisLabel);
  }

  private void updateTime () {

    if (!getPaused()) {

      long now = System.currentTimeMillis();

      setLowerBound(now - millisecondSpan.get());
      setUpperBound(now);
    }
  }

  public BooleanProperty pausedProperty () {

    return pausedProperty;
  }

  public boolean getPaused () {

    return pausedProperty.get();
  }

  public void setPaused (boolean paused) {

    pausedProperty.set(paused);
  }

  @Override
  protected void setRange (Object range, boolean animate) {

  }

  @Override
  protected Object getRange () {

    return new EndPoints<>((long)getLowerBound(), (long)getUpperBound());
  }

  @Override
  protected List<Long> calculateTickValues (double length, Object range) {

    LinkedList<Long> ticks = new LinkedList<Long>();
    LocalDateTime origin = LocalDateTime.ofInstant(Instant.ofEpochMilli(((EndPoints<Long>)range).getLow()), ZoneId.systemDefault());
    LocalDateTime bound = LocalDateTime.ofInstant(Instant.ofEpochMilli(((EndPoints<Long>)range).getHigh()), ZoneId.systemDefault());
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

  @Override
  protected List<Long> calculateMinorTickMarks () {

    LinkedList<Long> ticks = new LinkedList<Long>();
    Object range = getRange();
    LocalDateTime origin = LocalDateTime.ofInstant(Instant.ofEpochMilli(((EndPoints<Long>)range).getLow()), ZoneId.systemDefault());
    LocalDateTime bound = LocalDateTime.ofInstant(Instant.ofEpochMilli(((EndPoints<Long>)range).getHigh()), ZoneId.systemDefault());
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

  @Override
  protected String getTickMarkLabel (Long milliseconds) {

    return DATE_TIME_FORMATTER.format(LocalDateTime.ofEpochSecond(milliseconds, 0, ZONE_OFFSET));
  }

  public void stop () {

    future.cancel(false);
  }
}
