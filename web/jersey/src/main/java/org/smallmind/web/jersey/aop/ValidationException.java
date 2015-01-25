/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import javax.validation.ConstraintViolation;
import javax.validation.Path;

public class ValidationException extends RuntimeException {

  public <T> ValidationException (Set<ConstraintViolation<T>> constraintViolationSet) {

    super(convert(constraintViolationSet));
  }

  private static <T> String convert (Set<ConstraintViolation<T>> constraintViolationSet) {

    Violation[] violations = new Violation[constraintViolationSet.size()];
    int index = 0;

    for (ConstraintViolation<?> constraintViolation : constraintViolationSet) {
      violations[index++] = new Violation(constraintViolation);
    }

    return Arrays.toString(violations);
  }

  private static class Violation {

    private Path propertyPath;
    private String message;

    public Violation (ConstraintViolation<?> constraintViolation) {

      propertyPath = constraintViolation.getPropertyPath();
      message = constraintViolation.getMessage();
    }

    @Override
    public String toString () {

      return new StringBuilder("[propertyPath = ").append(propertyPath).append(", meassge = ").append(message).append(']').toString();
    }
  }
}
