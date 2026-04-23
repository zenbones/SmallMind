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
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedAreaChart;

/**
 * A {@link StackedAreaChart} that visualises latency distribution percentiles over a sliding
 * wall-clock time window. Each series represents the incremental gap between adjacent percentile
 * thresholds (median, 75th, 95th, 98th, 99th, 99.9th), so stacking them produces a filled view of
 * the full latency spread. Data points outside the visible window are trimmed on each update.
 * All data updates are marshalled onto the JavaFX application thread automatically.
 */
public class SigmaChart extends StackedAreaChart<Long, Number> {

  private static final String[] SERIES_NAMES = new String[] {"Median", "75th pctl", "95th pctl", "98th pctl", "99th pctl", "99.9th pctl"};

  private final TimeAxis timeAxis;
  private final Series<Long, Number>[] seriesArray = new Series[6];
  private final AtomicBoolean hasData = new AtomicBoolean(false);

  /**
   * Creates a chart whose time axis spans the given number of milliseconds into the past from now.
   *
   * @param spanInMilliseconds width of the visible time window in milliseconds; must be positive
   */
  public SigmaChart (long spanInMilliseconds) {

    this(new TimeAxis(spanInMilliseconds));
  }

  private SigmaChart (TimeAxis timeAxis) {

    super(timeAxis, new NumberAxis());

    this.timeAxis = timeAxis;

    for (int index = 0; index < seriesArray.length; index++) {
      seriesArray[index] = new Series<Long, Number>();
      seriesArray[index].setName(SERIES_NAMES[index]);
    }

    getStyleClass().add("sigma-chart");
    getStylesheets().add(SigmaChart.class.getResource("SigmaChart.css").toExternalForm());
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
   * Appends a percentile dispersion sample to the chart. Each series receives the incremental
   * difference between adjacent percentile boundaries so that stacking produces the full
   * distribution spread. Data points older than the current lower time-axis bound are trimmed.
   * Execution is dispatched to the JavaFX application thread via {@link Platform#runLater}.
   *
   * @param milliseconds epoch time in milliseconds for the sample
   * @param dispersion   percentile data to plot; must not be {@code null}
   */
  public void addDispersion (final long milliseconds, final Dispersion dispersion) {

    Platform.runLater(new Runnable() {

      @Override
      public void run () {

        if (hasData.compareAndSet(false, true)) {
          getData().addAll(Arrays.asList(seriesArray));
        }

        long minTime = (long)SigmaChart.this.timeAxis.getLowerBound();

        for (Series<Long, Number> series : getData()) {
          if (!series.getData().isEmpty()) {
            while (series.getData().get(0).getXValue() < minTime) {
              series.getData().remove(0);
            }
          }
        }

        seriesArray[0].getData().add(new Data<Long, Number>(milliseconds, dispersion.median()));
        seriesArray[1].getData().add(new Data<Long, Number>(milliseconds, dispersion.percentile_75() - dispersion.median()));
        seriesArray[2].getData().add(new Data<Long, Number>(milliseconds, dispersion.percentile_95() - dispersion.percentile_75()));
        seriesArray[3].getData().add(new Data<Long, Number>(milliseconds, dispersion.percentile_98() - dispersion.percentile_95()));
        seriesArray[4].getData().add(new Data<Long, Number>(milliseconds, dispersion.percentile_99() - dispersion.percentile_98()));
        seriesArray[5].getData().add(new Data<Long, Number>(milliseconds, dispersion.percentile_999() - dispersion.percentile_99()));
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
