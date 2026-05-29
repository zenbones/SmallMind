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

import java.math.BigDecimal;
import java.math.BigInteger;
import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class NumberComparatorTest {

  private final NumberComparator comparator = new NumberComparator();

  public void testNullsSortBeforeNonNull () {

    Assert.assertTrue(comparator.compare(null, 1) < 0);
    Assert.assertTrue(comparator.compare(1, null) > 0);
    Assert.assertEquals(comparator.compare(null, null), 0);
  }

  public void testBigDecimalAgainstEveryRightHandType () {

    Assert.assertEquals(comparator.compare(new BigDecimal("5"), new BigDecimal("5.00")), 0);
    Assert.assertTrue(comparator.compare(new BigDecimal("5.5"), new BigDecimal("5.4")) > 0);
    Assert.assertTrue(comparator.compare(new BigDecimal("5"), BigInteger.valueOf(6)) < 0);
    Assert.assertEquals(comparator.compare(new BigDecimal("5.25"), 5.25d), 0);
    Assert.assertTrue(comparator.compare(new BigDecimal("5.25"), 5.0f) > 0);
    Assert.assertTrue(comparator.compare(new BigDecimal("5"), 4L) > 0);
    Assert.assertEquals(comparator.compare(new BigDecimal("5"), 5), 0);
    Assert.assertTrue(comparator.compare(new BigDecimal("5"), (short)6) < 0);
    Assert.assertTrue(comparator.compare(new BigDecimal("5"), (byte)4) > 0);
  }

  public void testBigIntegerAgainstEveryRightHandType () {

    Assert.assertTrue(comparator.compare(BigInteger.valueOf(5), new BigDecimal("5.5")) < 0);
    Assert.assertEquals(comparator.compare(BigInteger.valueOf(5), BigInteger.valueOf(5)), 0);
    Assert.assertTrue(comparator.compare(BigInteger.valueOf(5), 4.9d) > 0);
    Assert.assertTrue(comparator.compare(BigInteger.valueOf(5), 5.1f) < 0);
    Assert.assertEquals(comparator.compare(BigInteger.valueOf(5), 5L), 0);
    Assert.assertTrue(comparator.compare(BigInteger.valueOf(5), 6) < 0);
    Assert.assertTrue(comparator.compare(BigInteger.valueOf(5), (short)4) > 0);
    Assert.assertEquals(comparator.compare(BigInteger.valueOf(5), (byte)5), 0);
  }

  public void testIntegralAgainstEveryRightHandType () {

    Assert.assertTrue(comparator.compare(5L, new BigDecimal("5.5")) < 0);
    Assert.assertTrue(comparator.compare(5, BigInteger.valueOf(4)) > 0);
    Assert.assertEquals(comparator.compare((short)5, 5.0d), 0);
    Assert.assertTrue(comparator.compare((byte)5, 5.5f) < 0);
    Assert.assertEquals(comparator.compare(5L, 5), 0);
    Assert.assertTrue(comparator.compare(5, 6L) < 0);
    Assert.assertTrue(comparator.compare((short)5, (byte)4) > 0);
  }

  public void testFloatingAgainstEveryRightHandType () {

    Assert.assertEquals(comparator.compare(5.5d, new BigDecimal("5.5")), 0);
    Assert.assertTrue(comparator.compare(5.5d, BigInteger.valueOf(6)) < 0);
    Assert.assertEquals(comparator.compare(5.5d, 5.5f), 0);
    Assert.assertTrue(comparator.compare(5.5f, 5L) > 0);
    Assert.assertTrue(comparator.compare(5.5d, 5) > 0);
    Assert.assertTrue(comparator.compare(5.5f, (short)6) < 0);
  }

  @Test(expectedExceptions = FormattedRuntimeException.class)
  public void testUnknownLeftHandTypeThrows () {

    comparator.compare(new UnsupportedNumber(), 1);
  }

  @Test(expectedExceptions = FormattedRuntimeException.class)
  public void testUnknownRightHandOfBigDecimalThrows () {

    comparator.compare(new BigDecimal("1"), new UnsupportedNumber());
  }

  @Test(expectedExceptions = FormattedRuntimeException.class)
  public void testUnknownRightHandOfBigIntegerThrows () {

    comparator.compare(BigInteger.ONE, new UnsupportedNumber());
  }

  @Test(expectedExceptions = FormattedRuntimeException.class)
  public void testUnknownRightHandOfIntegralThrows () {

    comparator.compare(1L, new UnsupportedNumber());
  }

  @Test(expectedExceptions = FormattedRuntimeException.class)
  public void testUnknownRightHandOfFloatingThrows () {

    comparator.compare(1.0d, new UnsupportedNumber());
  }

  private static class UnsupportedNumber extends Number {

    @Override
    public int intValue () {

      return 0;
    }

    @Override
    public long longValue () {

      return 0;
    }

    @Override
    public float floatValue () {

      return 0;
    }

    @Override
    public double doubleValue () {

      return 0;
    }
  }
}
