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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.smallmind.claxon.registry.feature.Feature;
import org.smallmind.claxon.registry.meter.TallyBuilder;
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the live {@link ClaxonRegistry} collection loop end to end.
 *
 * <p>Constructs a registry with a short collection stint, binds a recording
 * {@link PushEmitter}, registers a meter, applies an update, and waits for the
 * daemon worker thread to dispatch a reading. Also verifies that the {@code track}
 * path causes the worker to sample a measured object on each cycle, that registry
 * and instance tags are merged on the way out, and that {@link ClaxonRegistry#stop()}
 * drains the worker before returning.
 */
@Test(groups = "unit")
public class RegistryCollectionCycleTest {

  private static final long AWAIT_TIMEOUT_MILLIS = 10000L;
  private static final String METER_NAME = RegistryCollectionCycleTest.class.getName();

  private ClaxonRegistry registry;
  private RecordingPushEmitter emitter;

  @BeforeMethod
  public void setUp () {

    ClaxonConfiguration configuration = new ClaxonConfiguration();

    configuration.setCollectionStint(new Stint(50, TimeUnit.MILLISECONDS));
    configuration.setNamingStrategy(caller -> caller.getName());
    registry = new ClaxonRegistry(configuration);

    emitter = new RecordingPushEmitter();
    registry.bind("recorder", emitter);
  }

  @AfterMethod
  public void tearDown ()
    throws InterruptedException {

    registry.stop();
  }

  public void testWorkerDispatchesUpdatedMeterToBoundEmitter ()
    throws InterruptedException {

    registry.register(RegistryCollectionCycleTest.class, new TallyBuilder()).update(7L);

    Recording recording = emitter.await(METER_NAME, candidate -> candidate.findQuantity("count").getValue() >= 7.0D, AWAIT_TIMEOUT_MILLIS);

    Assert.assertNotNull(recording, "Expected a recording for the registered meter within the timeout");
    Assert.assertEquals(recording.findQuantity("count").getValue(), 7.0D);
  }

  public void testWorkerInvokesTrackedMeasurementOnEachSweep ()
    throws InterruptedException {

    AtomicInteger sampleInvocations = new AtomicInteger();
    Object measured = new Object();

    registry.track(RegistryCollectionCycleTest.class, new TallyBuilder(), measured, ignored -> {
      sampleInvocations.incrementAndGet();

      return 1L;
    }, new Tag("trackedAs", "object"));

    long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MILLIS;

    while ((sampleInvocations.get() < 2) && (System.currentTimeMillis() < deadline)) {
      Thread.sleep(20L);
    }

    Assert.assertTrue(sampleInvocations.get() >= 2, "Worker should invoke the tracked measurement function on each sweep");
    Assert.assertNotNull(emitter.await(METER_NAME, candidate -> candidate.findQuantity("count").getValue() >= 1.0D, AWAIT_TIMEOUT_MILLIS), "Tracked meter should produce a recording carrying the sampled value");
  }

  public void testWorkerMergesRegistryAndInstanceTagsOnEmission ()
    throws InterruptedException {

    registry.getConfiguration().setRegistryTags(new Tag[] {new Tag("env", "test")});
    registry.register(RegistryCollectionCycleTest.class, new TallyBuilder(), new Tag("zone", "a")).update(1L);

    Recording recording = emitter.await(METER_NAME, candidate -> candidate.hasTag("env", "test") && candidate.hasTag("zone", "a"), AWAIT_TIMEOUT_MILLIS);

    Assert.assertNotNull(recording, "Expected a recording carrying the merged tag set");
  }

  public void testStopDrainsWorkerThread ()
    throws InterruptedException {

    registry.register(RegistryCollectionCycleTest.class, new TallyBuilder()).update(1L);

    Assert.assertNotNull(emitter.await(METER_NAME, candidate -> true, AWAIT_TIMEOUT_MILLIS));

    registry.stop();

    int frozenCount = emitter.recordingCount();

    Thread.sleep(200L);

    Assert.assertEquals(emitter.recordingCount(), frozenCount, "No further recordings should arrive after stop() returns");
  }

  public void testWorkerDispatchesFeatureReadingsToBoundEmitter ()
    throws InterruptedException {

    Feature feature = new Feature() {

      @Override
      public String getName () {

        return "feature.reading";
      }

      @Override
      public Tag[] getTags () {

        return new Tag[0];
      }

      @Override
      public Quantity[] record () {

        return new Quantity[] {new Quantity("value", 99.0)};
      }
    };

    registry.getConfiguration().setFeatures(new Feature[] {feature});

    Recording recording = emitter.await("feature.reading", candidate -> candidate.findQuantity("value").getValue() >= 99.0, AWAIT_TIMEOUT_MILLIS);

    Assert.assertNotNull(recording, "Worker should dispatch feature readings to bound emitters");
    Assert.assertEquals(recording.findQuantity("value").getValue(), 99.0);
  }

  private static final class RecordingPushEmitter extends PushEmitter {

    private final List<Recording> recordings = new ArrayList<>();
    private final Object monitor = new Object();

    @Override
    public void record (String meterName, Tag[] tags, Quantity[] quantities) {

      synchronized (monitor) {
        recordings.add(new Recording(meterName, tags, quantities));
        monitor.notifyAll();
      }
    }

    int recordingCount () {

      synchronized (monitor) {
        return recordings.size();
      }
    }

    Recording await (String meterName, Predicate<Recording> condition, long timeoutMillis)
      throws InterruptedException {

      long deadline = System.currentTimeMillis() + timeoutMillis;
      int scanned = 0;

      synchronized (monitor) {
        while (true) {

          while (scanned < recordings.size()) {

            Recording candidate = recordings.get(scanned++);

            if (meterName.equals(candidate.meterName()) && condition.test(candidate)) {

              return candidate;
            }
          }

          long remaining = deadline - System.currentTimeMillis();

          if (remaining <= 0L) {

            return null;
          }

          monitor.wait(remaining);
        }
      }
    }
  }

  private record Recording(String meterName, Tag[] tags, Quantity[] quantities) {

    Quantity findQuantity (String name) {

      for (Quantity quantity : quantities) {
        if (name.equals(quantity.getName())) {

          return quantity;
        }
      }

      throw new AssertionError("No quantity named '" + name + "' in " + Arrays.toString(quantities));
    }

    boolean hasTag (String key, String value) {

      if (tags == null) {

        return false;
      }
      for (Tag tag : tags) {
        if (key.equals(tag.getKey()) && value.equals(tag.getValue())) {

          return true;
        }
      }

      return false;
    }
  }
}
