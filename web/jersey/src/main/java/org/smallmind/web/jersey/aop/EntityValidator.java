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

import java.lang.reflect.Method;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.executable.ExecutableValidator;
import org.hibernate.validator.HibernateValidator;

public class EntityValidator {

  private static final ExecutableValidator EXECUTABLE_VALIDATOR;

  static {

    EXECUTABLE_VALIDATOR = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory().getValidator().forExecutables();
  }

  public static <T> void validateParameters (T object, Method method, Object[] parameters) {

    Set<ConstraintViolation<T>> constraintViolationSet;

    if (!(constraintViolationSet = EXECUTABLE_VALIDATOR.validateParameters(object, method, parameters)).isEmpty()) {
      throw new ValidationException(constraintViolationSet);
    }
  }

  public static <T> void validateReturnValue (T object, Method method, Object returnValue) {

    Set<ConstraintViolation<T>> constraintViolationSet;

    if (!(constraintViolationSet = EXECUTABLE_VALIDATOR.validateReturnValue(object, method, returnValue)).isEmpty()) {
      throw new ValidationException(constraintViolationSet);
    }
  }
}
