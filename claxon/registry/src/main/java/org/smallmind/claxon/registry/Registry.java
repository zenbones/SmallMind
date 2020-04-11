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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.scribe.pen.LoggerManager;

public class Registry {

  private final ConcurrentHashMap<String, Collector> collectorMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<RegistryKey, Meter> meterMap = new ConcurrentHashMap<>();
  private final CollectionWorker collectionWorker;
  private final Domain domain;
  private final Clock clock;
  private final Stint collectionStint;

  public Registry () {

    this(null, SystemClock.instance(), new Stint(1, TimeUnit.SECONDS));
  }

  public Registry (Domain domain) {

    this(domain, SystemClock.instance(), new Stint(1, TimeUnit.SECONDS));
  }

  public Registry (Clock clock) {

    this(null, clock, new Stint(1, TimeUnit.SECONDS));
  }

  public Registry (Stint collectionStint) {

    this(null, SystemClock.instance(), collectionStint);
  }

  public Registry (Domain domain, Clock clock) {

    this(domain, clock, new Stint(1, TimeUnit.SECONDS));
  }

  public Registry (Domain domain, Stint collectionStint) {

    this(domain, SystemClock.instance(), collectionStint);
  }

  public Registry (Clock clock, Stint collectionStint) {

    this(null, clock, collectionStint);
  }

  public Registry (Domain domain, Clock clock, Stint collectionStint) {

    Thread workerThread = new Thread(collectionWorker = new CollectionWorker());

    this.domain = domain;
    this.clock = clock;
    this.collectionStint = collectionStint;

    workerThread.setDaemon(true);
    workerThread.start();
  }

  public void stop ()
    throws InterruptedException {

    collectionWorker.stop();
  }

  public Registry bind (String name, Collector collector) {

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

  public void instrument (Meter meter, Executable executable) {

    executable.execute();
  }

  public <T> T instrument (Meter meter, Supplier<T> supplier) {

    return supplier.get();
  }

  public void speak () {

    for (Collector collector : collectorMap.values()) {
      if (CollectionMethod.PUSH.equals(collector.getCollectionMethod())) {
        for (Map.Entry<RegistryKey, Meter> meterEntry : meterMap.entrySet()) {
          try {
            collector.record(domain, meterEntry.getKey().getIdentifier(), meterEntry.getKey().getTags(), meterEntry.getValue().getQuantities());
          } catch (Exception exception) {
            LoggerManager.getLogger(Registry.class).error(exception);
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
          speak();
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(Registry.class).error(interruptedException);
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
