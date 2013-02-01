/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.instrument;

import java.util.Arrays;
import java.util.Collection;

public class Snapshot implements Ranking {

  private static final double MEDIAN_Q = 0.5;
  private static final double P75_Q = 0.75;
  private static final double P95_Q = 0.95;
  private static final double P98_Q = 0.98;
  private static final double P99_Q = 0.99;
  private static final double P999_Q = 0.999;

  private final double[] values;

  public Snapshot (Collection<Long> values) {

    Object[] copy = values.toArray();

    this.values = new double[copy.length];
    for (int i = 0; i < copy.length; i++) {
      this.values[i] = (Long)copy[i];
    }

    Arrays.sort(this.values);
  }

  public Snapshot (double[] values) {

    this.values = new double[values.length];
    System.arraycopy(values, 0, this.values, 0, values.length);
    Arrays.sort(this.values);
  }

  public double getValue (double quantile) {

    if (quantile < 0.0 || quantile > 1.0) {
      throw new IllegalArgumentException(quantile + " is not in [0..1]");
    }

    if (values.length == 0) {
      return 0.0;
    }

    final double pos = quantile * (values.length + 1);

    if (pos < 1) {
      return values[0];
    }

    if (pos >= values.length) {
      return values[values.length - 1];
    }

    final double lower = values[(int)pos - 1];
    final double upper = values[(int)pos];
    return lower + (pos - Math.floor(pos)) * (upper - lower);
  }

  public int size () {

    return values.length;
  }

  public double getMedian () {

    return getValue(MEDIAN_Q);
  }

  public double get75thPercentile () {

    return getValue(P75_Q);
  }

  public double get95thPercentile () {

    return getValue(P95_Q);
  }

  public double get98thPercentile () {

    return getValue(P98_Q);
  }

  public double get99thPercentile () {

    return getValue(P99_Q);
  }

  public double get999thPercentile () {

    return getValue(P999_Q);
  }

  public double[] getValues () {

    return Arrays.copyOf(values, values.length);
  }
}
