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
package org.smallmind.nutsnbolts.layout;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ConstraintTest {

  public void testDefaultConstructorYieldsRigidConstraint () {

    Constraint constraint = new Constraint();

    Assert.assertEquals(constraint.getGrow(), 0.0D);
    Assert.assertEquals(constraint.getShrink(), 0.0D);
  }

  public void testTwoArgConstructorStoresGrowAndShrink () {

    Constraint constraint = new Constraint(0.75D, 0.25D);

    Assert.assertEquals(constraint.getGrow(), 0.75D);
    Assert.assertEquals(constraint.getShrink(), 0.25D);
  }

  public void testSharedFactoryInstancesAreIdentitySingletons () {

    Assert.assertSame(Constraint.immutable(), Constraint.immutable());
    Assert.assertSame(Constraint.expand(), Constraint.expand());
    Assert.assertSame(Constraint.contract(), Constraint.contract());
    Assert.assertSame(Constraint.stretch(), Constraint.stretch());
  }

  public void testExpandHasGrowButNoShrink () {

    Constraint constraint = Constraint.expand();

    Assert.assertTrue(constraint.getGrow() > 0.0D);
    Assert.assertEquals(constraint.getShrink(), 0.0D);
  }

  public void testContractHasShrinkButNoGrow () {

    Constraint constraint = Constraint.contract();

    Assert.assertEquals(constraint.getGrow(), 0.0D);
    Assert.assertTrue(constraint.getShrink() > 0.0D);
  }

  public void testStretchHasBothGrowAndShrink () {

    Constraint constraint = Constraint.stretch();

    Assert.assertTrue(constraint.getGrow() > 0.0D);
    Assert.assertTrue(constraint.getShrink() > 0.0D);
  }
}
