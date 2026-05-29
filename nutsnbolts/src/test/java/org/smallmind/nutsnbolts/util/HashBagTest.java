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
import java.util.HashMap;
import java.util.Iterator;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class HashBagTest {

  public void testEmptyBagReportsEmptyAndZeroSize () {

    HashBag<String> bag = new HashBag<>();

    Assert.assertTrue(bag.isEmpty());
    Assert.assertEquals(bag.size(), 0);
  }

  public void testAddAccumulatesMultiplicityIntoSize () {

    HashBag<String> bag = new HashBag<>();

    bag.add("x");
    bag.add("x");
    bag.add("y");

    Assert.assertEquals(bag.size(), 3);
    Assert.assertEquals(bag.get("x"), Integer.valueOf(2));
    Assert.assertEquals(bag.get("y"), Integer.valueOf(1));
    Assert.assertEquals(bag.keySet().size(), 2);
  }

  public void testAddWithMultipleIncrementsCount () {

    HashBag<String> bag = new HashBag<>();

    bag.add("x", 5);

    Assert.assertEquals(bag.size(), 5);
    Assert.assertEquals(bag.get("x"), Integer.valueOf(5));
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testAddZeroMultipleIsRejected () {

    new HashBag<String>().add("x", 0);
  }

  public void testRemoveDecrementsOccurrenceCount () {

    HashBag<String> bag = new HashBag<>();

    bag.add("x", 3);
    bag.remove("x");

    Assert.assertEquals(bag.size(), 2);
    Assert.assertEquals(bag.get("x"), Integer.valueOf(2));
  }

  public void testRemoveLastOccurrenceRemovesKeyEntirely () {

    HashBag<String> bag = new HashBag<>();

    bag.add("x");
    bag.remove("x");

    Assert.assertFalse(bag.contains("x"));
    Assert.assertNull(bag.get("x"));
    Assert.assertEquals(bag.size(), 0);
  }

  public void testRemoveExceedingCountRemovesAllOccurrences () {

    HashBag<String> bag = new HashBag<>();

    bag.add("x", 3);
    bag.remove("x", 10);

    Assert.assertEquals(bag.size(), 0);
    Assert.assertFalse(bag.contains("x"));
  }

  public void testRemoveMissingElementReturnsFalse () {

    Assert.assertFalse(new HashBag<String>().remove("missing"));
  }

  public void testIteratorYieldsEachOccurrence () {

    HashBag<String> bag = new HashBag<>();

    bag.add("x", 2);
    bag.add("y", 1);

    int count = 0;
    Iterator<String> iterator = bag.iterator();

    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }

    Assert.assertEquals(count, 3);
  }

  public void testCollectionConstructorAddsEachElementOnce () {

    HashBag<String> bag = new HashBag<>(Arrays.asList("a", "b", "a", "c", "a"));

    Assert.assertEquals(bag.size(), 5);
    Assert.assertEquals(bag.get("a"), Integer.valueOf(3));
  }

  public void testRetainAllReducesMultiplicitiesToMatchCollection () {

    HashBag<String> bag = new HashBag<>();

    bag.add("x", 5);
    bag.add("y", 2);
    bag.retainAll(Arrays.asList("x", "x"));

    Assert.assertEquals(bag.get("x"), Integer.valueOf(2));
    Assert.assertFalse(bag.contains("y"));
  }

  public void testClearRemovesEverything () {

    HashBag<String> bag = new HashBag<>();

    bag.add("x", 3);
    bag.add("y", 2);
    bag.clear();

    Assert.assertTrue(bag.isEmpty());
    Assert.assertEquals(bag.size(), 0);
  }

  public void testContainsAllChecksMultiplicities () {

    HashBag<String> bag = new HashBag<>();

    bag.add("x", 3);
    bag.add("y", 1);

    HashBag<String> required = new HashBag<>();

    required.add("x", 2);
    required.add("y", 1);

    Assert.assertTrue(bag.containsAll(required));

    required.add("x", 2);
    Assert.assertFalse(bag.containsAll(required));
  }
}
