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
 * Covers the structural accessors of {@link Where}, {@link WhereField}, and the {@link WhereConjunction}
 * subclasses, including entity-scoped construction and the criterion-type discriminators.
 */
@Test(groups = "unit")
public class WhereStructureTest {

  public void testWhereRootAccessors () {

    AndWhereConjunction root = new AndWhereConjunction();
    Where where = new Where();
    where.setRootConjunction(root);

    Assert.assertSame(where.getRootConjunction(), root);
    Assert.assertSame(Where.instance(root).getRootConjunction(), root);
  }

  public void testWhereFieldDefaultEntityAccessors () {

    StringWhereOperand operand = StringWhereOperand.instance("v");
    WhereField field = new WhereField();
    field.setName("status");
    field.setOperator(WhereOperator.EQ);
    field.setOperand(operand);

    Assert.assertEquals(field.getCriterionType(), CriterionType.FIELD);
    Assert.assertNull(field.getEntity());
    Assert.assertEquals(field.getName(), "status");
    Assert.assertEquals(field.getOperator(), WhereOperator.EQ);
    Assert.assertSame(field.getOperand(), operand);
  }

  public void testWhereFieldEntityScopedConstructor () {

    WhereField field = WhereField.instance("p", "name", WhereOperator.LIKE, StringWhereOperand.instance("bo*"));

    Assert.assertEquals(field.getEntity(), "p");
    Assert.assertEquals(field.getName(), "name");
    Assert.assertEquals(field.getOperator(), WhereOperator.LIKE);

    field.setEntity("q");
    Assert.assertEquals(field.getEntity(), "q");
  }

  public void testWhereFieldDefaultEntityFactory () {

    WhereField field = WhereField.instance("age", WhereOperator.GE, IntegerWhereOperand.instance(18));

    Assert.assertNull(field.getEntity());
    Assert.assertEquals(field.getName(), "age");
  }

  public void testConjunctionFactories () {

    WhereField field = WhereField.instance("a", WhereOperator.EQ, StringWhereOperand.instance("v"));

    Assert.assertEquals(AndWhereConjunction.instance(field).getConjunctionType(), ConjunctionType.AND);
    Assert.assertEquals(OrWhereConjunction.instance(field).getConjunctionType(), ConjunctionType.OR);
    Assert.assertEquals(AndWhereConjunction.instance(field).size(), 1);
  }
}
