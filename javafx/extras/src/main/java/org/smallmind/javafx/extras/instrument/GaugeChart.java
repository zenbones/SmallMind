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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;

public class GaugeChart extends LineChart<Long, Number> {

  private static final String[] SERIES_NAMES = new String[] {"Inception", "15 minute", "5 minute", "1 minute"};
  private final TimeAxis timeAxis;
  private final Series<Long, Number>[] seriesArray = new Series[4];
  private final AtomicBoolean hasData = new AtomicBoolean(false);

  public GaugeChart (long spanInMilliseconds) {

    this(new TimeAxis(spanInMilliseconds));
  }

  private GaugeChart (TimeAxis timeAxis) {

    super(timeAxis, new NumberAxis());

    this.timeAxis = timeAxis;

    for (int index = 0; index < seriesArray.length; index++) {
      seriesArray[index] = new Series<Long, Number>();
      seriesArray[index].setName(SERIES_NAMES[index]);
    }

    getStyleClass().add("gauge-chart");
    getStylesheets().add(GaugeChart.class.getResource("GaugeChart.css").toExternalForm());
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

  public void addMeasure (final long milliseconds, final Measure measure) {

    Platform.runLater(new Runnable() {

      @Override
      public void run () {

        if (hasData.compareAndSet(false, true)) {
          getData().addAll(Arrays.asList(seriesArray));
        }

        long minTime = (long)GaugeChart.this.timeAxis.getLowerBound();

        for (Series<Long, Number> series : getData()) {
          if (!series.getData().isEmpty()) {
            while (series.getData().get(0).getXValue() < minTime) {
              series.getData().remove(0);
            }
          }
        }

        seriesArray[0].getData().add(new Data<Long, Number>(milliseconds, measure.getAvgRate()));
        seriesArray[1].getData().add(new Data<Long, Number>(milliseconds, measure.getAvgRate_15()));
        seriesArray[2].getData().add(new Data<Long, Number>(milliseconds, measure.getAvgRate_5()));
        seriesArray[3].getData().add(new Data<Long, Number>(milliseconds, measure.getAvgRate_1()));
      }
    });
  }

  public void stop () {

    timeAxis.stop();
  }
}
