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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MultipleIteratorTest {

  public void testEmptyIteratorReportsNoNext () {

    MultipleIterator<String> iterator = new MultipleIterator<>();

    Assert.assertFalse(iterator.hasNext());
  }

  public void testSingleIteratorReplaysAllElements () {

    MultipleIterator<Integer> iterator = new MultipleIterator<>();

    iterator.add(Arrays.asList(1, 2, 3).iterator());
    iterator.done();

    List<Integer> collected = new ArrayList<>();
    while (iterator.hasNext()) {
      collected.add(iterator.next());
    }

    Assert.assertEquals(collected, Arrays.asList(1, 2, 3));
  }

  public void testChainAdvancesToNextIteratorWhenExhausted () {

    MultipleIterator<Integer> iterator = new MultipleIterator<>();

    iterator.add(Arrays.asList(1, 2).iterator());
    iterator.add(Arrays.asList(3, 4).iterator());
    iterator.add(Arrays.asList(5).iterator());
    iterator.done();

    List<Integer> collected = new ArrayList<>();
    for (Integer value : iterator) {
      collected.add(value);
    }

    Assert.assertEquals(collected, Arrays.asList(1, 2, 3, 4, 5));
  }

  public void testEmptyConstituentIteratorsAreSkipped () {

    MultipleIterator<String> iterator = new MultipleIterator<>();

    iterator.add(new ArrayList<String>().iterator());
    iterator.add(Arrays.asList("only").iterator());
    iterator.add(new ArrayList<String>().iterator());
    iterator.done();

    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(iterator.next(), "only");
    Assert.assertFalse(iterator.hasNext());
  }

  @Test(expectedExceptions = NoSuchElementException.class)
  public void testNextOnExhaustedIteratorThrows () {

    MultipleIterator<String> iterator = new MultipleIterator<>();

    iterator.done();
    iterator.next();
  }

  public void testRemoveDelegatesToCurrentConstituentIterator () {

    LinkedList<String> backing = new LinkedList<>(Arrays.asList("a", "b"));
    MultipleIterator<String> iterator = new MultipleIterator<>();

    iterator.add(backing.iterator());
    iterator.done();

    iterator.next();
    iterator.remove();

    Assert.assertEquals(backing, Arrays.asList("b"));
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testRemoveBeforeAnyConstituentIteratorThrows () {

    MultipleIterator<String> iterator = new MultipleIterator<>();

    iterator.done();
    iterator.remove();
  }

  public void testIteratorMethodReturnsThis () {

    MultipleIterator<String> iterator = new MultipleIterator<>();

    Assert.assertSame(iterator.iterator(), iterator);
  }
}
