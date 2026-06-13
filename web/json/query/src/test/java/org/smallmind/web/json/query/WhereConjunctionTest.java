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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the child-criterion management shared by {@link WhereConjunction} subclasses, including the
 * null-filtering varargs constructor, lazy append, and snapshot accessors.
 */
@Test(groups = "unit")
public class WhereConjunctionTest {

  private WhereField field (String name) {

    return WhereField.instance(name, WhereOperator.EQ, StringWhereOperand.instance("v"));
  }

  public void testConjunctionTypes () {

    Assert.assertEquals(new AndWhereConjunction().getConjunctionType(), ConjunctionType.AND);
    Assert.assertEquals(new OrWhereConjunction().getConjunctionType(), ConjunctionType.OR);
    Assert.assertEquals(new AndWhereConjunction().getCriterionType(), CriterionType.CONJUNCTION);
  }

  public void testEmptyConjunction () {

    WhereConjunction conjunction = new AndWhereConjunction();

    Assert.assertTrue(conjunction.isEmpty());
    Assert.assertEquals(conjunction.size(), 0);
    Assert.assertEquals(conjunction.getCriteria().length, 0);
  }

  public void testVarargsConstructorIgnoresNulls () {

    WhereConjunction conjunction = new AndWhereConjunction(field("a"), null, field("b"));

    Assert.assertFalse(conjunction.isEmpty());
    Assert.assertEquals(conjunction.size(), 2);
    Assert.assertEquals(conjunction.getCriteria().length, 2);
  }

  public void testAddCriterionLazilyInitializes () {

    WhereConjunction conjunction = new OrWhereConjunction();

    conjunction.addCriterion(field("a"));
    conjunction.addCriterion(field("b"));

    Assert.assertEquals(conjunction.size(), 2);
  }

  public void testGetCriteriaReturnsInsertionOrder () {

    WhereConjunction conjunction = new AndWhereConjunction(field("first"), field("second"));

    WhereCriterion[] criteria = conjunction.getCriteria();

    Assert.assertEquals(((WhereField)criteria[0]).getName(), "first");
    Assert.assertEquals(((WhereField)criteria[1]).getName(), "second");
  }

  public void testSetCriteriaReplacesContents () {

    WhereConjunction conjunction = new AndWhereConjunction(field("old"));

    conjunction.setCriteria(field("new1"), field("new2"));

    Assert.assertEquals(conjunction.size(), 2);
    Assert.assertEquals(((WhereField)conjunction.getCriteria()[0]).getName(), "new1");
  }
}
