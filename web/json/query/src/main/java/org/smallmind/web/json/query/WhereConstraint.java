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
package org.smallmind.web.json.query;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Bean Validation constraint that enforces allowed, required, excluded, and dependency rules on a {@link WherePermissible} value.
 */
@Documented
@Retention(RUNTIME)
@Target({FIELD, PARAMETER, METHOD, LOCAL_VARIABLE})
@Constraint(validatedBy = WhereValidator.class)
public @interface WhereConstraint {

  /**
   * Container annotation enabling repeatable {@link WhereConstraint} declarations on the same element.
   */
  @Target({FIELD, PARAMETER, METHOD, LOCAL_VARIABLE})
  @Retention(RUNTIME)
  @Documented
  @interface List {

    /**
     * @return the repeated constraint annotations
     */
    WhereConstraint[] value ();
  }

  /**
   * @return constraint violation message template
   */
  String message () default "Validation failed";

  /**
   * @return validation groups this constraint belongs to
   */
  Class<?>[] groups () default {};

  /**
   * @return payload for Bean Validation clients to attach metadata
   */
  Class<? extends Payload>[] payload () default {};

  /**
   * @return fields that are explicitly permitted in the clause
   */
  Allowed[] allow () default {};

  /**
   * @return fields that must be present in the clause
   */
  Required[] require () default {};

  /**
   * @return fields that must not appear in the clause
   */
  Excluded[] exclude () default {};

  /**
   * @return dependency rules between fields
   */
  Dependent[] dependencies () default {};
}
