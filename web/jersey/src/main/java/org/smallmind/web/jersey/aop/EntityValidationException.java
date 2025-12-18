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
 * Validation exception that aggregates constraint violations into a readable message for JsonEntity-backed resources.
 */
public class EntityValidationException extends jakarta.validation.ValidationException {

  /**
   * Builds an exception describing all constraint violations.
   *
   * @param constraintViolationSet violations collected during validation
   */
  public <T> EntityValidationException (Set<ConstraintViolation<T>> constraintViolationSet) {

    super(convert(constraintViolationSet));
  }

  /**
   * Formats constraint violations into a concise string array representation.
   *
   * @param constraintViolationSet violations to format
   * @return formatted message text
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
     * Captures key details from a constraint violation for rendering.
     *
     * @param constraintViolation the violation encountered
     */
    public Violation (ConstraintViolation<?> constraintViolation) {

      propertyPath = constraintViolation.getPropertyPath();
      message = constraintViolation.getMessage();
    }

    /**
     * Returns a simple representation containing property path and message.
     *
     * @return formatted violation text
     */
    @Override
    public String toString () {

      return new StringBuilder("{property_path = ").append(propertyPath).append(", message = ").append(message).append('}').toString();
    }
  }
}
