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
package org.smallmind.claxon.exotic.jvm;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ProfileFeatureTest {

  public void testNoHostnameTagWhenDisabled ()
    throws UnknownHostException {

    ProfileFeature feature = new ProfileFeature("jvm.profile", null, false);

    Assert.assertEquals(feature.getTags().length, 0);
  }

  public void testHostnameTagPrependedWhenEnabledWithNoOtherTags ()
    throws UnknownHostException {

    ProfileFeature feature = new ProfileFeature("jvm.profile", null, true);

    Assert.assertEquals(feature.getTags().length, 1);
    Assert.assertEquals(feature.getTags()[0].getKey(), "host");
  }

  public void testHostnameTagPrependedAheadOfOtherTags ()
    throws UnknownHostException {

    Tag custom = new Tag("service", "auth");
    ProfileFeature feature = new ProfileFeature("jvm.profile", null, true, custom);

    Assert.assertEquals(feature.getTags().length, 2);
    Assert.assertEquals(feature.getTags()[0].getKey(), "host");
    Assert.assertEquals(feature.getTags()[1], custom);
  }

  public void testRecordEmitsExpectedQuantityNames ()
    throws UnknownHostException {

    Set<String> expected = new HashSet<>();

    expected.add("totalMemorySize");
    expected.add("freeMemorySize");
    expected.add("userMemoryPercent");
    expected.add("heapMemoryMax");
    expected.add("heapMemoryUsed");
    expected.add("processCPUTime");
    expected.add("compilationTime");
    expected.add("youngGenerationHeapSize");
    expected.add("youngCollectionCount");
    expected.add("youngCollectionTime");
    expected.add("oldCollectionCount");
    expected.add("oldCollectionTime");
    expected.add("edenMemoryUsed");
    expected.add("survivorMemoryUsed");
    expected.add("tenuredMemoryUsed");

    Quantity[] quantities = new ProfileFeature("jvm.profile", null, false).record();

    Assert.assertEquals(quantities.length, 15);

    Set<String> actual = new HashSet<>();

    for (Quantity quantity : quantities) {
      actual.add(quantity.getName());
    }

    Assert.assertEquals(actual, expected);
  }

  public void testThrottleSuppressesSecondRecordingWithinDelay ()
    throws UnknownHostException {

    ProfileFeature feature = new ProfileFeature("jvm.profile", Long.MAX_VALUE, false);

    Assert.assertNotNull(feature.record());
    Assert.assertNull(feature.record());
  }

  public void testNullDelayDisablesThrottle ()
    throws UnknownHostException {

    ProfileFeature feature = new ProfileFeature("jvm.profile", null, false);

    Assert.assertNotNull(feature.record());
    Assert.assertNotNull(feature.record());
  }
}
