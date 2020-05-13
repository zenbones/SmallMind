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

import java.util.Arrays;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.MeterBuilder;
import org.smallmind.nutsnbolts.util.DotNotation;
import org.smallmind.scribe.pen.LoggerManager;

public class ClaxonRegistry {

  private final ConcurrentHashMap<String, Collector> collectorMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<RegistryKey, NamedMeter<?>> meterMap = new ConcurrentHashMap<>();
  private final Set<RegistryKey> noopSet = ConcurrentHashMap.newKeySet();
  private final MeasurableTracker measurableTrcker;
  private final ObservableTracker observableTracker;
  private final CollectionWorker collectionWorker;
  private final ClaxonConfiguration configuration;

  public ClaxonRegistry () {

    this(new ClaxonConfiguration());
  }

  public ClaxonRegistry (ClaxonConfiguration configuration) {

    Thread workerThread = new Thread(collectionWorker = new CollectionWorker());

    this.configuration = configuration;

    measurableTrcker = new MeasurableTracker(this);
    observableTracker = new ObservableTracker(this);

    workerThread.setDaemon(true);
    workerThread.start();
  }

  public void asInstrumentRegistry () {

    Instrument.register(this);
  }

  public void stop ()
    throws InterruptedException {

    collectionWorker.stop();
  }

  public Collector getCollector (String name) {

    return collectorMap.get(name);
  }

  public ClaxonRegistry bind (String name, Collector collector) {

    collectorMap.put(name, collector);

    return this;
  }

  public <M extends Meter> M register (Class<?> caller, MeterBuilder<M> builder, Tag... tags) {

    if (configuration.getPrefixMap() == null) {

      return NoOpMeter.instance();
    } else {

      RegistryKey key = new RegistryKey(caller, tags);

      if (noopSet.contains(key)) {

        return NoOpMeter.instance();
      } else {

        NamedMeter<?> namedMeter;

        if ((namedMeter = meterMap.get(key)) == null) {

          String className = caller.getName();
          Map.Entry<DotNotation, String> strongestEntry = null;
          int currentStrength = 0;

          for (Map.Entry<DotNotation, String> prefixEntry : configuration.getPrefixMap().entrySet()) {

            int possibleStrength;

            if ((possibleStrength = prefixEntry.getKey().calculateValue(className, -1)) > currentStrength) {
              currentStrength = possibleStrength;
              strongestEntry = prefixEntry;
            }
          }

          if (strongestEntry == null) {
            noopSet.add(key);

            return NoOpMeter.instance();
          } else {

            NamedMeter<?> previousNamedMeter;

            if ((previousNamedMeter = meterMap.putIfAbsent(key, namedMeter = new NamedMeter<M>(strongestEntry.getValue(), builder.build(configuration.getClock())))) != null) {
              namedMeter = previousNamedMeter;
            }
          }
        }

        return (M)namedMeter.getMeter();
      }
    }
  }

  public void unregister (Class<?> clazz, Tag... tags) {

    meterMap.remove(new RegistryKey(clazz, tags));
  }

  public <O extends Observable> O track (Class<?> caller, MeterBuilder<?> builder, O observable, Tag... tags) {

    return observableTracker.track(caller, builder, observable, tags);
  }

  public <T> T track (Class<?> caller, MeterBuilder<?> builder, T measured, Function<T, Long> measurement, Tag... tags) {

    return measurableTrcker.track(caller, builder, measured, measurement, tags);
  }

  public void instrument (Meter meter, long value) {

    meter.update(value);
  }

  public void instrument (Meter meter, TimeUnit timeUnit, SansResultExecutable sansResultExecutable)
    throws Throwable {

    long start = configuration.getClock().monotonicTime();

    sansResultExecutable.execute();
    meter.update(timeUnit.convert(configuration.getClock().monotonicTime() - start, TimeUnit.NANOSECONDS));
  }

  public <T> T instrument (Meter meter, TimeUnit timeUnit, WithResultExecutable<T> withResultExecutable)
    throws Throwable {

    T result;
    long start = configuration.getClock().monotonicTime();

    result = withResultExecutable.execute();
    meter.update(timeUnit.convert(configuration.getClock().monotonicTime() - start, TimeUnit.NANOSECONDS));

    return result;
  }

  public void record () {

    measurableTrcker.sweepAndUpdate();
    observableTracker.sweep();

    for (Map.Entry<RegistryKey, NamedMeter<?>> namedMeterEntry : meterMap.entrySet()) {

      Quantity[] quantities = namedMeterEntry.getValue().getMeter().record();

      if ((quantities != null) && (quantities.length > 0)) {

        Tag[] mergedTags;
        Tag[] meterTags = namedMeterEntry.getKey().getTags();

        if ((configuration.getRegistryTags() == null) || (configuration.getRegistryTags().length == 0)) {
          mergedTags = meterTags;
        } else if ((meterTags == null) || (meterTags.length == 0)) {
          mergedTags = configuration.getRegistryTags();
        } else {
          mergedTags = new Tag[configuration.getRegistryTags().length + meterTags.length];
          System.arraycopy(configuration.getRegistryTags(), 0, mergedTags, 0, configuration.getRegistryTags().length);
          System.arraycopy(meterTags, 0, mergedTags, configuration.getRegistryTags().length, meterTags.length);
        }

        for (Collector collector : collectorMap.values()) {
          try {
            collector.record(namedMeterEntry.getValue().getName(), mergedTags, quantities);
          } catch (Exception exception) {
            LoggerManager.getLogger(ClaxonRegistry.class).error(exception);
          }
        }
      }
    }
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
          record();
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ClaxonRegistry.class).error(interruptedException);
        finishLatch.countDown();
      } finally {
        exitLatch.countDown();
      }
    }
  }

  private static class RegistryKey {

    private final Class<?> caller;
    private final Tag[] tags;

    public RegistryKey (Class<?> caller, Tag... tags) {

      this.caller = caller;
      this.tags = tags;
    }

    public Class<?> getCaller () {

      return caller;
    }

    public Tag[] getTags () {

      return tags;
    }

    @Override
    public int hashCode () {

      return (caller.hashCode() * 31) + ((tags == null) ? 0 : Arrays.hashCode(tags));
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof RegistryKey) && ((RegistryKey)obj).getCaller().equals(caller) && Arrays.equals(((RegistryKey)obj).getTags(), tags);
    }
  }

  private static class NamedMeter<M extends Meter> {

    private final String name;
    private final M meter;

    public NamedMeter (String name, M meter) {

      this.name = name;
      this.meter = meter;
    }

    public String getName () {

      return name;
    }

    public M getMeter () {

      return meter;
    }
  }
}
