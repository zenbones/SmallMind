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
package org.smallmind.web.json.query;

import java.util.Set;
import org.smallmind.nutsnbolts.json.SortDirection;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the {@link Sort}/{@link SortField} value model: emptiness, target collection, and the
 * name-only equality contract of {@link SortField}.
 */
@Test(groups = "unit")
public class SortTest {

  public void testEmptySort () {

    Assert.assertTrue(new Sort().isEmpty());
    Assert.assertTrue(Sort.instance().isEmpty());
    Assert.assertFalse(Sort.instance(SortField.instance("age", SortDirection.ASC)).isEmpty());
  }

  public void testGetTargetSetOnePermitPerField () {

    Sort sort = Sort.instance(
      SortField.instance("created", SortDirection.DESC),
      SortField.instance("orders", "total", SortDirection.ASC));

    Set<WherePermit> targetSet = sort.getTargetSet();

    Assert.assertEquals(targetSet.size(), 2);
    Assert.assertTrue(targetSet.contains(new TargetWherePermit("created")));
    Assert.assertTrue(targetSet.contains(new TargetWherePermit("orders", "total")));
  }

  public void testFieldAccessors () {

    SortField field = SortField.instance("orders", "total", SortDirection.DESC);

    Assert.assertEquals(field.getEntity(), "orders");
    Assert.assertEquals(field.getName(), "total");
    Assert.assertEquals(field.getDirection(), SortDirection.DESC);
  }

  public void testSortFieldEqualityIsNameOnly () {

    SortField one = SortField.instance("alpha", "age", SortDirection.ASC);
    SortField sameName = SortField.instance("beta", "age", SortDirection.DESC);
    SortField otherName = SortField.instance("alpha", "status", SortDirection.ASC);

    Assert.assertEquals(one, sameName);
    Assert.assertEquals(one.hashCode(), sameName.hashCode());
    Assert.assertNotEquals(one, otherName);
  }
}
