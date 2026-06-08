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
package org.smallmind.persistence.cache.praxis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.cache.praxis.intrinsic.ByReferenceIntrinsicVector;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the ordering, uniqueness, and max-size logic in {@link AbstractDurableVector#add} through the
 * concrete, reference-backed {@link ByReferenceIntrinsicVector}, which needs no DAO or cache infrastructure.
 * The fixture deliberately separates identity (the durable id, which drives {@code equals}) from sort key
 * (the {@code weight} field, which drives the comparator) so the re-zoning path can be observed.
 */
@Test(groups = "unit")
public class AbstractDurableVectorTest {

  private static final Comparator<Item> BY_WEIGHT = new Comparator<Item>() {

    @Override
    public int compare (Item left, Item right) {

      return Integer.compare(left.getWeight(), right.getWeight());
    }
  };

  private static ByReferenceIntrinsicVector<Long, Item> orderedVector (Comparator<Item> comparator, int maxSize) {

    return new ByReferenceIntrinsicVector<>(new IntrinsicRoster<Item>(), comparator, maxSize, 0, true);
  }

  private static ByReferenceIntrinsicVector<Long, Item> unorderedVector (int maxSize) {

    return new ByReferenceIntrinsicVector<>(new IntrinsicRoster<Item>(), null, maxSize, 0, false);
  }

  private static List<Long> idsOf (ByReferenceIntrinsicVector<Long, Item> vector) {

    List<Long> ids = new ArrayList<>();

    for (Item item : vector.getRoster()) {
      ids.add(item.getId());
    }

    return ids;
  }

  public void testNullDurableIsIgnoredAndReportsNoChange () {

    ByReferenceIntrinsicVector<Long, Item> vector = orderedVector(BY_WEIGHT, 0);

    Assert.assertFalse(vector.add(null));
    Assert.assertTrue(vector.getRoster().isEmpty());
  }

  public void testOrderedAddInsertsByComparatorPosition () {

    ByReferenceIntrinsicVector<Long, Item> vector = orderedVector(BY_WEIGHT, 0);

    Assert.assertTrue(vector.add(new Item(2L, 20)));
    Assert.assertTrue(vector.add(new Item(3L, 30)));
    Assert.assertTrue(vector.add(new Item(1L, 10)));

    Assert.assertEquals(idsOf(vector), List.of(1L, 2L, 3L));
    Assert.assertEquals(vector.head().getId(), Long.valueOf(1L));
  }

  public void testOrderedReAddWithSameSortKeyIsANoOp () {

    ByReferenceIntrinsicVector<Long, Item> vector = orderedVector(BY_WEIGHT, 0);

    vector.add(new Item(1L, 10));
    vector.add(new Item(2L, 20));

    Assert.assertFalse(vector.add(new Item(1L, 10)));
    Assert.assertEquals(idsOf(vector), List.of(1L, 2L));
  }

  public void testOrderedReAddWithChangedSortKeyRemovesAndReinserts () {

    ByReferenceIntrinsicVector<Long, Item> vector = orderedVector(BY_WEIGHT, 0);

    vector.add(new Item(1L, 10));
    vector.add(new Item(2L, 20));
    vector.add(new Item(3L, 30));

    // Item 2's weight drops below item 1's, so it must leave its old slot and re-zone to the front.
    Assert.assertTrue(vector.add(new Item(2L, 5)));
    Assert.assertEquals(idsOf(vector), List.of(2L, 1L, 3L));
    Assert.assertEquals(vector.getRoster().size(), 3);
  }

  public void testOrderedAddUsesNaturalOrderingWhenComparatorIsNull () {

    ByReferenceIntrinsicVector<Long, Item> vector = orderedVector(null, 0);

    vector.add(new Item(1L, 10));
    vector.add(new Item(2L, 20));
    vector.add(new Item(3L, 30));

    // AbstractDurable orders by id descending, so the natural-ordering branch yields 3, 2, 1.
    Assert.assertEquals(idsOf(vector), List.of(3L, 2L, 1L));
  }

  public void testOrderedMaxSizeEvictsTailWhenAFrontInsertOverflows () {

    ByReferenceIntrinsicVector<Long, Item> vector = orderedVector(BY_WEIGHT, 2);

    vector.add(new Item(2L, 20));
    vector.add(new Item(3L, 30));

    Assert.assertTrue(vector.add(new Item(1L, 10)));
    Assert.assertEquals(idsOf(vector), List.of(1L, 2L));
  }

  public void testUnorderedAddPrependsAndDeduplicatesByIdentity () {

    ByReferenceIntrinsicVector<Long, Item> vector = unorderedVector(0);

    Assert.assertTrue(vector.add(new Item(1L, 10)));
    Assert.assertTrue(vector.add(new Item(2L, 20)));

    Assert.assertFalse(vector.add(new Item(1L, 99)));
    Assert.assertEquals(idsOf(vector), List.of(2L, 1L));
  }

  public void testUnorderedMaxSizeEvictsOldestElement () {

    ByReferenceIntrinsicVector<Long, Item> vector = unorderedVector(2);

    vector.add(new Item(1L, 10));
    vector.add(new Item(2L, 20));
    vector.add(new Item(3L, 30));

    Assert.assertEquals(idsOf(vector), List.of(3L, 2L));
  }

  public void testRemoveReturnsWhetherTheVectorChanged () {

    ByReferenceIntrinsicVector<Long, Item> vector = unorderedVector(0);

    vector.add(new Item(1L, 10));

    Assert.assertTrue(vector.remove(new Item(1L, 10)));
    Assert.assertTrue(vector.getRoster().isEmpty());
    Assert.assertFalse(vector.remove(new Item(1L, 10)));
  }

  public void testHeadReturnsNullWhenEmptyAndFirstElementOtherwise () {

    ByReferenceIntrinsicVector<Long, Item> vector = orderedVector(BY_WEIGHT, 0);

    Assert.assertNull(vector.head());

    vector.add(new Item(5L, 50));

    Assert.assertEquals(vector.head().getId(), Long.valueOf(5L));
  }

  public static class Item extends AbstractDurable<Long, Item> {

    private Long id;
    private int weight;

    public Item (Long id, int weight) {

      this.id = id;
      this.weight = weight;
    }

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }

    public int getWeight () {

      return weight;
    }
  }
}
