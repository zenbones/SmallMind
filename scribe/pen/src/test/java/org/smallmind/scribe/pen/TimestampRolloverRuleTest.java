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
package org.smallmind.scribe.pen;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TimestampRolloverRuleTest {

  private static final long DAY_IN_MILLIS = 24L * 60 * 60 * 1000;

  // The rule has no injectable clock, so deterministic assertions are framed as relative offsets
  // from the current instant rather than against a fixed wall-clock value.

  public void testSameDayInstantDoesNotRollForDayBoundary () {

    long now = System.currentTimeMillis();

    Assert.assertFalse(new TimestampRolloverRule(TimestampQuantifier.TOP_OF_DAY).willRollover(0, now, 0));
  }

  public void testSameMonthInstantDoesNotRollForMonthBoundary () {

    long now = System.currentTimeMillis();

    Assert.assertFalse(new TimestampRolloverRule(TimestampQuantifier.TOP_OF_MONTH).willRollover(0, now, 0));
  }

  public void testFortyDaysInThePastRollsEveryQuantifier () {

    // Forty days guarantees a crossed boundary for every quantifier, up to and including TOP_OF_MONTH.
    long longAgo = System.currentTimeMillis() - (40 * DAY_IN_MILLIS);

    for (TimestampQuantifier quantifier : TimestampQuantifier.values()) {
      Assert.assertTrue(new TimestampRolloverRule(quantifier).willRollover(0, longAgo, 0), "expected rollover for " + quantifier);
    }
  }

  public void testFileSizeAndPendingBytesAreIgnored () {

    long now = System.currentTimeMillis();

    // Huge size and pending-write values must not influence a purely time-based rule.
    Assert.assertFalse(new TimestampRolloverRule(TimestampQuantifier.TOP_OF_DAY).willRollover(Long.MAX_VALUE, now, Long.MAX_VALUE));
  }

  public void testDefaultQuantifierIsTopOfDay () {

    Assert.assertEquals(new TimestampRolloverRule().getTimestampQuantifier(), TimestampQuantifier.TOP_OF_DAY);
  }

  public void testEachQuantifierRollsAtItsFinestBoundary () {

    long now = System.currentTimeMillis();

    // Each offset is just past the relevant boundary while staying within the next-coarser one,
    // so the deciding operand is the quantifier's finest field.
    Assert.assertTrue(new TimestampRolloverRule(TimestampQuantifier.TOP_OF_MINUTE).willRollover(0, now - (90 * 1000L), 0));
    Assert.assertTrue(new TimestampRolloverRule(TimestampQuantifier.TOP_OF_HOUR).willRollover(0, now - (90 * 60 * 1000L), 0));
    Assert.assertTrue(new TimestampRolloverRule(TimestampQuantifier.HALF_DAY).willRollover(0, now - (13 * 60 * 60 * 1000L), 0));
    Assert.assertTrue(new TimestampRolloverRule(TimestampQuantifier.TOP_OF_WEEK).willRollover(0, now - (8 * DAY_IN_MILLIS), 0));
  }
}
