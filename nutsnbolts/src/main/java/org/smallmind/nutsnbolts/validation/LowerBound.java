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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class-level constraint ensuring one numeric field is greater than or equal to another, optionally offset.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, ANNOTATION_TYPE})
@Constraint(validatedBy = LowerBoundValidator.class)
public @interface LowerBound {

  /**
   * Allows multiple {@link LowerBound} constraints on the same type.
   */
  @Target({TYPE, ANNOTATION_TYPE})
  @Retention(RUNTIME)
  @Documented
  @interface List {

    /**
     * @return array of {@link LowerBound} constraints
     */
    LowerBound[] value ();
  }

  /**
   * @return validation message template
   */
  String message () default "The '{first}' field must be >= '{second}' field offset by {value}";

  /**
   * @return validation groups this constraint belongs to
   */
  Class<?>[] groups () default {};

  /**
   * @return custom payloads for Bean Validation clients
   */
  Class<? extends Payload>[] payload () default {};

  /**
   * @return name of the field that must be greater than or equal to {@link #second()} plus {@link #value()}
   */
  String first ();

  /**
   * @return name of the reference field to compare against
   */
  String second ();

  /**
   * @return whether both fields must be non-null to be considered valid
   */
  boolean notNull () default false;

  /**
   * @return offset added to {@link #second()} when performing the comparison
   */
  int value () default 0;
}
