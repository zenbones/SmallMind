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

import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ClaxonConfigurationTest {

  public void testCalculateTagsReturnsNullWhenAllLevelsEmpty () {

    Assert.assertNull(new ClaxonConfiguration().calculateTags("meter"));
  }

  public void testCalculateTagsWithOnlyRegistryTags () {

    Tag registryTag = new Tag("env", "prod");
    ClaxonConfiguration configuration = new ClaxonConfiguration();

    configuration.setRegistryTags(new Tag[] {registryTag});

    Assert.assertEquals(configuration.calculateTags("meter"), new Tag[] {registryTag});
  }

  public void testCalculateTagsWithOnlyInstanceTags () {

    Tag instanceTag = new Tag("user", "42");

    Assert.assertEquals(new ClaxonConfiguration().calculateTags("meter", instanceTag), new Tag[] {instanceTag});
  }

  public void testCalculateTagsWithOnlyMeterTags () {

    Tag meterTag = new Tag("layer", "api");
    ClaxonConfiguration configuration = new ClaxonConfiguration();
    HashMap<String, Tag[]> meterTagMap = new HashMap<>();

    meterTagMap.put("meter", new Tag[] {meterTag});
    configuration.setMeterTags(meterTagMap);

    Assert.assertEquals(configuration.calculateTags("meter"), new Tag[] {meterTag});
  }

  public void testCalculateTagsMergesAllThreeLevelsInRegistryThenMeterThenInstanceOrder () {

    Tag registryTag = new Tag("env", "prod");
    Tag meterTag = new Tag("layer", "api");
    Tag instanceTag = new Tag("user", "42");
    ClaxonConfiguration configuration = new ClaxonConfiguration();
    HashMap<String, Tag[]> meterTagMap = new HashMap<>();

    meterTagMap.put("meter", new Tag[] {meterTag});
    configuration.setRegistryTags(new Tag[] {registryTag});
    configuration.setMeterTags(meterTagMap);

    Assert.assertEquals(configuration.calculateTags("meter", instanceTag), new Tag[] {registryTag, meterTag, instanceTag});
  }

  public void testMeterTagsAppliedOnlyForMatchingName () {

    Tag registryTag = new Tag("env", "prod");
    Tag meterTagForA = new Tag("layer", "api");
    ClaxonConfiguration configuration = new ClaxonConfiguration();
    HashMap<String, Tag[]> meterTagMap = new HashMap<>();

    meterTagMap.put("meterA", new Tag[] {meterTagForA});
    configuration.setRegistryTags(new Tag[] {registryTag});
    configuration.setMeterTags(meterTagMap);

    Assert.assertEquals(configuration.calculateTags("meterA"), new Tag[] {registryTag, meterTagForA});
    Assert.assertEquals(configuration.calculateTags("meterB"), new Tag[] {registryTag});
  }

  public void testForMeterReturnsNullWhenMeterTagsUnset () {

    Assert.assertNull(new ClaxonConfiguration().forMeter("anything"));
  }

  public void testForMeterReturnsNullWhenMeterNameAbsent () {

    ClaxonConfiguration configuration = new ClaxonConfiguration();
    HashMap<String, Tag[]> meterTagMap = new HashMap<>();

    meterTagMap.put("known", new Tag[] {new Tag("k", "v")});
    configuration.setMeterTags(meterTagMap);

    Assert.assertNull(configuration.forMeter("unknown"));
  }

  public void testNullArgumentConstructorRetainsDefaults () {

    ClaxonConfiguration defaults = new ClaxonConfiguration();
    ClaxonConfiguration constructed = new ClaxonConfiguration(null, null, null, null, null, null);

    Assert.assertSame(constructed.getClock(), defaults.getClock());
    Assert.assertEquals(constructed.getCollectionStint().getTime(), defaults.getCollectionStint().getTime());
    Assert.assertEquals(constructed.getCollectionStint().getTimeUnit(), defaults.getCollectionStint().getTimeUnit());
    Assert.assertEquals(constructed.getRegistryTags().length, 0);
  }
}
