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
package org.smallmind.javafx.extras.instrument;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;

public class VelocityChart extends LineChart<Long, Number> {

  private static final String[] SERIES_NAMES = new String[] {"Inception", "15 minute", "5 minute", "1 minute"};
  private final TimeAxis timeAxis;
  private final Series<Long, Number>[] seriesArray = new Series[4];
  private final AtomicBoolean hasData = new AtomicBoolean(false);

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

  public BooleanProperty pausedProperty () {

    return timeAxis.pausedProperty();
  }

  public boolean isPaused () {

    return timeAxis.getPaused();
  }

  public void setPaused (boolean paused) {

    timeAxis.setPaused(paused);
  }

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

        seriesArray[0].getData().add(new Data<Long, Number>(milliseconds, blur.getAvgVelocity()));
        seriesArray[1].getData().add(new Data<Long, Number>(milliseconds, blur.getAvgVelocity_15()));
        seriesArray[2].getData().add(new Data<Long, Number>(milliseconds, blur.getAvgVelocity_5()));
        seriesArray[3].getData().add(new Data<Long, Number>(milliseconds, blur.getAvgVelocity_1()));
      }
    });
  }

  public void stop () {

    timeAxis.stop();
  }
}
