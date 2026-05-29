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

import java.util.HashSet;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SnowflakeIdTest {

  public void testByteSizeIsEighteen () {

    Assert.assertEquals(SnowflakeId.byteSize(), 18);
  }

  public void testNewInstanceProducesEighteenByteArray () {

    Assert.assertEquals(SnowflakeId.newInstance().asByteArray().length, 18);
  }

  public void testFirstByteIsZeroSentinel () {

    Assert.assertEquals(SnowflakeId.newInstance().asByteArray()[0], (byte)0);
  }

  public void testConsecutiveIdsAreUnique () {

    Set<SnowflakeId> ids = new HashSet<>();

    for (int i = 0; i < 1000; i++) {
      ids.add(SnowflakeId.newInstance());
    }

    Assert.assertEquals(ids.size(), 1000);
  }

  public void testConsecutiveIdsAreMonotonicallyOrdered () {

    SnowflakeId previous = SnowflakeId.newInstance();

    for (int i = 0; i < 100; i++) {

      SnowflakeId next = SnowflakeId.newInstance();

      Assert.assertTrue(previous.compareTo(next) < 0);
      previous = next;
    }
  }

  public void testEqualsBasedOnByteContent () {

    SnowflakeId original = SnowflakeId.newInstance();
    SnowflakeId copy = new SnowflakeId(original.asByteArray());

    Assert.assertEquals(original, copy);
    Assert.assertEquals(original.hashCode(), copy.hashCode());
  }

  public void testEqualsRejectsForeignTypes () {

    Assert.assertNotEquals(SnowflakeId.newInstance(), "not a snowflake");
  }

  public void testGenerateHexEncodingProducesThirtySixHexChars () {

    String hex = SnowflakeId.newInstance().generateHexEncoding();

    Assert.assertEquals(hex.length(), 36);
    Assert.assertTrue(hex.matches("[0-9a-fA-F]{36}"));
  }

  public void testGenerateBigIntegerMatchesByteArray () {

    SnowflakeId id = SnowflakeId.newInstance();

    Assert.assertEquals(id.generateBigInteger(), new java.math.BigInteger(id.asByteArray()));
  }

  public void testGenerateCompactStringIsNonEmptyAndPrintable () {

    String compact = SnowflakeId.newInstance().generateCompactString();

    Assert.assertNotNull(compact);
    Assert.assertFalse(compact.isEmpty());
  }

  public void testGenerateDottedStringContainsDots () {

    String dotted = SnowflakeId.newInstance().generateDottedString();

    Assert.assertTrue(dotted.contains("."));
  }
}
