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

import java.math.BigDecimal;
import java.math.BigInteger;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link jakarta.validation.ConstraintValidator} for the {@link GreaterThan} constraint that checks numeric values against a configured threshold.
 */
public class GreaterThanValidator implements ConstraintValidator<GreaterThan, Number> {

  private GreaterThan constraintAnnotation;

  /**
   * Stores the constraint annotation so that the configured threshold is available during validation.
   *
   * @param constraintAnnotation the annotation instance driving this validation
   */
  @Override
  public void initialize (GreaterThan constraintAnnotation) {

    this.constraintAnnotation = constraintAnnotation;
  }

  /**
   * Returns {@code true} if the value strictly exceeds the configured threshold.
   * A {@code null} value is considered valid.
   *
   * @param value   the candidate number to validate
   * @param context the constraint validator context (unused)
   * @return {@code true} if the value is {@code null} or greater than the threshold
   */
  @Override
  public boolean isValid (Number value, ConstraintValidatorContext context) {

    return switch (value) {
      case null -> true;
      case BigDecimal bigDecimal -> bigDecimal.compareTo(BigDecimal.valueOf(constraintAnnotation.value())) > 0;
      case BigInteger bigInteger -> bigInteger.compareTo(BigInteger.valueOf(constraintAnnotation.value())) > 0;
      default -> value.doubleValue() > constraintAnnotation.value();
    };
  }
}
