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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.smallmind.claxon.registry.feature.Feature;
import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.MeterBuilder;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Central hub of the Claxon metrics system that manages the full lifecycle of meters,
 * emitters, and periodic metric collection.
 *
 * <p>On construction, the registry starts a daemon background thread (the
 * {@link CollectionWorker}) that wakes on the cadence defined by
 * {@link ClaxonConfiguration#getCollectionStint()}, sweeps tracked objects, collects
 * readings from every active {@link Feature} and {@link Meter}, and delivers those
 * readings to each registered {@link Emitter}.
 *
 * <p>Meters are keyed by the combination of the requesting class and its tag set.
 * The first call to {@link #register} for a given key constructs the meter lazily
 * using the supplied {@link MeterBuilder}; subsequent calls return the cached instance.
 * When the configured {@link NamingStrategy} cannot produce a name for a caller, the
 * registry silently substitutes a {@link NoOpMeter} and remembers that decision so the
 * strategy is not consulted again for the same key.
 */
public class ClaxonRegistry {

  /**
   * Thread-safe map of named emitters bound to this registry.
   */
  private final ConcurrentHashMap<String, Emitter> emitterMap = new ConcurrentHashMap<>();

  /**
   * Thread-safe map of active meters keyed by caller class and tags.
   */
  private final ConcurrentHashMap<RegistryKey, NamedMeter<? extends Meter>> meterMap = new ConcurrentHashMap<>();

  /**
   * Set of keys for which the naming strategy returned {@code null}; these always resolve to {@link NoOpMeter}.
   */
  private final Set<RegistryKey> noopSet = ConcurrentHashMap.newKeySet();

  /**
   * Tracker that maintains weak references to measured objects and keeps their meters updated.
   */
  private final MeasurableTracker measurableTracker;

  /**
   * Background thread that periodically collects and emits meter readings.
   */
  private final CollectionWorker collectionWorker;

  /**
   * Immutable configuration supplied at construction time.
   */
  private final ClaxonConfiguration configuration;

  /**
   * Constructs a registry using the supplied configuration and immediately starts the
   * background collection daemon thread.
   *
   * @param configuration the registry configuration controlling timing, tags, and naming
   */
  public ClaxonRegistry (ClaxonConfiguration configuration) {

    Thread workerThread = new Thread(collectionWorker = new CollectionWorker());

    this.configuration = configuration;

    measurableTracker = new MeasurableTracker(this);

    workerThread.setDaemon(true);
    workerThread.start();
  }

  /**
   * Registers this registry as the per-application instrumentation source so that
   * static calls through {@link Instrument} resolve to this instance.
   */
  public void initializeInstrumentation () {

    Instrument.register(this);
  }

  /**
   * Returns the configuration that governs this registry's behaviour.
   *
   * @return the {@link ClaxonConfiguration} supplied at construction time
   */
  public ClaxonConfiguration getConfiguration () {

    return configuration;
  }

  /**
   * Signals the background collection worker to stop and blocks until it has exited.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for
   *                              the worker to finish
   */
  public void stop ()
    throws InterruptedException {

    collectionWorker.stop();
  }

  /**
   * Returns the {@link Emitter} previously bound under the given name, or {@code null}
   * if no emitter is bound to that name.
   *
   * @param name the logical name used when the emitter was bound
   * @return the bound {@link Emitter}, or {@code null}
   */
  public Emitter getEmitter (String name) {

    return emitterMap.get(name);
  }

  /**
   * Binds an {@link Emitter} to this registry under a logical name.
   *
   * <p>If an emitter is already bound under the same name it is replaced. Bound emitters
   * receive readings from every meter and feature on each collection interval.
   *
   * @param name    the logical name to associate with the emitter
   * @param emitter the emitter to bind
   * @return this registry instance to support fluent chaining
   */
  public ClaxonRegistry bind (String name, Emitter emitter) {

    emitterMap.put(name, emitter);

    return this;
  }

  /**
   * Registers or retrieves the {@link Meter} for the given caller class and tag combination.
   *
   * <p>If the key is new, the registry consults the configured {@link NamingStrategy} to
   * derive a name. When the strategy returns {@code null} the key is added to an internal
   * no-op set and a {@link NoOpMeter} is returned immediately for all future calls with
   * the same key. Otherwise the meter is constructed lazily from {@code builder} and cached.
   *
   * @param caller  the class requesting the meter, used for name derivation
   * @param builder builder that constructs the concrete {@link Meter} on first access
   * @param tags    tags that, together with {@code caller}, uniquely identify this meter
   * @return the active {@link Meter} for the key, or a {@link NoOpMeter} when no name
   * can be derived
   */
  public Meter register (Class<?> caller, MeterBuilder<? extends Meter> builder, Tag... tags) {

    RegistryKey key = new RegistryKey(caller, tags);

    if (noopSet.contains(key)) {

      return NoOpMeter.instance();
    } else {

      NamedMeter<? extends Meter> namedMeter;

      if ((namedMeter = meterMap.get(key)) == null) {

        String meterName;

        if ((meterName = configuration.getNamingStrategy().from(caller)) == null) {
          noopSet.add(key);

          return NoOpMeter.instance();
        } else {

          NamedMeter<? extends Meter> previousNamedMeter;

          if ((previousNamedMeter = meterMap.putIfAbsent(key, namedMeter = new NamedMeter<>(meterName, builder))) != null) {

            return previousNamedMeter.getMeter();
          } else {

            return namedMeter.getMeter();
          }
        }
      } else {

        return namedMeter.getMeter();
      }
    }
  }

  /**
   * Removes the meter registered for the given caller class and tag combination, if any.
   *
   * @param caller the class whose meter should be removed
   * @param tags   the tags that identify the specific meter to remove
   */
  public void unregister (Class<?> caller, Tag... tags) {

    meterMap.remove(new RegistryKey(caller, tags));
  }

  /**
   * Begins tracking a weakly referenced measured object by registering a meter on its behalf
   * and arranging for the meter to receive updated measurements on every collection interval.
   *
   * <p>The object is held via a {@link java.lang.ref.WeakReference}. Once the object is
   * garbage-collected, the associated meter is automatically unregistered on the next sweep.
   *
   * @param caller      the class requesting the meter, used for name derivation
   * @param builder     builder that constructs the backing {@link Meter}
   * @param measured    the domain object to be tracked
   * @param measurement a function that extracts a {@code long} measurement from {@code measured}
   * @param tags        tags that identify the meter
   * @param <T>         the type of the measured object
   * @return {@code measured}, unchanged, to support fluent assignment
   */
  public <T> T track (Class<?> caller, MeterBuilder<? extends Meter> builder, T measured, Function<T, Long> measurement, Tag... tags) {

    return measurableTracker.track(caller, builder, measured, measurement, tags);
  }

  /**
   * Background daemon that wakes periodically to sweep tracked objects, collect feature
   * readings, collect meter readings, and forward everything to each bound emitter.
   *
   * <p>Exceptions thrown by individual emitters are caught, logged, and do not interrupt
   * the collection loop. An {@link InterruptedException} from the sleep-wait is logged and
   * causes the worker to exit cleanly.
   */
  private class CollectionWorker implements Runnable {

    /**
     * Counting latch used to request a graceful shutdown; counted down to zero to signal stop.
     */
    private final CountDownLatch finishLatch = new CountDownLatch(1);

    /**
     * Counting latch used by {@link #stop()} to wait until the worker thread has fully exited.
     */
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    /**
     * Requests a graceful shutdown of the collection worker and blocks until the worker
     * thread has exited.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

    /**
     * Main loop of the collection worker. On each iteration the worker:
     * <ol>
     *   <li>Sweeps garbage-collected tracked objects and updates surviving ones.</li>
     *   <li>Collects {@link Quantity} readings from every configured {@link Feature}.</li>
     *   <li>Collects {@link Quantity} readings from every registered {@link Meter}.</li>
     *   <li>Delivers all non-empty reading sets to every bound {@link Emitter}.</li>
     * </ol>
     * Emitter-level exceptions are logged individually and do not abort the loop.
     * An {@link InterruptedException} from {@link CountDownLatch#await} causes the
     * loop to exit and the exit latch to be released.
     */
    @Override
    public void run () {

      try {
        while (!finishLatch.await(configuration.getCollectionStint().getTime(), configuration.getCollectionStint().getTimeUnit())) {

          Feature[] features;

          measurableTracker.sweepAndUpdate();

          if ((features = configuration.getFeatures()) != null) {
            for (Feature feature : features) {

              Quantity[] quantities = feature.record();

              if ((quantities != null) && (quantities.length > 0)) {
                for (Emitter emitter : emitterMap.values()) {
                  try {
                    emitter.record(feature.getName(), configuration.calculateTags(feature.getName(), feature.getTags()), quantities);
                  } catch (Exception exception) {
                    LoggerManager.getLogger(ClaxonRegistry.class).error(exception);
                  }
                }
              }
            }
          }

          for (Map.Entry<RegistryKey, NamedMeter<? extends Meter>> namedMeterEntry : meterMap.entrySet()) {

            Quantity[] quantities = namedMeterEntry.getValue().getMeter().record();

            if ((quantities != null) && (quantities.length > 0)) {
              for (Emitter emitter : emitterMap.values()) {
                try {
                  emitter.record(namedMeterEntry.getValue().getName(), configuration.calculateTags(namedMeterEntry.getValue().getName(), namedMeterEntry.getKey().tags()), quantities);
                } catch (Exception exception) {
                  LoggerManager.getLogger(ClaxonRegistry.class).error(exception);
                }
              }
            }
          }
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ClaxonRegistry.class).error(interruptedException);
        finishLatch.countDown();
      } finally {
        exitLatch.countDown();
      }
    }
  }

  /**
   * Associates a derived meter name with a lazily constructed {@link Meter} instance.
   *
   * <p>The meter is created on the first call to {@link #getMeter()} using double-checked
   * locking to ensure only one instance is ever built per key.
   *
   * @param <M> the concrete {@link Meter} type produced by the builder
   */
  private class NamedMeter<M extends Meter> {

    /**
     * The derived string name for the meter, used when delivering readings to emitters.
     */
    private final String name;

    /**
     * Builder responsible for constructing the meter instance on first access.
     */
    private final MeterBuilder<M> builder;

    /**
     * Atomic reference that caches the lazily constructed meter.
     */
    private final AtomicReference<M> meterRef = new AtomicReference<>();

    /**
     * Creates a named-meter wrapper for the supplied name and builder.
     *
     * @param name    the derived meter name
     * @param builder the builder that will construct the meter on first access
     */
    public NamedMeter (String name, MeterBuilder<M> builder) {

      this.name = name;
      this.builder = builder;
    }

    /**
     * Returns the derived string name of the meter.
     *
     * @return the meter name
     */
    public String getName () {

      return name;
    }

    /**
     * Returns the {@link Meter} instance, constructing it on the first call using the
     * associated builder and caching it for all subsequent calls.
     *
     * @return the constructed and cached meter instance
     */
    public M getMeter () {

      M meter;

      if ((meter = meterRef.get()) == null) {
        synchronized (meterRef) {
          if ((meter = meterRef.get()) == null) {
            meterRef.set(meter = builder.build(configuration.getClock()));
          }
        }
      }

      return meter;
    }
  }

  /**
   * Immutable composite key that uniquely identifies a meter registration by the
   * requesting caller class and its associated tag set.
   *
   * <p>Equality and hashing are defined over both the caller class identity and the
   * ordered contents of the tag array so that the same class registered with different
   * tag sets produces distinct entries in the meter map.
   *
   * @param caller the class that registered the meter
   * @param tags   the tags associated with the meter registration
   */
  private record RegistryKey(Class<?> caller, Tag... tags) {

    /**
     * Computes a hash code based on the caller class identity and the tag array contents.
     *
     * @return the combined hash code
     */
    @Override
    public int hashCode () {

      return (caller.hashCode() * 31) + ((tags == null) ? 0 : Arrays.hashCode(tags));
    }

    /**
     * Returns {@code true} when {@code obj} is a {@link RegistryKey} with the same caller
     * class and an equal tag array.
     *
     * @param obj the object to compare against this key
     * @return {@code true} if caller and tags are equal; {@code false} otherwise
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof RegistryKey) && ((RegistryKey)obj).caller().equals(caller) && Arrays.equals(((RegistryKey)obj).tags(), tags);
    }
  }
}
