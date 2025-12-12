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
package org.smallmind.claxon.registry;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.MeterBuilder;

/**
 * Tracks weakly referenced measured objects and updates meters while removing entries when the objects are collected.
 */
public class MeasurableTracker {

  private final ConcurrentHashMap<Reference<?>, Measurable> measurableMap = new ConcurrentHashMap<>();
  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
  private final ClaxonRegistry registry;

  /**
   * Creates a tracker tied to a registry.
   *
   * @param registry registry used to register and unregister meters for tracked objects
   */
  public MeasurableTracker (ClaxonRegistry registry) {

    this.registry = registry;
  }

  /**
   * Starts tracking a measured object with a builder and measurement function.
   *
   * @param caller      calling class for naming
   * @param builder     meter builder to construct the meter
   * @param measured    object being tracked
   * @param measurement function to compute a measurement from the object
   * @param tags        tags associated with the meter
   * @param <T>         measured type
   * @return the measured object for fluent usage
   */
  public <T> T track (Class<?> caller, MeterBuilder<? extends Meter> builder, T measured, Function<T, Long> measurement, Tag... tags) {

    measurableMap.put(new WeakReference<>(measured, referenceQueue), new Measurable(caller, builder, measurement, tags));

    return measured;
  }

  /**
   * Removes meters whose measured objects have been collected and updates remaining meters with fresh measurements.
   */
  public void sweepAndUpdate () {

    Reference<?> sweptReference;

    while ((sweptReference = referenceQueue.poll()) != null) {

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

    private final MeterBuilder<? extends Meter> builder;
    private final Tag[] tags;
    private final Function<Object, Long> measurement;
    private final Class<?> caller;

    /**
     * Captures measurement metadata for a tracked object.
     *
     * @param caller      calling class for naming
     * @param builder     meter builder to create meters
     * @param measurement measurement function applied to the tracked object
     * @param tags        tags associated with the meter
     */
    public Measurable (Class<?> caller, MeterBuilder<? extends Meter> builder, Function<?, Long> measurement, Tag... tags) {

      this.caller = caller;
      this.builder = builder;
      this.tags = tags;
      this.measurement = (Function<Object, Long>)measurement;
    }

    /**
     * @return the calling class used for meter naming
     */
    public Class<?> getCaller () {

      return caller;
    }

    /**
     * @return tags associated with the tracked meter
     */
    public Tag[] getTags () {

      return tags;
    }

    /**
     * @return builder used to create the meter
     */
    public MeterBuilder<? extends Meter> getBuilder () {

      return builder;
    }

    /**
     * @return function used to compute measurements
     */
    public Function<Object, Long> getMeasurement () {

      return measurement;
    }
  }
}
