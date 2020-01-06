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
package org.smallmind.instrument;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Uses Vitter's Algorithm R to produce a statistically representative sample.
 */
public class UniformSample implements Sample {

  private static final int BITS_PER_LONG = 63;

  private final AtomicLong count = new AtomicLong();
  private final AtomicLongArray values;

  public UniformSample (int reservoirSize) {

    this.values = new AtomicLongArray(reservoirSize);
    clear();
  }

  @Override
  public Samples getType () {

    return Samples.UNIFORM;
  }

  @Override
  public void clear () {

    for (int i = 0; i < values.length(); i++) {
      values.set(i, 0);
    }

    count.set(0);
  }

  @Override
  public int size () {

    long currentCount = count.get();

    return (currentCount > values.length()) ? values.length() : (int)currentCount;
  }

  @Override
  public void update (long value) {

    long updatedCount = count.incrementAndGet();

    if (updatedCount <= values.length()) {
      values.set((int)updatedCount - 1, value);
    } else {

      long randomLong = nextLong(updatedCount);

      if (randomLong < values.length()) {
        values.set((int)randomLong, value);
      }
    }
  }

  private long nextLong (long n) {

    long bits;
    long val;

    do {
      bits = ThreadLocalRandom.current().nextLong() & (~(1L << BITS_PER_LONG));
    } while (bits - (val = bits % n) + (n - 1) < 0L);

    return val;
  }

  @Override
  public Statistics getStatistics () {

    int currentSize = size();
    List<Long> copy = new ArrayList<Long>(currentSize);

    for (int i = 0; i < currentSize; i++) {
      copy.add(values.get(i));
    }

    return new Statistics(copy);
  }
}
