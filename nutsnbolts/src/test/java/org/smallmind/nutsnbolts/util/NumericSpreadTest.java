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
package org.smallmind.nutsnbolts.util;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class NumericSpreadTest {

  public void testNullInputReturnsEmptyArray ()
    throws SpreadParserException {

    Assert.assertEquals(NumericSpread.calculate(null), new int[0]);
  }

  public void testEmptyInputReturnsEmptyArray ()
    throws SpreadParserException {

    Assert.assertEquals(NumericSpread.calculate(""), new int[0]);
  }

  public void testSingleNumberIsParsedAsOneElement ()
    throws SpreadParserException {

    Assert.assertEquals(NumericSpread.calculate("42"), new int[] {42});
  }

  public void testCommaSeparatedNumbersAreExpandedInOrder ()
    throws SpreadParserException {

    Assert.assertEquals(NumericSpread.calculate("1,3,5,7"), new int[] {1, 3, 5, 7});
  }

  public void testAscendingRangeIsInclusive ()
    throws SpreadParserException {

    Assert.assertEquals(NumericSpread.calculate("2..5"), new int[] {2, 3, 4, 5});
  }

  public void testDescendingRangeWalksBackwards ()
    throws SpreadParserException {

    Assert.assertEquals(NumericSpread.calculate("5..2"), new int[] {5, 4, 3, 2});
  }

  public void testSingleElementRangeProducesSingleNumber ()
    throws SpreadParserException {

    Assert.assertEquals(NumericSpread.calculate("7..7"), new int[] {7});
  }

  public void testMixedSinglesAndRangesAreExpandedInOrder ()
    throws SpreadParserException {

    Assert.assertEquals(NumericSpread.calculate("1,3..5,9"), new int[] {1, 3, 4, 5, 9});
  }

  public void testInputIsTrimmedPerZone ()
    throws SpreadParserException {

    Assert.assertEquals(NumericSpread.calculate(" 1 , 3..5 , 9 "), new int[] {1, 3, 4, 5, 9});
  }

  @Test(expectedExceptions = SpreadParserException.class)
  public void testBlankElementIsRejected ()
    throws SpreadParserException {

    NumericSpread.calculate("1,,3");
  }

  @Test(expectedExceptions = SpreadParserException.class)
  public void testNonNumericTokenIsRejected ()
    throws SpreadParserException {

    NumericSpread.calculate("1,abc,3");
  }
}
