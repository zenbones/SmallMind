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
 * Validator backing the {@link NotZero} constraint for numeric values.
 */
public class NotZeroValidator implements ConstraintValidator<NotZero, Number> {

  /**
   * No-op initializer; present for interface compliance.
   */
  @Override
  public void initialize (NotZero constraintAnnotation) {

  }

  /**
   * Checks that the number is non-zero; {@code null} is treated as valid.
   *
   * @param value   candidate number
   * @param context validation context (unused)
   * @return {@code true} when the value is null or non-zero
   */
  @Override
  public boolean isValid (Number value, ConstraintValidatorContext context) {

    if (value == null) {
      return true;
    } else if (value instanceof BigDecimal) {
      return ((BigDecimal)value).compareTo(BigDecimal.valueOf(0)) != 0;
    } else if (value instanceof BigInteger) {
      return ((BigInteger)value).compareTo(BigInteger.valueOf(0)) != 0;
    } else {
      return value.doubleValue() != 0D;
    }
  }
}
