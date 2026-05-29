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
public class AlphaNumericComparatorTest {

  private final AlphaNumericComparator<String> comparator = new AlphaNumericComparator<>();

  public void testEqualStringsCompareEqual () {

    Assert.assertEquals(comparator.compare("foo", "foo"), 0);
  }

  public void testDigitPositionSortsBeforeLetterPosition () {

    Assert.assertTrue(comparator.compare("a1", "ab") < 0);
    Assert.assertTrue(comparator.compare("ab", "a1") > 0);
  }

  public void testDigitsCompareByDigitValueAtSamePosition () {

    Assert.assertTrue(comparator.compare("a2", "a9") < 0);
  }

  public void testLetterCompareIsCaseInsensitive () {

    Assert.assertEquals(comparator.compare("Foo", "foo"), 0);
    Assert.assertTrue(comparator.compare("Apple", "banana") < 0);
  }

  public void testShorterStringSortsBeforeLongerWithCommonPrefix () {

    Assert.assertTrue(comparator.compare("abc", "abcd") < 0);
    Assert.assertTrue(comparator.compare("abcd", "abc") > 0);
  }

  public void testNullSortsBeforeNonNull () {

    Assert.assertTrue(comparator.compare(null, "x") < 0);
    Assert.assertTrue(comparator.compare("x", null) > 0);
    Assert.assertEquals(comparator.compare(null, null), 0);
  }

  public void testEmptyStringSortsBeforeNonEmpty () {

    Assert.assertTrue(comparator.compare("", "x") < 0);
    Assert.assertTrue(comparator.compare("x", "") > 0);
    Assert.assertEquals(comparator.compare("", ""), 0);
  }
}
