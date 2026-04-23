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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;

/**
 * A {@link LineChart} that plots velocity statistics across four rolling windows — inception
 * average, 15-minute, 5-minute, and 1-minute — over a sliding wall-clock time window managed by
 * a {@link TimeAxis}. Data points outside the visible window are trimmed on each update to bound
 * memory usage. All data updates are marshalled onto the JavaFX application thread automatically.
 */
public class VelocityChart extends LineChart<Long, Number> {

  private static final String[] SERIES_NAMES = new String[] {"Inception", "15 minute", "5 minute", "1 minute"};
  private final TimeAxis timeAxis;
  private final Series<Long, Number>[] seriesArray = new Series[4];
  private final AtomicBoolean hasData = new AtomicBoolean(false);

  /**
   * Creates a chart whose time axis spans the given number of milliseconds into the past from now.
   *
   * @param spanInMilliseconds width of the visible time window in milliseconds; must be positive
   */
  public VelocityChart (long spanInMilliseconds) {

    this(new TimeAxis(spanInMilliseconds));
  }

  private VelocityChart (TimeAxis timeAxis) {

    super(timeAxis, new NumberAxis());

    this.timeAxis = timeAxis;

    for (int index = 0; index < seriesArray.length; index++) {
      seriesArray[index] = new Series<Long, Number>();
      seriesArray[index].setName(SERIES_NAMES[index]);
    }

    getStyleClass().add("velocity-chart");
    getStylesheets().add(VelocityChart.class.getResource("VelocityChart.css").toExternalForm());
  }

  /**
   * Returns the property controlling whether chart updates are paused.
   *
   * @return the paused property; never {@code null}
   */
  public BooleanProperty pausedProperty () {

    return timeAxis.pausedProperty();
  }

  /**
   * Returns whether the chart is currently paused.
   *
   * @return {@code true} if the time axis has stopped advancing
   */
  public boolean isPaused () {

    return timeAxis.getPaused();
  }

  /**
   * Pauses or resumes time-axis advancement and data acceptance.
   *
   * @param paused {@code true} to halt the time axis; {@code false} to resume
   */
  public void setPaused (boolean paused) {

    timeAxis.setPaused(paused);
  }

  /**
   * Appends a velocity measurement to all four series at the given timestamp and removes any data
   * points that fall before the current lower bound of the time axis. The series are populated
   * lazily on the first call. Execution is dispatched to the JavaFX application thread via
   * {@link Platform#runLater}.
   *
   * @param milliseconds epoch time in milliseconds for the sample
   * @param blur         the velocity data to plot; must not be {@code null}
   */
  public void addBlur (final long milliseconds, final Blur blur) {

    Platform.runLater(new Runnable() {

      @Override
      public void run () {

        if (hasData.compareAndSet(false, true)) {
          getData().addAll(Arrays.asList(seriesArray));
        }

        long minTime = (long)VelocityChart.this.timeAxis.getLowerBound();

        for (Series<Long, Number> series : getData()) {
          if (!series.getData().isEmpty()) {
            while (series.getData().get(0).getXValue() < minTime) {
              series.getData().remove(0);
            }
          }
        }

        seriesArray[0].getData().add(new Data<Long, Number>(milliseconds, blur.avgVelocity()));
        seriesArray[1].getData().add(new Data<Long, Number>(milliseconds, blur.avgVelocity_15()));
        seriesArray[2].getData().add(new Data<Long, Number>(milliseconds, blur.avgVelocity_5()));
        seriesArray[3].getData().add(new Data<Long, Number>(milliseconds, blur.avgVelocity_1()));
      }
    });
  }

  /**
   * Stops the underlying {@link TimeAxis} scheduled executor, preventing further axis updates.
   * Should be called when the chart is no longer needed to release background threads.
   */
  public void stop () {

    timeAxis.stop();
  }
}
