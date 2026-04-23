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
package org.smallmind.web.jersey.aop;

import java.util.Arrays;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;

/**
 * Validation exception that converts a set of constraint violations into a human-readable message string.
 */
public class EntityValidationException extends jakarta.validation.ValidationException {

  /**
   * Constructs the exception by formatting all violations in the supplied set.
   *
   * @param constraintViolationSet the violations found during validation
   * @param <T>                    type of the validated object
   */
  public <T> EntityValidationException (Set<ConstraintViolation<T>> constraintViolationSet) {

    super(convert(constraintViolationSet));
  }

  /**
   * Converts the violation set into a formatted string listing each property path and message.
   *
   * @param constraintViolationSet violations to format
   * @param <T>                    type of the validated object
   * @return formatted string representation of all violations
   */
  private static <T> String convert (Set<ConstraintViolation<T>> constraintViolationSet) {

    Violation[] violations = new Violation[constraintViolationSet.size()];
    int index = 0;

    for (ConstraintViolation<?> constraintViolation : constraintViolationSet) {
      violations[index++] = new Violation(constraintViolation);
    }

    return Arrays.toString(violations);
  }

  private static class Violation {

    private final Path propertyPath;
    private final String message;

    /**
     * Captures the property path and message from a single constraint violation.
     *
     * @param constraintViolation the violation to record
     */
    public Violation (ConstraintViolation<?> constraintViolation) {

      propertyPath = constraintViolation.getPropertyPath();
      message = constraintViolation.getMessage();
    }

    /**
     * Returns a formatted representation of the violation including property path and message.
     *
     * @return violation description
     */
    @Override
    public String toString () {

      return new StringBuilder("{property_path = ").append(propertyPath).append(", message = ").append(message).append('}').toString();
    }
  }
}
