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
package org.smallmind.kafka.utility;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link KafkaServer}. Covers the documented ordering contract
 * of {@link KafkaServer#compareTo(KafkaServer)} (host first, port second) and the
 * documented {@code 9094} default port from the no-arg constructor. Mechanical
 * accessors are not exercised; their only contract is field assignment.
 */
@Test(groups = "unit")
public class KafkaServerTest {

  public void testDefaultPortIs9094 () {

    Assert.assertEquals(new KafkaServer().getPort(), 9094);
  }

  public void testCompareToOrdersByHostWhenHostsDiffer () {

    KafkaServer a = new KafkaServer("alpha", 9094);
    KafkaServer b = new KafkaServer("beta", 9094);

    Assert.assertTrue(a.compareTo(b) < 0);
    Assert.assertTrue(b.compareTo(a) > 0);
  }

  public void testCompareToBreaksHostTiesByPort () {

    KafkaServer low = new KafkaServer("alpha", 9094);
    KafkaServer high = new KafkaServer("alpha", 9095);

    Assert.assertTrue(low.compareTo(high) < 0);
    Assert.assertTrue(high.compareTo(low) > 0);
  }

  public void testCompareToReturnsZeroForIdenticalHostAndPort () {

    KafkaServer a = new KafkaServer("alpha", 9094);
    KafkaServer b = new KafkaServer("alpha", 9094);

    Assert.assertEquals(a.compareTo(b), 0);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testCompareToThrowsNullPointerExceptionWhenHostUnset () {

    new KafkaServer().compareTo(new KafkaServer("alpha", 9094));
  }
}
