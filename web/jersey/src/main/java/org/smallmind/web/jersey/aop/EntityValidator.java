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

import java.lang.reflect.Method;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.executable.ExecutableValidator;
import org.hibernate.validator.HibernateValidator;

/**
 * Provides executable validation utilities that integrate with {@link EntityParameterNameProvider}.
 */
public class EntityValidator {

  private static final ExecutableValidator EXECUTABLE_VALIDATOR;

  static {

    EXECUTABLE_VALIDATOR = Validation.byProvider(HibernateValidator.class).configure().parameterNameProvider(new EntityParameterNameProvider()).buildValidatorFactory().getValidator().forExecutables();
  }

  /**
   * Validates the supplied method parameters and throws {@link EntityValidationException} on violations.
   *
   * @param object the object whose method is being invoked
   * @param method the method under validation
   * @param parameters invocation arguments
   * @param <T> type of the target object
   * @throws EntityValidationException when constraint violations are present
   */
  public static <T> void validateParameters (T object, Method method, Object[] parameters) {

    Set<ConstraintViolation<T>> constraintViolationSet;

    if (!(constraintViolationSet = EXECUTABLE_VALIDATOR.validateParameters(object, method, parameters)).isEmpty()) {
      throw new EntityValidationException(constraintViolationSet);
    }
  }

  /**
   * Validates the return value of a method call.
   *
   * @param object the object whose method was invoked
   * @param method the executed method
   * @param returnValue value returned by the method
   * @param <T> type of the target object
   * @throws EntityValidationException when constraint violations are present
   */
  public static <T> void validateReturnValue (T object, Method method, Object returnValue) {

    Set<ConstraintViolation<T>> constraintViolationSet;

    if (!(constraintViolationSet = EXECUTABLE_VALIDATOR.validateReturnValue(object, method, returnValue)).isEmpty()) {
      throw new EntityValidationException(constraintViolationSet);
    }
  }
}
