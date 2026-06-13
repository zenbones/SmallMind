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

import org.testng.annotations.Test;

/**
 * Exercises the permit rule engine in {@link WherePermissible#validate} against {@link Where} clauses:
 * allowed whitelists, required fields, excluded blacklists, and field dependencies.
 */
@Test(groups = "unit")
public class WherePermissibleValidationTest {

  private Where whereOn (String... fieldNames) {

    WhereCriterion[] criteria = new WhereCriterion[fieldNames.length];

    for (int index = 0; index < fieldNames.length; index++) {
      criteria[index] = WhereField.instance(fieldNames[index], WhereOperator.EQ, StringWhereOperand.instance("v"));
    }

    return Where.instance(new AndWhereConjunction(criteria));
  }

  public void testNoPermitsAlwaysPasses ()
    throws Exception {

    whereOn("age", "status").validate();
    whereOn("age", "status").validate((WherePermit[])null);
  }

  public void testAllowedWhitelistPasses ()
    throws Exception {

    whereOn("age").validate(new AllowedWherePermit("age"), new AllowedWherePermit("status"));
  }

  @Test(expectedExceptions = WhereValidationException.class)
  public void testFieldOutsideAllowedWhitelistRejected ()
    throws Exception {

    whereOn("age", "secret").validate(new AllowedWherePermit("age"));
  }

  public void testRequiredFieldPresentPasses ()
    throws Exception {

    // A required permit also acts as an allow-list entry, so only required/allowed fields may appear.
    whereOn("age").validate(new RequiredWherePermit("age"));
  }

  @Test(expectedExceptions = WhereValidationException.class)
  public void testRequiredFieldAbsentRejected ()
    throws Exception {

    whereOn("status").validate(new RequiredWherePermit("age"));
  }

  @Test(expectedExceptions = WhereValidationException.class)
  public void testExcludedFieldPresentRejected ()
    throws Exception {

    whereOn("age", "secret").validate(new ExcludedWherePermit("secret"));
  }

  public void testExcludedFieldAbsentPasses ()
    throws Exception {

    whereOn("age").validate(new ExcludedWherePermit("secret"));
  }

  public void testDependencySatisfiedPasses ()
    throws Exception {

    whereOn("discount", "customerId").validate(new DependentWherePermit("discount", new TargetWherePermit("customerId")));
  }

  @Test(expectedExceptions = WhereValidationException.class)
  public void testDependencyUnmetRejected ()
    throws Exception {

    whereOn("discount").validate(new DependentWherePermit("discount", new TargetWherePermit("customerId")));
  }

  public void testDependencyInactiveWhenTriggerAbsent ()
    throws Exception {

    // The dependency only fires when the dependent field is itself referenced.
    whereOn("customerId").validate(new DependentWherePermit("discount", new TargetWherePermit("customerId")));
  }

  public void testSortValidatesAgainstItsFields ()
    throws Exception {

    Sort.instance(SortField.instance("age", org.smallmind.nutsnbolts.json.SortDirection.ASC)).validate(new AllowedWherePermit("age"));
  }

  @Test(expectedExceptions = WhereValidationException.class)
  public void testSortFieldOutsideAllowedRejected ()
    throws Exception {

    Sort.instance(SortField.instance("secret", org.smallmind.nutsnbolts.json.SortDirection.DESC)).validate(new AllowedWherePermit("age"));
  }
}
