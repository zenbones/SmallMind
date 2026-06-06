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
public class ArrayUtilityTest {

  public void testCloneReturnsIndependentCopy () {

    String[] original = {"a", "b", "c"};
    String[] copy = ArrayUtility.clone(String.class, original);

    Assert.assertNotSame(copy, original);
    Assert.assertEquals(copy, original);

    copy[0] = "z";
    Assert.assertEquals(original[0], "a");
  }

  public void testCloneReturnsNullForNullInput () {

    Assert.assertNull(ArrayUtility.clone(String.class, null));
  }

  public void testConcatenateJoinsArrays () {

    String[] result = ArrayUtility.concatenate(String.class, new String[] {"a", "b"}, "c", "d");

    Assert.assertEquals(result, new String[] {"a", "b", "c", "d"});
  }

  public void testConcatenateWithNullFirstReturnsSecond () {

    String[] result = ArrayUtility.concatenate(String.class, null, "x", "y");

    Assert.assertEquals(result, new String[] {"x", "y"});
  }

  public void testConcatenateWithBothNullReturnsEmptyArray () {

    String[] result = ArrayUtility.concatenate(String.class, (String[])null, (String[])null);

    Assert.assertEquals(result.length, 0);
  }

  public void testConcatenateWithEmptySecondReturnsFirst () {

    String[] first = {"a", "b"};

    Assert.assertSame(ArrayUtility.concatenate(String.class, first), first);
  }

  public void testLinearSearchFindsElement () {

    Assert.assertEquals(ArrayUtility.linearSearch(new String[] {"a", "b", "c"}, "b"), 1);
    Assert.assertEquals(ArrayUtility.linearSearch(new String[] {"a", "b", "c"}, "a"), 0);
    Assert.assertEquals(ArrayUtility.linearSearch(new String[] {"a", "b", "c"}, "c"), 2);
  }

  public void testLinearSearchReturnsMinusOneWhenNotFound () {

    Assert.assertEquals(ArrayUtility.linearSearch(new String[] {"a", "b"}, "z"), -1);
  }

  public void testLinearSearchHandlesNullKey () {

    Assert.assertEquals(ArrayUtility.linearSearch(new String[] {"a", null, "c"}, null), 1);
  }

  public void testLinearSearchOnNullOrEmptyReturnsMinusOne () {

    Assert.assertEquals(ArrayUtility.linearSearch((String[])null, "a"), -1);
    Assert.assertEquals(ArrayUtility.linearSearch(new String[0], "a"), -1);
  }
}
