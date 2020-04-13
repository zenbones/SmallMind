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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.MeterBuilder;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.scribe.pen.LoggerManager;

public class ClaxonRegistry {

  private final ConcurrentHashMap<String, Collector> collectorMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<RegistryKey, Meter> meterMap = new ConcurrentHashMap<>();
  private final MeasurableTracker measurableTrcker;
  private final ObservableTracker observableTracker;
  private final CollectionWorker collectionWorker;
  private final Clock clock;
  private final Stint collectionStint;
  private final Tag[] registryTags;

  public ClaxonRegistry (Tag... registryTags) {

    this(SystemClock.instance(), new Stint(1, TimeUnit.SECONDS), registryTags);
  }

  public ClaxonRegistry (Clock clock, Tag... registryTags) {

    this(clock, new Stint(1, TimeUnit.SECONDS), registryTags);
  }

  public ClaxonRegistry (Stint collectionStint, Tag... registryTags) {

    this(SystemClock.instance(), collectionStint, registryTags);
  }

  public ClaxonRegistry (Clock clock, Stint collectionStint, Tag... registryTags) {

    Thread workerThread = new Thread(collectionWorker = new CollectionWorker());

    this.clock = clock;
    this.collectionStint = collectionStint;
    this.registryTags = registryTags;

    measurableTrcker = new MeasurableTracker(this);
    observableTracker = new ObservableTracker(this);

    workerThread.setDaemon(true);
    workerThread.start();
  }

  public void stop ()
    throws InterruptedException {

    collectionWorker.stop();
  }

  public Collector collector (String name) {

    return collectorMap.get(name);
  }

  public ClaxonRegistry bind (String name, Collector collector) {

    collectorMap.put(name, collector);

    return this;
  }

  public <M extends Meter> M register (Identifier identifier, MeterBuilder<M> builder, Tag... tags) {

    RegistryKey key = new RegistryKey(identifier, tags);
    M meter;

    if ((meter = (M)meterMap.get(key)) == null) {
      synchronized (meterMap) {
        if ((meter = (M)meterMap.get(key)) == null) {
          meterMap.put(key, meter = builder.clock(clock).build());
        }
      }
    }

    return meter;
  }

  public void unregister (Identifier identifier, Tag... tags) {

    meterMap.remove(new RegistryKey(identifier, tags));
  }

  public <O extends Observable> O track (Identifier identifier, MeterBuilder<?> builder, O observable, Tag... tags) {

    return observableTracker.track(identifier, builder, observable, tags);
  }

  public <T> T track (Identifier identifier, MeterBuilder<?> builder, T measured, Function<T, Long> measurement, Tag... tags) {

    return measurableTrcker.track(identifier, builder, measured, measurement, tags);
  }

  public void instrument (Meter meter, long value) {

    meter.update(value);
  }

  public void instrument (Meter meter, TimeUnit timeUnit, SansResultExecutable sansResultExecutable)
    throws Throwable {

    long start = clock.monotonicTime();

    sansResultExecutable.execute();
    meter.update(timeUnit.convert(clock.monotonicTime() - start, TimeUnit.NANOSECONDS));
  }

  public <T> T instrument (Meter meter, TimeUnit timeUnit, WithResultExecutable<T> withResultExecutable)
    throws Throwable {

    T result;
    long start = clock.monotonicTime();

    result = withResultExecutable.execute();
    meter.update(timeUnit.convert(clock.monotonicTime() - start, TimeUnit.NANOSECONDS));

    return result;
  }

  public void record () {

    measurableTrcker.sweepAndUpdate();
    observableTracker.sweep();

    for (Map.Entry<RegistryKey, Meter> meterEntry : meterMap.entrySet()) {

      Quantity[] quantities = meterEntry.getValue().record();

      if ((quantities != null) && (quantities.length > 0)) {

        Tag[] mergedTags;
        Tag[] meterTags = meterEntry.getKey().getTags();

        if ((registryTags == null) || (registryTags.length == 0)) {
          mergedTags = meterTags;
        } else if ((meterTags == null) || (meterTags.length == 0)) {
          mergedTags = registryTags;
        } else {
          mergedTags = new Tag[registryTags.length + meterTags.length];
          System.arraycopy(registryTags, 0, mergedTags, 0, registryTags.length);
          System.arraycopy(meterTags, 0, mergedTags, registryTags.length, meterTags.length);
        }

        for (Collector collector : collectorMap.values()) {
          try {
            collector.record(meterEntry.getKey().getIdentifier(), mergedTags, quantities);
          } catch (Exception exception) {
            LoggerManager.getLogger(ClaxonRegistry.class).error(exception);
          }
        }
      }
    }
  }

  private class CollectionWorker implements Runnable {

    private CountDownLatch finishLatch = new CountDownLatch(1);
    private CountDownLatch exitLatch = new CountDownLatch(1);

    public void stop ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!finishLatch.await(collectionStint.getTime(), collectionStint.getTimeUnit())) {
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

    private Identifier identifier;
    private Tag[] tags;

    public RegistryKey (Identifier identifier, Tag... tags) {

      this.identifier = identifier;
      this.tags = tags;
    }

    public Identifier getIdentifier () {

      return identifier;
    }

    public Tag[] getTags () {

      return tags;
    }

    @Override
    public int hashCode () {

      return (identifier.hashCode() * 31) + ((tags == null) ? 0 : Arrays.hashCode(tags));
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof RegistryKey) && ((RegistryKey)obj).getIdentifier().equals(identifier) && Arrays.equals(((RegistryKey)obj).getTags(), tags);
    }
  }
}
