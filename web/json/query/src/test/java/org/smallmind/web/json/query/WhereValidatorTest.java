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

import jakarta.validation.ConstraintValidatorContext;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the {@link WhereValidator} Bean Validation adapter: it reads {@link WhereConstraint}
 * attributes into permit objects, returns {@code true} when validation passes, and reports a
 * constraint violation (returning {@code false}) when the underlying rule engine rejects the value.
 *
 * <p>The {@code @Allowed}/{@code @Excluded}/{@code @Required} {@code entity()} attribute defaults to
 * {@code ""}, while a query field built without an entity carries {@code null}. {@link WherePermit}
 * canonicalizes a blank entity to {@code null} in its constructor, so the annotation default matches a
 * default-entity field regardless of whether that field's entity is {@code null} or {@code ""}.
 * {@link #testNullEntityFieldMatchesAnnotationDefault} and
 * {@link #testEmptyEntityFieldMatchesAnnotationDefault} pin both representations.
 */
@Test(groups = "unit")
public class WhereValidatorTest {

  @WhereConstraint(allow = {@Allowed(field = "age"), @Allowed(field = "status")})
  private Where allowAgeAndStatus;

  @WhereConstraint(exclude = @Excluded(field = "secret"))
  private Where excludeSecret;

  private Where whereOn (String... fieldNames) {

    WhereCriterion[] criteria = new WhereCriterion[fieldNames.length];

    for (int index = 0; index < fieldNames.length; index++) {
      // Default-entity field: built with no entity, so entity == null — the normal client shape.
      criteria[index] = WhereField.instance(fieldNames[index], WhereOperator.EQ, StringWhereOperand.instance("v"));
    }

    return Where.instance(new AndWhereConjunction(criteria));
  }

  private WhereValidator validatorFor (String fieldName)
    throws NoSuchFieldException {

    WhereValidator validator = new WhereValidator();

    validator.initialize(WhereValidatorTest.class.getDeclaredField(fieldName).getAnnotation(WhereConstraint.class));

    return validator;
  }

  public void testValidQueryPasses ()
    throws NoSuchFieldException {

    // On the success path the context is never touched, so a null context is acceptable.
    Assert.assertTrue(validatorFor("allowAgeAndStatus").isValid(whereOn("age", "status"), null));
  }

  public void testDisallowedFieldReportsViolation ()
    throws NoSuchFieldException {

    ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class, Mockito.RETURNS_DEEP_STUBS);

    Assert.assertFalse(validatorFor("allowAgeAndStatus").isValid(whereOn("age", "danger"), context));

    Mockito.verify(context).disableDefaultConstraintViolation();
  }

  public void testExcludedFieldReportsViolation ()
    throws NoSuchFieldException {

    ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class, Mockito.RETURNS_DEEP_STUBS);

    Assert.assertFalse(validatorFor("excludeSecret").isValid(whereOn("secret"), context));
  }

  public void testExcludedConstraintAllowsCleanQuery ()
    throws NoSuchFieldException {

    Assert.assertTrue(validatorFor("excludeSecret").isValid(whereOn("age"), null));
  }

  public void testNullEntityFieldMatchesAnnotationDefault ()
    throws NoSuchFieldException {

    ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class, Mockito.RETURNS_DEEP_STUBS);

    // A field built without an entity has entity == null; the permit's blank entity is canonicalized to
    // null too, so the exclude rule now correctly catches it.
    Where nullEntitySecret = Where.instance(new AndWhereConjunction(WhereField.instance("secret", WhereOperator.EQ, StringWhereOperand.instance("v"))));

    Assert.assertFalse(validatorFor("excludeSecret").isValid(nullEntitySecret, context));
  }

  public void testEmptyEntityFieldMatchesAnnotationDefault ()
    throws NoSuchFieldException {

    ConstraintValidatorContext context = Mockito.mock(ConstraintValidatorContext.class, Mockito.RETURNS_DEEP_STUBS);

    // A field carrying an explicit empty entity ("") is likewise canonicalized to null, so it matches the
    // annotation default just as the null-entity field does — both representations are equivalent.
    Where emptyEntitySecret = Where.instance(new AndWhereConjunction(WhereField.instance("", "secret", WhereOperator.EQ, StringWhereOperand.instance("v"))));

    Assert.assertFalse(validatorFor("excludeSecret").isValid(emptyEntitySecret, context));
  }
}
