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
package org.smallmind.instrument.micrometer.statistic;

import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.util.MutationUtility;

public class Averaged extends AbstractStatistic<Double[], DoubleHolder[]> {

  private final ExponentiallyWeightedMovingAverage[] movingAverages;

  public Averaged () {

    this(null, TimeUnit.MINUTES, 1, 5, 15);
  }

  public Averaged (String name) {

    this(name, TimeUnit.MINUTES, 1, 5, 15);
  }

  public Averaged (TimeUnit windowTimeUnit, long... windowTimes) {

    this(null, windowTimeUnit, windowTimes);
  }

  public Averaged (String name, TimeUnit windowTimeUnit, long... windowTimes) {

    super(name);

    int index = 0;

    movingAverages = new ExponentiallyWeightedMovingAverage[windowTimes.length];
    for (long averagedTime : windowTimes) {
      movingAverages[index++] = new ExponentiallyWeightedMovingAverage(averagedTime, windowTimeUnit);
    }
  }

  @Override
  public Double[] get () {

    return MutationUtility.toArray(movingAverages, double.class, ExponentiallyWeightedMovingAverage::getMovingAverage);
  }

  @Override
  public void get (DoubleHolder[] holders) {

    int index = 0;

    for (DoubleHolder holder : holders) {
      holder.set(movingAverages[index].getMovingAverage());
    }
  }

  public double get (int index) {

    return movingAverages[index].getMovingAverage();
  }

  @Override
  public void update (long value) {

    for (ExponentiallyWeightedMovingAverage movingAverage : movingAverages) {
      movingAverage.update(value);
    }
  }
}
