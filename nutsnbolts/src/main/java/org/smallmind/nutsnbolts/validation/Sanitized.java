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
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;

/**
 * Bean Validation constraint that requires an annotated string to be non-empty and free of illegal characters.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, PARAMETER, METHOD, RECORD_COMPONENT})
@Constraint(validatedBy = SanitizedValidator.class)
public @interface Sanitized {

  /**
   * Container annotation that allows multiple {@link Sanitized} constraints on the same element.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target({FIELD, PARAMETER, METHOD, RECORD_COMPONENT})
  @Documented
  @interface List {

    /**
     * @return the contained {@link Sanitized} constraints
     */
    Sanitized[] value ();
  }

  /**
   * @return the constraint violation message
   */
  String message () default "Must not be empty or contain illegal characters";

  /**
   * @return the validation groups to which this constraint belongs
   */
  Class<?>[] groups () default {};

  /**
   * @return the payload types associated with this constraint
   */
  Class<? extends Payload>[] payload () default {};
}
