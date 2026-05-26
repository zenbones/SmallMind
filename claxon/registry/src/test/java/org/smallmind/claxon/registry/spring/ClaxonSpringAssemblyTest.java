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
package org.smallmind.claxon.registry.spring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.smallmind.claxon.registry.ClaxonConfiguration;
import org.smallmind.claxon.registry.ClaxonRegistry;
import org.smallmind.claxon.registry.Emitter;
import org.smallmind.claxon.registry.ImpliedNamingStrategy;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.PushEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.TallyBuilder;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.util.DotNotation;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the Spring assembly path end to end through the three factory beans
 * ({@link StintFactoryBean}, {@link TagFactoryBean}, {@link ClaxonRegistryFactoryBean}).
 *
 * <p>The test drives the same {@code afterPropertiesSet} / {@code destroy} lifecycle that a
 * Spring container would, then proves that:
 * <ol>
 *   <li>{@code afterPropertiesSet} installs the registry via
 *       {@link ClaxonRegistry#initializeInstrumentation()} so that
 *       {@link Instrument#with} resolves through to it,</li>
 *   <li>the bound emitter receives readings from the live collection worker, and</li>
 *   <li>{@code destroy} drains the worker thread so that no further emissions arrive after
 *       the bean is disposed.</li>
 * </ol>
 *
 * <p>The {@link PerApplicationContext} is reset in {@link #setUp()} so the static
 * {@link Instrument} façade resolves to the registry built in this test rather than one
 * left behind by an earlier test class.
 */
@Test(groups = "unit")
public class ClaxonSpringAssemblyTest {

  private static final long AWAIT_TIMEOUT_MILLIS = 10000L;
  private static final String METER_PREFIX = "app.test";

  private PerApplicationContext.ContextCarrier priorContext;
  private ClaxonRegistryFactoryBean registryBean;
  private RecordingPushEmitter emitter;

  @BeforeMethod
  public void setUp ()
    throws Exception {

    priorContext = PerApplicationContext.generateCarrier();
    new PerApplicationContext();

    StintFactoryBean stintBean = new StintFactoryBean();

    stintBean.setTime(50L);
    stintBean.setTimeUnit(TimeUnit.MILLISECONDS);
    stintBean.afterPropertiesSet();

    TagFactoryBean tagBean = new TagFactoryBean();

    tagBean.setKey("env");
    tagBean.setValue("integration");
    tagBean.afterPropertiesSet();

    Map<DotNotation, String> prefixMap = new LinkedHashMap<>();

    prefixMap.put(new DotNotation(ClaxonSpringAssemblyTest.class.getPackageName() + ".*"), METER_PREFIX);

    ClaxonConfiguration configuration = new ClaxonConfiguration();

    configuration.setCollectionStint(stintBean.getObject());
    configuration.setRegistryTags(new Tag[] {tagBean.getObject()});
    configuration.setNamingStrategy(new ImpliedNamingStrategy().setPrefixMap(prefixMap));

    emitter = new RecordingPushEmitter();

    Map<String, Emitter> emitterMap = new HashMap<>();

    emitterMap.put("recorder", emitter);

    registryBean = new ClaxonRegistryFactoryBean();
    registryBean.setConfiguration(configuration);
    registryBean.setEmitterMap(emitterMap);
    registryBean.afterPropertiesSet();
  }

  @AfterMethod
  public void tearDown ()
    throws InterruptedException {

    try {
      if (registryBean != null) {
        registryBean.destroy();
      }
    } finally {
      priorContext.prepareThread();
    }
  }

  public void testAfterPropertiesSetInstallsRegistryForInstrumentFacade () {

    ClaxonRegistry registry = registryBean.getObject();

    Assert.assertNotNull(registry, "Factory bean should produce a registry after afterPropertiesSet");
    Assert.assertSame(Instrument.getRegistry(), registry, "Registry should be installed in the per-application context");
  }

  public void testInstrumentCallSiteFlowsThroughBoundEmitter ()
    throws InterruptedException {

    Instrument.with(ClaxonSpringAssemblyTest.class, new TallyBuilder(), new Tag("op", "checkout")).update(5L);

    Recording recording = emitter.await(METER_PREFIX, candidate -> candidate.hasTag("env", "integration") && candidate.hasTag("op", "checkout"), AWAIT_TIMEOUT_MILLIS);

    Assert.assertNotNull(recording, "Expected an emission carrying both registry-wide and instance tags");
    Assert.assertEquals(recording.findQuantity("count").getValue(), 5.0D);
  }

  public void testDestroyStopsTheCollectionWorker ()
    throws InterruptedException {

    Instrument.with(ClaxonSpringAssemblyTest.class, new TallyBuilder()).update(1L);

    Assert.assertNotNull(emitter.await(METER_PREFIX, candidate -> true, AWAIT_TIMEOUT_MILLIS));

    registryBean.destroy();
    registryBean = null;

    int frozenCount = emitter.recordingCount();

    Thread.sleep(200L);

    Assert.assertEquals(emitter.recordingCount(), frozenCount, "No further recordings should arrive after destroy() returns");
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

      throw new AssertionError("No quantity named '" + name + "' in this recording");
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
