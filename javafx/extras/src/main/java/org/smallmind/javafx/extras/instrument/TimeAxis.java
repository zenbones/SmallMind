/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.javafx.extras.instrument;

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
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimeAxis extends ValueAxis<Long> {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss");
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

    return new EndPoints<Long>((long)getLowerBound(), (long)getUpperBound());
  }

  @Override
  protected List<Long> calculateTickValues (double length, Object range) {

    LinkedList<Long> ticks = new LinkedList<Long>();
    DateTime origin = new DateTime(((EndPoints<Long>)range).getLow()).withMillisOfSecond(0);
    DateTime bound = new DateTime(((EndPoints<Long>)range).getHigh());
    int majorInterval;

    if ((majorInterval = (origin.getSecondOfMinute() / 15) + 1) < 4) {
      origin = origin.withSecondOfMinute(majorInterval * 15);
    }
    else {
      origin = origin.plusMinutes(1).withSecondOfMinute(0);
    }

    while (origin.isBefore(bound)) {
      ticks.add(origin.getMillis());
      origin = origin.plusSeconds(15);
    }

    return ticks;
  }

  @Override
  protected List<Long> calculateMinorTickMarks () {

    LinkedList<Long> ticks = new LinkedList<Long>();
    Object range = getRange();
    DateTime origin = new DateTime(((EndPoints<Long>)range).getLow()).withMillisOfSecond(0);
    DateTime bound = new DateTime(((EndPoints<Long>)range).getHigh());
    int minorInterval;

    if ((minorInterval = (origin.getSecondOfMinute() / 5) + 1) < 12) {
      origin = origin.withSecondOfMinute(minorInterval * 5);
    }
    else {
      origin = origin.plusMinutes(1).withSecondOfMinute(0);
    }

    while (origin.isBefore(bound)) {
      if ((origin.getSecondOfMinute() % 15) != 0) {
        ticks.add(origin.getMillis());
      }

      origin = origin.plusSeconds(5);
    }

    return ticks;
  }

  @Override
  protected String getTickMarkLabel (Long milliseconds) {

    return DATE_TIME_FORMATTER.print(new DateTime(milliseconds));
  }

  public void stop () {

    future.cancel(false);
  }
}
