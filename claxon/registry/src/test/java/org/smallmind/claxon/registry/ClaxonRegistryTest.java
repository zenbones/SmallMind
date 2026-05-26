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

import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.claxon.registry.meter.Meter;
import org.smallmind.claxon.registry.meter.Tally;
import org.smallmind.claxon.registry.meter.TallyBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ClaxonRegistryTest {

  private ClaxonRegistry registry;

  @BeforeMethod
  public void setUp () {

    ClaxonConfiguration configuration = new ClaxonConfiguration();

    configuration.setNamingStrategy(caller -> caller.getName());
    registry = new ClaxonRegistry(configuration);
  }

  @AfterMethod
  public void tearDown ()
    throws InterruptedException {

    registry.stop();
  }

  public void testRegisterReturnsSameMeterForSameCallerAndTags () {

    Meter first = registry.register(ClaxonRegistryTest.class, new TallyBuilder());
    Meter second = registry.register(ClaxonRegistryTest.class, new TallyBuilder());

    Assert.assertSame(first, second);
  }

  public void testRegisterReturnsDistinctMetersForDifferentTagSets () {

    Meter taggedA = registry.register(ClaxonRegistryTest.class, new TallyBuilder(), new Tag("zone", "a"));
    Meter taggedB = registry.register(ClaxonRegistryTest.class, new TallyBuilder(), new Tag("zone", "b"));

    Assert.assertNotSame(taggedA, taggedB);
  }

  public void testRegisterReturnsDistinctMetersForDifferentCallers () {

    Meter callerA = registry.register(ClaxonRegistryTest.class, new TallyBuilder());
    Meter callerB = registry.register(String.class, new TallyBuilder());

    Assert.assertNotSame(callerA, callerB);
  }

  public void testRegisterBuildsConcreteMeterFromBuilder () {

    Meter meter = registry.register(ClaxonRegistryTest.class, new TallyBuilder());

    Assert.assertTrue(meter instanceof Tally);
  }

  public void testRegisterSubstitutesNoOpMeterWhenNamingStrategyReturnsNull () {

    registry.getConfiguration().setNamingStrategy(caller -> null);

    Assert.assertSame(registry.register(ClaxonRegistryTest.class, new TallyBuilder()), NoOpMeter.instance());
  }

  public void testNoOpDecisionIsCachedAndStrategyNotReconsulted () {

    AtomicInteger callCount = new AtomicInteger();

    registry.getConfiguration().setNamingStrategy(caller -> {
      callCount.incrementAndGet();
      return null;
    });

    registry.register(ClaxonRegistryTest.class, new TallyBuilder());
    registry.register(ClaxonRegistryTest.class, new TallyBuilder());
    registry.register(ClaxonRegistryTest.class, new TallyBuilder());

    Assert.assertEquals(callCount.get(), 1);
  }

  public void testNamingStrategyConsultedOncePerSuccessfulKey () {

    AtomicInteger callCount = new AtomicInteger();

    registry.getConfiguration().setNamingStrategy(caller -> {
      callCount.incrementAndGet();
      return caller.getName();
    });

    registry.register(ClaxonRegistryTest.class, new TallyBuilder());
    registry.register(ClaxonRegistryTest.class, new TallyBuilder());

    Assert.assertEquals(callCount.get(), 1);
  }

  public void testUnregisterAllowsRebuildingWithDifferentBuilder () {

    Meter first = registry.register(ClaxonRegistryTest.class, new TallyBuilder());

    registry.unregister(ClaxonRegistryTest.class);

    Meter second = registry.register(ClaxonRegistryTest.class, new TallyBuilder());

    Assert.assertNotSame(first, second);
  }

  public void testBindAndGetEmitterRoundTrip () {

    Emitter pushy = new PushEmitter() {

      @Override
      public void record (String meterName, Tag[] tags, Quantity[] quantities) {

      }
    };

    registry.bind("foo", pushy);

    Assert.assertSame(registry.getEmitter("foo"), pushy);
  }

  public void testGetEmitterReturnsNullForUnknownName () {

    Assert.assertNull(registry.getEmitter("missing"));
  }

  public void testBindReplacesExistingEmitterUnderSameName () {

    Emitter first = new PushEmitter() {

      @Override
      public void record (String meterName, Tag[] tags, Quantity[] quantities) {

      }
    };
    Emitter second = new PushEmitter() {

      @Override
      public void record (String meterName, Tag[] tags, Quantity[] quantities) {

      }
    };

    registry.bind("foo", first);
    registry.bind("foo", second);

    Assert.assertSame(registry.getEmitter("foo"), second);
  }

  public void testBindReturnsRegistryForChaining () {

    Emitter pushy = new PushEmitter() {

      @Override
      public void record (String meterName, Tag[] tags, Quantity[] quantities) {

      }
    };

    Assert.assertSame(registry.bind("foo", pushy), registry);
  }
}
