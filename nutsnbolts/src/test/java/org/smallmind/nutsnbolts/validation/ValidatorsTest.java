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
package org.smallmind.nutsnbolts.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ValidatorsTest {

  public void testEmailValidatorAcceptsValidAddress () {

    EmailValidator validator = new EmailValidator();
    validator.initialize(emailWith('\0'));

    Assert.assertTrue(validator.isValid("user@example.com", null));
    Assert.assertTrue(validator.isValid("first.last+tag@sub.example.co", null));
  }

  public void testEmailValidatorRejectsInvalidAddress () {

    EmailValidator validator = new EmailValidator();
    validator.initialize(emailWith('\0'));

    Assert.assertFalse(validator.isValid("no-at-sign", null));
    Assert.assertFalse(validator.isValid("missing@tld", null));
  }

  public void testEmailValidatorAcceptsNullValue () {

    EmailValidator validator = new EmailValidator();
    validator.initialize(emailWith('\0'));

    Assert.assertTrue(validator.isValid(null, null));
  }

  public void testEmailValidatorWithSeparatorChecksEveryPart () {

    EmailValidator validator = new EmailValidator();
    validator.initialize(emailWith(','));

    Assert.assertTrue(validator.isValid("a@example.com, b@example.com", null));
    Assert.assertFalse(validator.isValid("a@example.com, invalid", null));
  }

  public void testNotBlankValidator () {

    NotBlankValidator validator = new NotBlankValidator();
    validator.initialize(null);

    Assert.assertTrue(validator.isValid(null, null));
    Assert.assertTrue(validator.isValid("non blank", null));
    Assert.assertFalse(validator.isValid("", null));
    Assert.assertFalse(validator.isValid("   \t\n", null));
  }

  public void testNotEmptyValidatorOnCollectionMapArray () {

    NotEmptyValidator validator = new NotEmptyValidator();
    validator.initialize(null);

    Assert.assertTrue(validator.isValid(null, null));
    Assert.assertTrue(validator.isValid(List.of(1), null));
    Assert.assertFalse(validator.isValid(List.of(), null));
    Assert.assertTrue(validator.isValid(Map.of("k", "v"), null));
    Assert.assertFalse(validator.isValid(Map.of(), null));
    Assert.assertTrue(validator.isValid(new int[] {1}, null));
    Assert.assertFalse(validator.isValid(new int[] {}, null));
    Assert.assertFalse(validator.isValid("not-a-collection", null));
  }

  public void testGreaterThanValidator () {

    GreaterThanValidator validator = new GreaterThanValidator();
    validator.initialize(greaterThan(5));

    Assert.assertTrue(validator.isValid(null, null));
    Assert.assertTrue(validator.isValid(6, null));
    Assert.assertFalse(validator.isValid(5, null));
    Assert.assertTrue(validator.isValid(new BigDecimal("5.0001"), null));
    Assert.assertFalse(validator.isValid(new BigDecimal("5.0000"), null));
    Assert.assertTrue(validator.isValid(BigInteger.valueOf(10), null));
    Assert.assertFalse(validator.isValid(BigInteger.valueOf(5), null));
  }

  public void testNotZeroValidator () {

    NotZeroValidator validator = new NotZeroValidator();
    validator.initialize(null);

    Assert.assertTrue(validator.isValid(null, null));
    Assert.assertTrue(validator.isValid(1, null));
    Assert.assertFalse(validator.isValid(0, null));
    Assert.assertFalse(validator.isValid(BigDecimal.ZERO, null));
    Assert.assertTrue(validator.isValid(BigDecimal.ONE, null));
    Assert.assertFalse(validator.isValid(BigInteger.ZERO, null));
    Assert.assertTrue(validator.isValid(BigInteger.ONE, null));
  }

  public void testSanitizedValidator () {

    SanitizedValidator validator = new SanitizedValidator();

    Assert.assertTrue(validator.isValid(null, null));
    Assert.assertFalse(validator.isValid("", null));
    Assert.assertTrue(validator.isValid("plain-name", null));
    Assert.assertFalse(validator.isValid("a;drop;table", null));
  }

  public void testLowerBoundValidatorSatisfiedWhenFirstAtLeastSecond () {

    LowerBoundValidator validator = new LowerBoundValidator();
    validator.initialize(lowerBound("first", "second", 0, false));

    Assert.assertTrue(validator.isValid(null, null));
    Assert.assertTrue(validator.isValid(new BoundedBean(10, 5), null));
    Assert.assertTrue(validator.isValid(new BoundedBean(5, 5), null));
    Assert.assertFalse(validator.isValid(new BoundedBean(4, 5), null));
  }

  public void testLowerBoundValidatorWithStrictlyGreaterRequirement () {

    LowerBoundValidator validator = new LowerBoundValidator();
    validator.initialize(lowerBound("first", "second", 1, false));

    Assert.assertTrue(validator.isValid(new BoundedBean(6, 5), null));
    Assert.assertTrue(validator.isValid(new BoundedBean(new BigDecimal("5.0001"), 5), null));
    Assert.assertFalse(validator.isValid(new BoundedBean(5, 5), null));
    Assert.assertFalse(validator.isValid(new BoundedBean(4, 5), null));
  }

  public void testLowerBoundValidatorHonoursNotNull () {

    LowerBoundValidator strict = new LowerBoundValidator();
    strict.initialize(lowerBound("first", "second", 0, true));

    Assert.assertFalse(strict.isValid(new BoundedBean(null, 5), null));
    Assert.assertFalse(strict.isValid(new BoundedBean(10, null), null));

    LowerBoundValidator lenient = new LowerBoundValidator();
    lenient.initialize(lowerBound("first", "second", 0, false));

    Assert.assertTrue(lenient.isValid(new BoundedBean(null, 5), null));
    Assert.assertTrue(lenient.isValid(new BoundedBean(10, null), null));
  }

  public void testFormattedValidationExceptionFormatsMessage () {

    FormattedValidationException exception = new FormattedValidationException("Bad id(%s)", "x-1");

    Assert.assertEquals(exception.getMessage(), "Bad id(x-1)");
  }

  private static Email emailWith (char separator) {

    Map<String, Object> overrides = new HashMap<>();

    overrides.put("separator", separator);

    return proxyAnnotation(Email.class, overrides);
  }

  private static GreaterThan greaterThan (long value) {

    Map<String, Object> overrides = new HashMap<>();

    overrides.put("value", value);

    return proxyAnnotation(GreaterThan.class, overrides);
  }

  private static LowerBound lowerBound (String first, String second, int value, boolean notNull) {

    Map<String, Object> overrides = new HashMap<>();

    overrides.put("first", first);
    overrides.put("second", second);
    overrides.put("value", value);
    overrides.put("notNull", notNull);

    return proxyAnnotation(LowerBound.class, overrides);
  }

  @SuppressWarnings("unchecked")
  private static <A extends Annotation> A proxyAnnotation (Class<A> annotationType, Map<String, Object> overrides) {

    return (A)Proxy.newProxyInstance(
      annotationType.getClassLoader(),
      new Class<?>[] {annotationType},
      (proxy, method, args) -> {
        if ("annotationType".equals(method.getName()) && (method.getParameterCount() == 0)) {
          return annotationType;
        } else if (overrides.containsKey(method.getName())) {
          return overrides.get(method.getName());
        } else {
          return method.getDefaultValue();
        }
      }
    );
  }

  public static class BoundedBean {

    private final Number first;
    private final Number second;

    public BoundedBean (Number first, Number second) {

      this.first = first;
      this.second = second;
    }

    public Number getFirst () {

      return first;
    }

    public Number getSecond () {

      return second;
    }
  }
}
