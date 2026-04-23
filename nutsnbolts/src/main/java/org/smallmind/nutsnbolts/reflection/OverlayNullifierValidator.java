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
package org.smallmind.nutsnbolts.reflection;

import java.lang.annotation.Annotation;

/**
 * Strategy interface for classes that decide whether a given field value should be treated as {@code null}
 * during overlay processing, parameterised over the annotation type that carries the configuration.
 *
 * @param <A> the {@link Annotation} type that activates this validator
 * @param <T> the field value type that this validator evaluates
 */
public interface OverlayNullifierValidator<A extends Annotation, T> {

  /**
   * Called once before the first evaluation to allow the validator to read configuration from the
   * annotation instance placed on the field.
   * The default implementation is a no-op.
   *
   * @param constraintAnnotation the annotation instance found on the field
   */
  default void initialize (A constraintAnnotation) {

  }

  /**
   * Evaluates whether the supplied value should be replaced by {@code null} in the target object.
   *
   * @param obj the non-null field value read from the overlay source
   * @return {@code true} if the value is a sentinel that the overlay system should treat as {@code null}
   */
  boolean equivalentToNull (T obj);
}
