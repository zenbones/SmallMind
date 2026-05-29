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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class IteratorAdaptersTest {

  public void testIteratorEnumerationDelegatesHasMoreAndNext () {

    Enumeration<Integer> enumeration = new IteratorEnumeration<>(Arrays.asList(1, 2).iterator());

    List<Integer> collected = new ArrayList<>();
    while (enumeration.hasMoreElements()) {
      collected.add(enumeration.nextElement());
    }

    Assert.assertEquals(collected, Arrays.asList(1, 2));
  }

  public void testEnumerationIteratorDelegatesHasNextAndNext () {

    Enumeration<String> enumeration = Collections.enumeration(Arrays.asList("a", "b"));
    EnumerationIterator<String> iterator = new EnumerationIterator<>(enumeration);

    List<String> collected = new ArrayList<>();
    for (String value : iterator) {
      collected.add(value);
    }

    Assert.assertEquals(collected, Arrays.asList("a", "b"));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testEnumerationIteratorRemoveRejected () {

    EnumerationIterator<String> iterator = new EnumerationIterator<>(Collections.enumeration(Arrays.asList("only")));

    iterator.next();
    iterator.remove();
  }

  public void testEnumerationIteratorIteratorMethodReturnsThis () {

    EnumerationIterator<String> iterator = new EnumerationIterator<>(Collections.enumeration(Arrays.asList("x")));

    Assert.assertSame(iterator.iterator(), iterator);
  }

  public void testSingleItemIterableYieldsItemOnce () {

    SingleItemIterable<String> iterable = new SingleItemIterable<>("only");

    Iterator<String> iterator = iterable.iterator();

    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(iterator.next(), "only");
    Assert.assertFalse(iterator.hasNext());
  }

  public void testSingleItemIterableWithNullYieldsNothing () {

    SingleItemIterable<String> iterable = new SingleItemIterable<>(null);

    Assert.assertFalse(iterable.iterator().hasNext());
  }

  @Test(expectedExceptions = NoSuchElementException.class)
  public void testSingleItemIterableNextAfterConsumedThrows () {

    SingleItemIterable<String> iterable = new SingleItemIterable<>("x");
    Iterator<String> iterator = iterable.iterator();

    iterator.next();
    iterator.next();
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testSingleItemIterableRemoveRejected () {

    SingleItemIterable<String> iterable = new SingleItemIterable<>("x");

    iterable.iterator().remove();
  }

  public void testEmptyIterableYieldsNothing () {

    EmptyIterable<String> empty = new EmptyIterable<>();

    Assert.assertFalse(empty.iterator().hasNext());
  }
}
