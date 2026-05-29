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
package org.smallmind.nutsnbolts.time;

import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class StintUtilityTest {

  public void testSameUnitReturnsValueAsDouble () {

    Assert.assertEquals(StintUtility.convertToDouble(5L, TimeUnit.SECONDS, TimeUnit.SECONDS), 5.0d);
  }

  public void testConvertingToCoarserUnitPreservesFractionAsDouble () {

    Assert.assertEquals(StintUtility.convertToDouble(1500L, TimeUnit.MILLISECONDS, TimeUnit.SECONDS), 1.5d);
    Assert.assertEquals(StintUtility.convertToDouble(60000L, TimeUnit.MILLISECONDS, TimeUnit.MINUTES), 1.0d);
    Assert.assertEquals(StintUtility.convertToDouble(30L, TimeUnit.SECONDS, TimeUnit.MINUTES), 0.5d, 1e-9);
  }

  public void testConvertingToFinerUnitMultipliesAsExpected () {

    Assert.assertEquals(StintUtility.convertToDouble(1L, TimeUnit.SECONDS, TimeUnit.MILLISECONDS), 1000.0d);
    Assert.assertEquals(StintUtility.convertToDouble(2L, TimeUnit.MINUTES, TimeUnit.SECONDS), 120.0d);
  }
}
