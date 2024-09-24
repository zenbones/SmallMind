/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class ClaxonRegistry {

  private final ConcurrentHashMap<String, Emitter> emitterMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<RegistryKey, NamedMeter<? extends Meter>> meterMap = new ConcurrentHashMap<>();
  private final Set<RegistryKey> noopSet = ConcurrentHashMap.newKeySet();
  private final MeasurableTracker measurableTracker;
  private final CollectionWorker collectionWorker;
  private final ClaxonConfiguration configuration;

  public ClaxonRegistry (ClaxonConfiguration configuration) {

    Thread workerThread = new Thread(collectionWorker = new CollectionWorker());

    this.configuration = configuration;

    measurableTracker = new MeasurableTracker(this);

    workerThread.setDaemon(true);
    workerThread.start();
  }

  public void initializeInstrumentation () {

    Instrument.register(this);
  }

  public ClaxonConfiguration getConfiguration () {

    return configuration;
  }

  public void stop ()
    throws InterruptedException {

    collectionWorker.stop();
  }

  public Emitter getEmitter (String name) {

    return emitterMap.get(name);
  }

  public ClaxonRegistry bind (String name, Emitter emitter) {

    emitterMap.put(name, emitter);

    return this;
  }

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

          if ((previousNamedMeter = meterMap.putIfAbsent(key, namedMeter = new NamedMeter<>(meterName, builder, configuration.getClock()))) != null) {

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

  public void unregister (Class<?> caller, Tag... tags) {

    meterMap.remove(new RegistryKey(caller, tags));
  }

  public <T> T track (Class<?> caller, MeterBuilder<? extends Meter> builder, T measured, Function<T, Long> measurement, Tag... tags) {

    return measurableTracker.track(caller, builder, measured, measurement, tags);
  }

  private class CollectionWorker implements Runnable {

    private final CountDownLatch finishLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

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

  private record RegistryKey(Class<?> caller, Tag... tags) {

    @Override
    public int hashCode () {

      return (caller.hashCode() * 31) + ((tags == null) ? 0 : Arrays.hashCode(tags));
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof RegistryKey) && ((RegistryKey)obj).caller().equals(caller) && Arrays.equals(((RegistryKey)obj).tags(), tags);
    }
  }

  private static class NamedMeter<M extends Meter> {

    private final String name;
    private final MeterBuilder<M> builder;
    private final Clock clock;
    private final AtomicReference<M> meterRef = new AtomicReference<>();

    public NamedMeter (String name, MeterBuilder<M> builder, Clock clock) {

      this.name = name;
      this.builder = builder;
      this.clock = clock;
    }

    public String getName () {

      return name;
    }

    public M getMeter () {

      M meter;

      if ((meter = meterRef.get()) == null) {
        synchronized (meterRef) {
          if ((meter = meterRef.get()) == null) {
            meterRef.set(meter = builder.build(clock));
          }
        }
      }

      return meter;
    }
  }
}
