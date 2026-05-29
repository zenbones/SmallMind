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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TimeArithmeticTest {

  private static final ZonedDateTime REFERENCE = ZonedDateTime.of(2026, 5, 28, 10, 0, 0, 0, ZoneId.of("UTC"));
  private static final Instant BEFORE_INSTANT = REFERENCE.minusHours(1).toInstant();
  private static final Instant SAME_INSTANT = REFERENCE.toInstant();
  private static final Instant AFTER_INSTANT = REFERENCE.plusHours(1).toInstant();

  public void testAccessorsReturnConstructorArguments () {

    TimeArithmetic arithmetic = new TimeArithmetic(REFERENCE, TimeOperation.BEFORE);

    Assert.assertEquals(arithmetic.getDate(), REFERENCE);
    Assert.assertEquals(arithmetic.getOperation(), TimeOperation.BEFORE);
  }

  public void testBeforeAcceptsEarlierInstantStrictly () {

    TimeArithmetic before = new TimeArithmetic(REFERENCE, TimeOperation.BEFORE);

    Assert.assertTrue(before.accept(BEFORE_INSTANT));
    Assert.assertFalse(before.accept(SAME_INSTANT));
    Assert.assertFalse(before.accept(AFTER_INSTANT));
  }

  public void testBeforeOrOnAcceptsEqualAndEarlier () {

    TimeArithmetic beforeOrOn = new TimeArithmetic(REFERENCE, TimeOperation.BEFORE_OR_ON);

    Assert.assertTrue(beforeOrOn.accept(BEFORE_INSTANT));
    Assert.assertTrue(beforeOrOn.accept(SAME_INSTANT));
    Assert.assertFalse(beforeOrOn.accept(AFTER_INSTANT));
  }

  public void testOnOrAfterAcceptsEqualAndLater () {

    TimeArithmetic onOrAfter = new TimeArithmetic(REFERENCE, TimeOperation.ON_OR_AFTER);

    Assert.assertFalse(onOrAfter.accept(BEFORE_INSTANT));
    Assert.assertTrue(onOrAfter.accept(SAME_INSTANT));
    Assert.assertTrue(onOrAfter.accept(AFTER_INSTANT));
  }

  public void testAfterAcceptsLaterInstantStrictly () {

    TimeArithmetic after = new TimeArithmetic(REFERENCE, TimeOperation.AFTER);

    Assert.assertFalse(after.accept(BEFORE_INSTANT));
    Assert.assertFalse(after.accept(SAME_INSTANT));
    Assert.assertTrue(after.accept(AFTER_INSTANT));
  }
}
