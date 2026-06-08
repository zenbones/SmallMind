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

import java.util.Iterator;
import java.util.List;
import org.smallmind.persistence.AbstractDurable;
import org.smallmind.persistence.cache.DurableVector;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link ByReferenceSingularVector}, which holds exactly one durable by direct reference and so
 * needs no DAO. Replacement on {@link ByReferenceSingularVector#add} is governed by {@code AbstractDurable}
 * id-equality, so a same-id durable is treated as identical and a different-id durable triggers a swap.
 */
@Test(groups = "unit")
public class ByReferenceSingularVectorTest {

  public void testIsSingularIsAlwaysTrue () {

    Assert.assertTrue(new ByReferenceSingularVector<>(new Widget(1L), 0).isSingular());
  }

  public void testHeadReturnsTheStoredDurable () {

    Widget widget = new Widget(1L);

    Assert.assertSame(new ByReferenceSingularVector<>(widget, 0).head(), widget);
  }

  public void testAsBestEffortLazyListHoldsTheSingleDurable () {

    Widget widget = new Widget(1L);

    List<Widget> list = new ByReferenceSingularVector<>(widget, 0).asBestEffortLazyList();

    Assert.assertEquals(list.size(), 1);
    Assert.assertSame(list.get(0), widget);
  }

  public void testIteratorYieldsTheSingleDurableExactlyOnce () {

    Widget widget = new Widget(1L);

    Iterator<Widget> iterator = new ByReferenceSingularVector<>(widget, 0).iterator();

    Assert.assertTrue(iterator.hasNext());
    Assert.assertSame(iterator.next(), widget);
    Assert.assertFalse(iterator.hasNext());
  }

  public void testAddReplacesWhenDurableDiffersById () {

    ByReferenceSingularVector<Long, Widget> vector = new ByReferenceSingularVector<>(new Widget(1L), 0);

    Widget replacement = new Widget(2L);

    Assert.assertTrue(vector.add(replacement));
    Assert.assertSame(vector.head(), replacement);
  }

  public void testAddReturnsFalseWhenDurableHasSameId () {

    Widget original = new Widget(1L);
    ByReferenceSingularVector<Long, Widget> vector = new ByReferenceSingularVector<>(original, 0);

    // Same id means AbstractDurable.equals reports equality, so no replacement occurs.
    Assert.assertFalse(vector.add(new Widget(1L)));
    Assert.assertSame(vector.head(), original);
  }

  public void testCopyPreservesDurableAndTimeToLive () {

    Widget widget = new Widget(1L);
    ByReferenceSingularVector<Long, Widget> vector = new ByReferenceSingularVector<>(widget, 42);

    DurableVector<Long, Widget> copy = vector.copy();

    Assert.assertTrue(copy instanceof ByReferenceSingularVector);
    Assert.assertSame(copy.head(), widget);
    Assert.assertEquals(copy.getTimeToLiveSeconds(), 42);
    Assert.assertTrue(copy.isSingular());
  }

  @Test(groups = "unit", expectedExceptions = UnsupportedOperationException.class)
  public void testRemoveIsUnsupported () {

    new ByReferenceSingularVector<>(new Widget(1L), 0).remove(new Widget(1L));
  }

  public static class Widget extends AbstractDurable<Long, Widget> {

    private Long id;

    public Widget () {

    }

    public Widget (Long id) {

      this.id = id;
    }

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }
}
