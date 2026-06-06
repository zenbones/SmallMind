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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MutationUtilityTest {

  private static final Mutation<Integer, String> STRINGIFY = i -> "v" + i;

  public void testToArrayTransformsEveryElement ()
    throws MutationException {

    String[] result = MutationUtility.toArray(new Integer[] {1, 2, 3}, String.class, STRINGIFY);

    Assert.assertEquals(result, new String[] {"v1", "v2", "v3"});
  }

  public void testToArrayWithNullInputReturnsNull ()
    throws MutationException {

    Assert.assertNull(MutationUtility.toArray((Integer[])null, String.class, STRINGIFY));
  }

  public void testToListTransformsEveryElement ()
    throws MutationException {

    List<String> result = MutationUtility.toList(Arrays.asList(1, 2, 3), STRINGIFY);

    Assert.assertEquals(result, List.of("v1", "v2", "v3"));
  }

  public void testToSetDedupesTransformedValues ()
    throws MutationException {

    Set<String> result = MutationUtility.toSet(Arrays.asList(1, 2, 2, 3, 3), STRINGIFY);

    Assert.assertEquals(result, Set.of("v1", "v2", "v3"));
  }

  public void testToMapBuildsFromKeyAndValueMutations ()
    throws MutationException {

    Map<String, Integer> result = MutationUtility.toMap(Arrays.asList(1, 2, 3), STRINGIFY, i -> i * 10);

    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get("v1"), Integer.valueOf(10));
    Assert.assertEquals(result.get("v3"), Integer.valueOf(30));
  }

  public void testToBagCountsTransformedDuplicates ()
    throws MutationException {

    Bag<String> result = MutationUtility.toBag(Arrays.asList(1, 1, 2), STRINGIFY);

    Assert.assertEquals(result.size(), 3);
    Assert.assertTrue(result.contains("v1"));
  }

  @Test(expectedExceptions = MutationException.class)
  public void testApplyExceptionIsWrappedAsMutationException ()
    throws MutationException {

    MutationUtility.toList(Arrays.asList(1, 2, 3), i -> {
      throw new Exception("boom");
    });
  }

  public void testNullCollectionInputsReturnNullThroughout ()
    throws MutationException {

    Assert.assertNull(MutationUtility.toList((List<Integer>)null, STRINGIFY));
    Assert.assertNull(MutationUtility.toSet((Iterable<Integer>)null, STRINGIFY));
    Assert.assertNull(MutationUtility.toMap((List<Integer>)null, STRINGIFY, STRINGIFY));
    Assert.assertNull(MutationUtility.toBag((Iterable<Integer>)null, STRINGIFY));
  }
}
