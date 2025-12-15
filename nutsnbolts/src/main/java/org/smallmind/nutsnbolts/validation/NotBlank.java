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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Bean Validation constraint requiring a string to contain at least one non-whitespace character.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, PARAMETER, METHOD, LOCAL_VARIABLE})
@Constraint(validatedBy = NotBlankValidator.class)
public @interface NotBlank {

  /**
   * Allows multiple {@link NotBlank} annotations on the same element.
   */
  @Target({FIELD, PARAMETER, METHOD, LOCAL_VARIABLE})
  @Retention(RUNTIME)
  @Documented
  @interface List {

    /**
     * @return array of {@link NotBlank} constraints
     */
    NotBlank[] value ();
  }

  /**
   * @return message used when the value is blank
   */
  String message () default "must be not be blank";

  /**
   * @return validation groups this constraint belongs to
   */
  Class<?>[] groups () default {};

  /**
   * @return custom payloads for Bean Validation clients
   */
  Class<? extends Payload>[] payload () default {};
}
