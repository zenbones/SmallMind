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
package org.smallmind.claxon.registry;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.smallmind.claxon.registry.meter.MeterBuilder;

public class MeasurableTracker {

  private final ConcurrentHashMap<Reference<?>, Measurable> measurableMap = new ConcurrentHashMap<>();
  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
  private final ClaxonRegistry registry;

  public MeasurableTracker (ClaxonRegistry registry) {

    this.registry = registry;
  }

  public <T> T track (Class<?> caller, MeterBuilder<?> builder, T measured, Function<T, Long> measurement, Tag... tags) {

    measurableMap.put(new WeakReference<>(measured, referenceQueue), new Measurable(caller, builder, measurement, tags));

    return measured;
  }

  public void sweepAndUpdate () {

    Reference<?> sweptReference;

    if ((sweptReference = referenceQueue.poll()) != null) {

      Measurable measurable;

      if ((measurable = measurableMap.remove(sweptReference)) != null) {
        registry.unregister(measurable.getCaller(), measurable.getTags());
      }
    }

    for (Map.Entry<Reference<?>, Measurable> measurableEntry : measurableMap.entrySet()) {

      Object measured;

      if ((measured = measurableEntry.getKey().get()) != null) {
        registry.register(measurableEntry.getValue().getCaller(), measurableEntry.getValue().getBuilder(), measurableEntry.getValue().getTags()).update(measurableEntry.getValue().getMeasurement().apply(measured));
      }
    }
  }

  private static class Measurable {

    private MeterBuilder<?> builder;
    private Tag[] tags;
    private Function<Object, Long> measurement;
    private Class<?> caller;

    public Measurable (Class<?> caller, MeterBuilder<?> builder, Function<?, Long> measurement, Tag... tags) {

      this.caller = caller;
      this.builder = builder;
      this.tags = tags;
      this.measurement = (Function<Object, Long>)measurement;
    }

    public Class<?> getCaller () {

      return caller;
    }

    public Tag[] getTags () {

      return tags;
    }

    public MeterBuilder<?> getBuilder () {

      return builder;
    }

    public Function<Object, Long> getMeasurement () {

      return measurement;
    }
  }
}
