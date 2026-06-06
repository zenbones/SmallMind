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
public class MartianBase34Test {

  public void testRoundTripFirstGroupWithSafeDigitWidth () {

    for (long value : new long[] {0L, 1L, 42L, 1234L, 99999L}) {

      String encoded = MartianBase34.base10To34(value, 5, MartianBase34.Group.FIRST);

      Assert.assertEquals(encoded.length(), 5);
      Assert.assertEquals(MartianBase34.base34To10(encoded, MartianBase34.Group.FIRST), value);
    }
  }

  public void testRoundTripSecondGroupWithSafeDigitWidth () {

    for (long value : new long[] {0L, 7L, 555L, 100000L}) {

      String encoded = MartianBase34.base10To34(value, 5, MartianBase34.Group.SECOND);

      Assert.assertEquals(MartianBase34.base34To10(encoded, MartianBase34.Group.SECOND), value);
    }
  }

  public void testRoundTripThirdGroupWithSafeDigitWidth () {

    for (long value : new long[] {0L, 1234567L, 9999999L}) {

      String encoded = MartianBase34.base10To34(value, 9, MartianBase34.Group.THIRD);

      Assert.assertEquals(MartianBase34.base34To10(encoded, MartianBase34.Group.THIRD), value);
    }
  }

  public void testEncodedStringUsesOnlyAlphabetCharacters () {

    String encoded = MartianBase34.base10To34(123456L, 5, MartianBase34.Group.FIRST);

    for (int index = 0; index < encoded.length(); index++) {
      Assert.assertTrue(MartianBase34.NUMEROLOGY.indexOf(encoded.charAt(index)) >= 0,
        "non-alphabet char: " + encoded.charAt(index));
    }
  }

  public void testGroupsProduceDifferentEncodings () {

    String encodedFirst = MartianBase34.base10To34(100L, 5, MartianBase34.Group.FIRST);
    String encodedSecond = MartianBase34.base10To34(100L, 5, MartianBase34.Group.SECOND);

    Assert.assertNotEquals(encodedFirst, encodedSecond);
  }

  public void testGroupConstantsAreExposed () {

    Assert.assertEquals(MartianBase34.Group.FIRST.getMixConstant(), 648913L);
    Assert.assertEquals(MartianBase34.Group.SECOND.getMixConstant(), 247123L);
    Assert.assertEquals(MartianBase34.Group.THIRD.getMixConstant(), 7294612383675L);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeInputIsRejected () {

    MartianBase34.base10To34(-1L, 5, MartianBase34.Group.FIRST);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testValueExceedingDigitCapacityIsRejected () {

    MartianBase34.base10To34(35L, 1, MartianBase34.Group.FIRST);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDecodeWithNonAlphabetCharacterIsRejected () {

    MartianBase34.base34To10("!!!!!", MartianBase34.Group.FIRST);
  }
}
