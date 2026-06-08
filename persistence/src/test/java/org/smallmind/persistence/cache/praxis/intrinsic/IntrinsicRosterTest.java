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
package org.smallmind.persistence.cache.praxis.intrinsic;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class IntrinsicRosterTest {

  private static IntrinsicRoster<String> roster (String... elements) {

    return new IntrinsicRoster<>(Arrays.asList(elements));
  }

  public void testCollectionConstructorPreservesOrderAndSize () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");

    Assert.assertEquals(roster.size(), 3);
    Assert.assertEquals(roster.get(0), "a");
    Assert.assertEquals(roster.get(1), "b");
    Assert.assertEquals(roster.get(2), "c");
  }

  public void testEmptyRosterReportsEmpty () {

    IntrinsicRoster<String> roster = new IntrinsicRoster<>();

    Assert.assertTrue(roster.isEmpty());
    Assert.assertEquals(roster.size(), 0);
  }

  public void testAddFirstAndAddLastPlaceElementsAtEnds () {

    IntrinsicRoster<String> roster = new IntrinsicRoster<>();

    roster.addLast("b");
    roster.addFirst("a");
    roster.addLast("c");

    Assert.assertEquals(roster.getFirst(), "a");
    Assert.assertEquals(roster.getLast(), "c");
    Assert.assertEquals(roster.size(), 3);
  }

  public void testGetReachesBothEndsOfTheList () {

    IntrinsicRoster<String> roster = roster("a", "b", "c", "d", "e");

    Assert.assertEquals(roster.get(0), "a");
    Assert.assertEquals(roster.get(4), "e");
    Assert.assertEquals(roster.get(3), "d");
  }

  public void testIndexOfAndLastIndexOf () {

    IntrinsicRoster<String> roster = roster("a", "b", "a");

    Assert.assertEquals(roster.indexOf("a"), 0);
    Assert.assertEquals(roster.lastIndexOf("a"), 2);
    Assert.assertEquals(roster.indexOf("z"), -1);
  }

  public void testContainsAndContainsAll () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");

    Assert.assertTrue(roster.contains("b"));
    Assert.assertFalse(roster.contains("z"));
    Assert.assertTrue(roster.containsAll(Arrays.asList("a", "c")));
    Assert.assertFalse(roster.containsAll(Arrays.asList("a", "z")));
    Assert.assertTrue(roster.containsAll(Arrays.asList()));
  }

  public void testSetReplacesElementAndReturnsPrevious () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");

    Assert.assertEquals(roster.set(1, "B"), "b");
    Assert.assertEquals(roster.get(1), "B");
    Assert.assertEquals(roster.size(), 3);
  }

  public void testAddAtIndexShiftsSubsequentElements () {

    IntrinsicRoster<String> roster = roster("a", "c");

    roster.add(1, "b");

    Assert.assertEquals(roster.get(0), "a");
    Assert.assertEquals(roster.get(1), "b");
    Assert.assertEquals(roster.get(2), "c");
  }

  public void testAddAtSizeAppends () {

    IntrinsicRoster<String> roster = roster("a", "b");

    roster.add(2, "c");

    Assert.assertEquals(roster.getLast(), "c");
    Assert.assertEquals(roster.size(), 3);
  }

  public void testRemoveFirstAndLastUpdateBoundaries () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");

    Assert.assertEquals(roster.removeFirst(), "a");
    Assert.assertEquals(roster.removeLast(), "c");
    Assert.assertEquals(roster.size(), 1);
    Assert.assertEquals(roster.getFirst(), "b");
    Assert.assertEquals(roster.getLast(), "b");
  }

  public void testRemoveByObjectRemovesFirstOccurrenceOnly () {

    IntrinsicRoster<String> roster = roster("a", "b", "a");

    Assert.assertTrue(roster.remove("a"));
    Assert.assertEquals(roster.size(), 2);
    Assert.assertEquals(roster.get(0), "b");
    Assert.assertEquals(roster.get(1), "a");
    Assert.assertFalse(roster.remove("z"));
  }

  public void testRemoveByIndexReturnsRemovedElement () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");

    Assert.assertEquals(roster.remove(1), "b");
    Assert.assertEquals(roster.size(), 2);
    Assert.assertEquals(roster.get(1), "c");
  }

  @Test(groups = "unit", expectedExceptions = NoSuchElementException.class)
  public void testRemoveFirstOnEmptyThrows () {

    new IntrinsicRoster<String>().removeFirst();
  }

  @Test(groups = "unit", expectedExceptions = IndexOutOfBoundsException.class)
  public void testGetOutOfBoundsThrows () {

    roster("a").get(5);
  }

  public void testToArrayReturnsElementsInOrder () {

    Object[] array = roster("a", "b", "c").toArray();

    Assert.assertEquals(array.length, 3);
    Assert.assertEquals(array, new Object[] {"a", "b", "c"});
  }

  public void testToArrayWithLargerArrayNullTerminatesAfterLastElement () {

    String[] destination = new String[5];

    String[] result = roster("a", "b").toArray(destination);

    Assert.assertSame(result, destination);
    Assert.assertEquals(result[0], "a");
    Assert.assertEquals(result[1], "b");
    Assert.assertNull(result[2]);
  }

  public void testIteratorWalksInOrder () {

    Iterator<String> iterator = roster("a", "b", "c").iterator();
    StringBuilder builder = new StringBuilder();

    while (iterator.hasNext()) {
      builder.append(iterator.next());
    }

    Assert.assertEquals(builder.toString(), "abc");
  }

  public void testSubListSharesElementsAndReportsViewSize () {

    List<String> view = roster("a", "b", "c", "d", "e").subList(1, 4);

    Assert.assertEquals(view.size(), 3);
    Assert.assertEquals(view.get(0), "b");
    Assert.assertEquals(view.get(2), "d");
    Assert.assertTrue(view.contains("c"));
    Assert.assertFalse(view.contains("a"));
    Assert.assertEquals(view.indexOf("d"), 2);
  }

  public void testAddAllAppendsAndAddAllAtIndexInserts () {

    IntrinsicRoster<String> roster = roster("a", "d");

    Assert.assertTrue(roster.addAll(Arrays.asList("e", "f")));
    Assert.assertEquals(roster.getLast(), "f");

    Assert.assertTrue(roster.addAll(1, Arrays.asList("b", "c")));
    Assert.assertEquals(roster.get(1), "b");
    Assert.assertEquals(roster.get(2), "c");
    Assert.assertEquals(roster.get(3), "d");
  }

  public void testAddAllAtIndexEqualToSizeAppends () {

    IntrinsicRoster<String> roster = roster("a", "b");

    // addAll at an index equal to size must append (mirroring add(int, T)), not throw IndexOutOfBoundsException.
    Assert.assertTrue(roster.addAll(roster.size(), Arrays.asList("c", "d")));
    Assert.assertEquals(roster.size(), 4);
    Assert.assertEquals(roster.get(2), "c");
    Assert.assertEquals(roster.get(3), "d");
  }

  public void testRemoveAllAndRetainAll () {

    IntrinsicRoster<String> removeTarget = roster("a", "b", "c", "d");

    Assert.assertTrue(removeTarget.removeAll(Arrays.asList("b", "d")));
    Assert.assertEquals(removeTarget.size(), 2);
    Assert.assertEquals(removeTarget.get(0), "a");
    Assert.assertEquals(removeTarget.get(1), "c");

    IntrinsicRoster<String> retainTarget = roster("a", "b", "c", "d");

    Assert.assertTrue(retainTarget.retainAll(Arrays.asList("b", "d")));
    Assert.assertEquals(retainTarget.size(), 2);
    Assert.assertEquals(retainTarget.get(0), "b");
    Assert.assertEquals(retainTarget.get(1), "d");
  }

  public void testClearEmptiesRoster () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");

    roster.clear();

    Assert.assertTrue(roster.isEmpty());
    Assert.assertEquals(roster.size(), 0);
  }

  public void testListIteratorAtNonZeroIndexTraversesBidirectionally () {

    ListIterator<String> iterator = roster("a", "b", "c", "d").listIterator(2);

    Assert.assertEquals(iterator.nextIndex(), 2);
    Assert.assertEquals(iterator.previousIndex(), 1);

    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(iterator.next(), "c");
    Assert.assertEquals(iterator.next(), "d");
    Assert.assertFalse(iterator.hasNext());

    Assert.assertTrue(iterator.hasPrevious());
    Assert.assertEquals(iterator.previous(), "d");
    Assert.assertEquals(iterator.previous(), "c");
    Assert.assertEquals(iterator.previous(), "b");
    Assert.assertEquals(iterator.previousIndex(), 0);
  }

  @Test(groups = "unit", expectedExceptions = IndexOutOfBoundsException.class)
  public void testListIteratorBeyondSizeThrows () {

    roster("a", "b").listIterator(5);
  }

  public void testSubListMidRangeOperationsShowThroughToParent () {

    IntrinsicRoster<String> roster = roster("a", "b", "c", "d", "e");
    List<String> view = roster.subList(1, 4);

    Assert.assertEquals(view.size(), 3);
    Assert.assertEquals(view.get(0), "b");
    Assert.assertEquals(view.get(2), "d");

    Assert.assertEquals(view.set(1, "C"), "c");
    Assert.assertEquals(view.get(1), "C");
    Assert.assertEquals(roster.get(2), "C");

    Assert.assertEquals(view.remove(0), "b");
    Assert.assertEquals(view.size(), 2);
    Assert.assertEquals(roster.size(), 4);
    Assert.assertEquals(roster.get(1), "C");
  }

  public void testSubListEmptyRangeIsEmptyView () {

    List<String> view = roster("a", "b", "c").subList(1, 1);

    Assert.assertTrue(view.isEmpty());
    Assert.assertEquals(view.size(), 0);
  }

  @Test(groups = "unit", expectedExceptions = IndexOutOfBoundsException.class)
  public void testSubListFromGreaterThanToThrows () {

    roster("a", "b", "c").subList(2, 1);
  }

  public void testLastIndexOfReturnsLatestOccurrence () {

    IntrinsicRoster<String> roster = roster("a", "b", "a", "c", "a");

    Assert.assertEquals(roster.lastIndexOf("a"), 4);
    Assert.assertEquals(roster.lastIndexOf("b"), 1);
    Assert.assertEquals(roster.lastIndexOf("z"), -1);
  }

  public void testGetFirstAndGetLastReturnBoundaryElements () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");

    Assert.assertEquals(roster.getFirst(), "a");
    Assert.assertEquals(roster.getLast(), "c");
  }

  @Test(groups = "unit", expectedExceptions = NoSuchElementException.class)
  public void testGetFirstOnEmptyThrows () {

    new IntrinsicRoster<String>().getFirst();
  }

  @Test(groups = "unit", expectedExceptions = NoSuchElementException.class)
  public void testGetLastOnEmptyThrows () {

    new IntrinsicRoster<String>().getLast();
  }

  public void testRemoveLastReturnsAndUnlinksTail () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");

    Assert.assertEquals(roster.removeLast(), "c");
    Assert.assertEquals(roster.size(), 2);
    Assert.assertEquals(roster.getLast(), "b");
  }

  @Test(groups = "unit", expectedExceptions = NoSuchElementException.class)
  public void testRemoveLastOnEmptyThrows () {

    new IntrinsicRoster<String>().removeLast();
  }

  public void testToArrayWithExactlySizedArrayReusesIt () {

    String[] destination = new String[3];

    String[] result = roster("a", "b", "c").toArray(destination);

    Assert.assertSame(result, destination);
    Assert.assertEquals(result, new String[] {"a", "b", "c"});
  }

  public void testToArrayWithTooSmallArrayAllocatesNewArray () {

    String[] destination = new String[1];

    String[] result = roster("a", "b", "c").toArray(destination);

    Assert.assertNotSame(result, destination);
    Assert.assertEquals(result.length, 3);
    Assert.assertEquals(result, new String[] {"a", "b", "c"});
  }

  public void testAddAllWithEmptyCollectionDoesNotChangeRoster () {

    IntrinsicRoster<String> roster = roster("a", "b");

    Assert.assertFalse(roster.addAll(Arrays.<String>asList()));
    Assert.assertFalse(roster.addAll(1, Arrays.<String>asList()));
    Assert.assertEquals(roster.size(), 2);
  }

  public void testRemoveAllWithEmptyCollectionReturnsFalse () {

    IntrinsicRoster<String> roster = roster("a", "b");

    Assert.assertFalse(roster.removeAll(Arrays.<String>asList()));
    Assert.assertEquals(roster.size(), 2);
  }

  public void testRemoveAllWithNoMatchingElementsReturnsFalse () {

    IntrinsicRoster<String> roster = roster("a", "b");

    Assert.assertFalse(roster.removeAll(Arrays.asList("y", "z")));
    Assert.assertEquals(roster.size(), 2);
  }

  public void testRetainAllWithEmptyCollectionReturnsFalse () {

    IntrinsicRoster<String> roster = roster("a", "b");

    Assert.assertFalse(roster.retainAll(Arrays.<String>asList()));
    Assert.assertEquals(roster.size(), 2);
  }

  public void testRetainAllWithAllElementsPresentReturnsFalse () {

    IntrinsicRoster<String> roster = roster("a", "b");

    Assert.assertFalse(roster.retainAll(Arrays.asList("a", "b")));
    Assert.assertEquals(roster.size(), 2);
  }
}
