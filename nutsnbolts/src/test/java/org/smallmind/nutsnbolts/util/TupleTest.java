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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TupleTest {

  public void testAddPairPreservesInsertionOrder () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("b", 2);
    tuple.addPair("c", 3);

    Assert.assertEquals(tuple.size(), 3);
    Assert.assertEquals(tuple.getKey(0), "a");
    Assert.assertEquals(tuple.getKey(2), "c");
    Assert.assertEquals(tuple.getValue(1), Integer.valueOf(2));
  }

  public void testAddPairAtIndexInsertsAndShiftsRight () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("c", 3);
    tuple.addPair(1, "b", 2);

    Assert.assertEquals(tuple.getKey(1), "b");
    Assert.assertEquals(tuple.getKey(2), "c");
  }

  public void testDuplicateKeysArePreservedSeparately () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("x", 1);
    tuple.addPair("x", 2);
    tuple.addPair("x", 3);

    Assert.assertEquals(tuple.size(), 3);
    Assert.assertEquals(tuple.getValues("x"), List.of(1, 2, 3));
    Assert.assertEquals(tuple.getUniqueKeys().size(), 1);
  }

  public void testGetValueByKeyReturnsFirstOccurrence () {

    Tuple<String, String> tuple = new Tuple<>();

    tuple.addPair("k", "first");
    tuple.addPair("k", "second");

    Assert.assertEquals(tuple.getValue("k"), "first");
    Assert.assertEquals(tuple.indexOfKey("k"), 0);
  }

  public void testGetValueReturnsNullForMissingKey () {

    Tuple<String, String> tuple = new Tuple<>();

    tuple.addPair("k", "v");

    Assert.assertNull(tuple.getValue("missing"));
    Assert.assertNull(tuple.getValues("missing"));
    Assert.assertEquals(tuple.indexOfKey("missing"), -1);
  }

  public void testSetPairUpdatesExistingKeyInPlace () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("b", 2);
    tuple.setPair("a", 99);

    Assert.assertEquals(tuple.size(), 2);
    Assert.assertEquals(tuple.getValue("a"), Integer.valueOf(99));
    Assert.assertEquals(tuple.getKey(0), "a");
  }

  public void testSetPairAppendsWhenKeyIsAbsent () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.setPair("b", 2);

    Assert.assertEquals(tuple.size(), 2);
    Assert.assertEquals(tuple.getKey(1), "b");
  }

  public void testRemoveKeyRemovesAllMatchingPairs () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("b", 2);
    tuple.addPair("a", 3);
    tuple.removeKey("a");

    Assert.assertEquals(tuple.size(), 1);
    Assert.assertEquals(tuple.getKey(0), "b");
  }

  public void testRemovePairByKeyRemovesOnlyFirstAndReturnsValue () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("a", 2);

    Assert.assertEquals(tuple.removePair("a"), Integer.valueOf(1));
    Assert.assertEquals(tuple.size(), 1);
    Assert.assertEquals(tuple.getValue("a"), Integer.valueOf(2));
  }

  public void testContainsKeyValuePair () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("a", 2);

    Assert.assertTrue(tuple.containsKeyValuePair("a", 1));
    Assert.assertTrue(tuple.containsKeyValuePair("a", 2));
    Assert.assertFalse(tuple.containsKeyValuePair("a", 99));
    Assert.assertFalse(tuple.containsKeyValuePair("missing", 1));
  }

  public void testAsMapGroupsValuesByKeyInOrder () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("b", 2);
    tuple.addPair("a", 3);

    Map<String, List<Integer>> map = tuple.asMap();

    Assert.assertEquals(map.size(), 2);
    Assert.assertEquals(map.get("a"), List.of(1, 3));
    Assert.assertEquals(map.get("b"), List.of(2));
  }

  public void testCloneProducesIndependentTupleWithSameContents () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("b", 2);

    @SuppressWarnings("unchecked")
    Tuple<String, Integer> copy = (Tuple<String, Integer>)tuple.clone();

    copy.addPair("c", 3);

    Assert.assertEquals(tuple.size(), 2);
    Assert.assertEquals(copy.size(), 3);
  }

  public void testIteratorYieldsPairsInOrder () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("b", 2);

    Iterator<Pair<String, Integer>> iterator = tuple.iterator();

    Assert.assertTrue(iterator.hasNext());
    Pair<String, Integer> first = iterator.next();
    Assert.assertEquals(first.first(), "a");
    Assert.assertEquals(first.second(), Integer.valueOf(1));

    Pair<String, Integer> second = iterator.next();
    Assert.assertEquals(second.first(), "b");
    Assert.assertFalse(iterator.hasNext());
  }

  @Test(expectedExceptions = ConcurrentModificationException.class)
  public void testIteratorThrowsWhenTupleMutatedExternally () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("b", 2);

    Iterator<Pair<String, Integer>> iterator = tuple.iterator();

    iterator.next();
    tuple.addPair("c", 3);
    iterator.next();
  }

  public void testIteratorRemoveDropsLastReturnedPair () {

    Tuple<String, Integer> tuple = new Tuple<>();

    tuple.addPair("a", 1);
    tuple.addPair("b", 2);
    tuple.addPair("c", 3);

    Iterator<Pair<String, Integer>> iterator = tuple.iterator();

    iterator.next();
    iterator.next();
    iterator.remove();

    Assert.assertEquals(tuple.size(), 2);
    Assert.assertEquals(tuple.getKey(0), "a");
    Assert.assertEquals(tuple.getKey(1), "c");
  }
}
