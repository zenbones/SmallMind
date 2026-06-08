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
import java.util.ListIterator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link IntrinsicRosterIterator} obtained from an {@link IntrinsicRoster}: forward/backward
 * traversal and index bookkeeping, the {@link IllegalStateException} guards on {@code set}/{@code remove}
 * before any cursor movement, and the {@code add}/{@code remove} mutations that operate relative to the
 * last-returned element.
 */
@Test(groups = "unit")
public class IntrinsicRosterIteratorTest {

  private static IntrinsicRoster<String> roster (String... elements) {

    return new IntrinsicRoster<>(Arrays.asList(elements));
  }

  public void testForwardTraversalReportsValuesAndIndices () {

    ListIterator<String> iterator = roster("a", "b", "c").listIterator();

    Assert.assertFalse(iterator.hasPrevious());
    Assert.assertEquals(iterator.nextIndex(), 0);
    Assert.assertEquals(iterator.previousIndex(), -1);

    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(iterator.next(), "a");
    Assert.assertEquals(iterator.nextIndex(), 1);
    Assert.assertEquals(iterator.previousIndex(), 0);

    Assert.assertEquals(iterator.next(), "b");
    Assert.assertEquals(iterator.next(), "c");

    Assert.assertFalse(iterator.hasNext());
    Assert.assertEquals(iterator.nextIndex(), 3);
  }

  public void testBackwardTraversalWalksFromTail () {

    ListIterator<String> iterator = roster("a", "b", "c").listIterator(3);

    Assert.assertFalse(iterator.hasNext());
    Assert.assertTrue(iterator.hasPrevious());
    Assert.assertEquals(iterator.previousIndex(), 2);

    Assert.assertEquals(iterator.previous(), "c");
    Assert.assertEquals(iterator.previous(), "b");
    Assert.assertEquals(iterator.previous(), "a");

    Assert.assertFalse(iterator.hasPrevious());
    Assert.assertEquals(iterator.nextIndex(), 0);
  }

  public void testNextThenPreviousReturnsSameElement () {

    ListIterator<String> iterator = roster("a", "b", "c").listIterator();

    Assert.assertEquals(iterator.next(), "a");
    Assert.assertEquals(iterator.previous(), "a");
    Assert.assertEquals(iterator.nextIndex(), 0);
  }

  @Test(groups = "unit", expectedExceptions = IllegalStateException.class)
  public void testSetBeforeAnyCursorMovementThrows () {

    roster("a", "b").listIterator().set("z");
  }

  @Test(groups = "unit", expectedExceptions = IllegalStateException.class)
  public void testRemoveBeforeAnyCursorMovementThrows () {

    roster("a", "b").listIterator().remove();
  }

  @Test(groups = "unit", expectedExceptions = IllegalStateException.class)
  public void testAddBeforeAnyCursorMovementThrows () {

    roster("a", "b").listIterator().add("z");
  }

  public void testSetReplacesLastReturnedElement () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");
    ListIterator<String> iterator = roster.listIterator();

    iterator.next();
    iterator.set("A");

    Assert.assertEquals(roster.get(0), "A");
    Assert.assertEquals(roster.size(), 3);
  }

  public void testAddInsertsBeforeTheLastReturnedElement () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");
    ListIterator<String> iterator = roster.listIterator();

    iterator.next();
    iterator.add("z");

    Assert.assertEquals(roster.size(), 4);
    Assert.assertEquals(roster.get(0), "z");
    Assert.assertEquals(roster.get(1), "a");
    Assert.assertEquals(roster.get(2), "b");
  }

  public void testRemoveDeletesLastReturnedElementFromRoster () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");
    ListIterator<String> iterator = roster.listIterator();

    iterator.next();
    iterator.remove();

    Assert.assertEquals(roster.size(), 2);
    Assert.assertEquals(roster.get(0), "b");
    Assert.assertEquals(roster.get(1), "c");
  }

  public void testRemoveAfterPreviousDeletesThatElement () {

    IntrinsicRoster<String> roster = roster("a", "b", "c");
    ListIterator<String> iterator = roster.listIterator(3);

    Assert.assertEquals(iterator.previous(), "c");
    iterator.remove();

    Assert.assertEquals(roster.size(), 2);
    Assert.assertEquals(roster.get(0), "a");
    Assert.assertEquals(roster.get(1), "b");
  }

  @Test(groups = "unit", expectedExceptions = IllegalStateException.class)
  public void testRemoveTwiceInARowThrows () {

    ListIterator<String> iterator = roster("a", "b", "c").listIterator();

    iterator.next();
    iterator.remove();
    // current is cleared after a remove, so a second remove with no intervening next()/previous() throws.
    iterator.remove();
  }
}
