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
 * Maintains a set of weakly referenced domain objects and keeps their associated
 * {@link Meter} readings current across collection intervals.
 *
 * <p>Each tracked object is held via a {@link WeakReference} so that the tracker does not
 * prevent garbage collection. A {@link ReferenceQueue} is used to detect when an object
 * has been collected; the corresponding meter is then unregistered from the
 * {@link ClaxonRegistry} on the next call to {@link #sweepAndUpdate()}.
 *
 * <p>For every object that is still reachable, {@link #sweepAndUpdate()} applies the
 * registered measurement function and pushes the result into the backing meter via
 * {@link ClaxonRegistry#register}.
 */
public class MeasurableTracker {

  /**
   * Maps each weak reference to the metadata needed to update or unregister its meter.
   */
  private final ConcurrentHashMap<Reference<?>, Measurable> measurableMap = new ConcurrentHashMap<>();

  /**
   * Queue into which the JVM enqueues weak references after their referents are collected.
   */
  private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();

  /**
   * Registry used to register and unregister meters on behalf of tracked objects.
   */
  private final ClaxonRegistry registry;

  /**
   * Creates a tracker that delegates meter registration and unregistration to the
   * given registry.
   *
   * @param registry the {@link ClaxonRegistry} that manages meter instances for tracked objects
   */
  public MeasurableTracker (ClaxonRegistry registry) {

    this.registry = registry;
  }

  /**
   * Begins tracking {@code measured} by registering a weak reference to it and
   * associating the reference with the metadata needed to update and unregister its meter.
   *
   * @param caller      the class that is requesting the meter, used for name derivation
   * @param builder     the builder used to construct the backing {@link Meter}
   * @param measured    the domain object to track; held via a {@link WeakReference}
   * @param measurement a function that derives a {@code long} measurement from {@code measured}
   * @param tags        tags that identify the meter in the registry
   * @param <T>         the type of the domain object
   * @return {@code measured}, unchanged, to support fluent assignment patterns
   */
  public <T> T track (Class<?> caller, MeterBuilder<? extends Meter> builder, T measured, Function<T, Long> measurement, Tag... tags) {

    measurableMap.put(new WeakReference<>(measured, referenceQueue), new Measurable(caller, builder, measurement, tags));

    return measured;
  }

  /**
   * Performs a single maintenance pass over all tracked objects:
   * <ol>
   *   <li>Drains the {@link ReferenceQueue}, unregistering the meter for each
   *       garbage-collected object.</li>
   *   <li>For every object that is still reachable, computes a fresh measurement and
   *       pushes it into the object's meter via {@link ClaxonRegistry#register}.</li>
   * </ol>
   *
   * <p>This method is called by the registry's background collection worker on each
   * collection interval.
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

  /**
   * Captures all metadata needed to update and eventually unregister a meter on behalf
   * of a single tracked domain object.
   */
  private static class Measurable {

    /**
     * Builder used to construct or look up the meter for this object.
     */
    private final MeterBuilder<? extends Meter> builder;

    /**
     * Tags associated with the meter registration.
     */
    private final Tag[] tags;

    /**
     * Measurement function that extracts a {@code long} value from the tracked object.
     * The generic wildcard is erased to {@code Object} so that a single field can hold
     * functions over arbitrary domain types.
     */
    private final Function<Object, Long> measurement;

    /**
     * Class that originally requested the meter, used for name derivation.
     */
    private final Class<?> caller;

    /**
     * Constructs a {@code Measurable} capturing the caller, builder, measurement function,
     * and tags for a tracked domain object.
     *
     * @param caller      the class requesting the meter
     * @param builder     the meter builder
     * @param measurement a function that extracts a {@code long} measurement from the tracked object
     * @param tags        the tags associated with the meter
     */
    public Measurable (Class<?> caller, MeterBuilder<? extends Meter> builder, Function<?, Long> measurement, Tag... tags) {

      this.caller = caller;
      this.builder = builder;
      this.tags = tags;
      this.measurement = (Function<Object, Long>)measurement;
    }

    /**
     * Returns the class that originally requested the meter, used for name derivation
     * by the configured {@link NamingStrategy}.
     *
     * @return the requesting caller class
     */
    public Class<?> getCaller () {

      return caller;
    }

    /**
     * Returns the tags associated with the meter registration for the tracked object.
     *
     * @return the meter's tag array
     */
    public Tag[] getTags () {

      return tags;
    }

    /**
     * Returns the {@link MeterBuilder} used to construct or look up the meter for the
     * tracked object.
     *
     * @return the meter builder
     */
    public MeterBuilder<? extends Meter> getBuilder () {

      return builder;
    }

    /**
     * Returns the measurement function that derives a {@code long} value from the
     * tracked domain object on each collection interval.
     *
     * @return the measurement function, typed as {@code Function<Object, Long>}
     */
    public Function<Object, Long> getMeasurement () {

      return measurement;
    }
  }
}
