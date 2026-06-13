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
 * Covers the {@link WherePermit} hierarchy: the static factory helpers, permit-type discriminators, the
 * blank-entity normalization in the constructor, entity-aware {@code equals}/{@code hashCode}, and the
 * subtype {@code toString} formatting.
 */
@Test(groups = "unit")
public class WherePermitTest {

  public void testStaticFactoriesProduceExpectedTypes () {

    Assert.assertEquals(WherePermit.allowed("p", "a").getType(), PermitType.ALLOWED);
    Assert.assertEquals(WherePermit.required("p", "a").getType(), PermitType.REQUIRED);
    Assert.assertEquals(WherePermit.excluded("p", "a").getType(), PermitType.EXCLUDED);
    Assert.assertEquals(WherePermit.dependent("p", "a", new TargetWherePermit("b")).getType(), PermitType.DEPENDENT);
  }

  public void testTargetPermitTypeAndFactory () {

    TargetWherePermit target = TargetWherePermit.instance("p", "name");

    Assert.assertEquals(target.getType(), PermitType.ALLOWED);
    Assert.assertEquals(target.getEntity(), "p");
    Assert.assertEquals(target.getName(), "name");
    Assert.assertEquals(new TargetWherePermit("name").getEntity(), null);
  }

  public void testBlankEntityNormalizedToNull () {

    Assert.assertNull(new AllowedWherePermit("", "name").getEntity());
    Assert.assertNull(new AllowedWherePermit("   ", "name").getEntity());
    Assert.assertEquals(new AllowedWherePermit("p", "name").getEntity(), "p");
  }

  public void testToStringWithAndWithoutEntity () {

    Assert.assertEquals(new TargetWherePermit("name").toString(), "name");
    Assert.assertEquals(new TargetWherePermit("p", "name").toString(), "p.name");
    Assert.assertEquals(new AllowedWherePermit("p", "name").toString(), "Allowed p.name");
    Assert.assertEquals(new DependentWherePermit("discount", new TargetWherePermit("customerId")).toString(), "discount requires customerId");
  }

  public void testEqualsAndHashCodeWithoutEntity () {

    AllowedWherePermit first = new AllowedWherePermit("name");
    ExcludedWherePermit second = new ExcludedWherePermit("name");

    Assert.assertEquals(first, second);
    Assert.assertEquals(first.hashCode(), second.hashCode());
    Assert.assertEquals(first.hashCode(), "name".hashCode());
  }

  public void testEqualsAndHashCodeWithEntity () {

    AllowedWherePermit first = new AllowedWherePermit("p", "name");
    AllowedWherePermit second = new AllowedWherePermit("p", "name");
    AllowedWherePermit other = new AllowedWherePermit("q", "name");

    Assert.assertEquals(first, second);
    Assert.assertEquals(first.hashCode(), second.hashCode());
    Assert.assertNotEquals(first, other);
  }

  public void testEqualsRejectsNonPermitAndNull () {

    AllowedWherePermit permit = new AllowedWherePermit("name");

    Assert.assertNotEquals(permit, "name");
    Assert.assertNotEquals(permit, null);
  }

  public void testEntityDistinguishesFromDefaultEntity () {

    Assert.assertNotEquals(new AllowedWherePermit("p", "name"), new AllowedWherePermit("name"));
  }

  public void testDependentRequirementAccessor () {

    TargetWherePermit requirement = new TargetWherePermit("customerId");
    DependentWherePermit dependent = WherePermit.dependent("p", "discount", requirement);

    Assert.assertSame(dependent.getRequirement(), requirement);
    Assert.assertEquals(dependent.getEntity(), "p");
    Assert.assertEquals(dependent.getName(), "discount");
  }

  public void testRequiredAndExcludedConstructorVariants () {

    Assert.assertEquals(new RequiredWherePermit("p", "a").getType(), PermitType.REQUIRED);
    Assert.assertEquals(new RequiredWherePermit("a").getType(), PermitType.REQUIRED);
    Assert.assertEquals(new ExcludedWherePermit("p", "a").getType(), PermitType.EXCLUDED);
    Assert.assertEquals(new ExcludedWherePermit("a").getType(), PermitType.EXCLUDED);
    Assert.assertEquals(new AllowedWherePermit("a").getType(), PermitType.ALLOWED);
  }
}
