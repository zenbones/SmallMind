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
package org.smallmind.nutsnbolts.inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.FieldUtility;

/**
 * Aspect that implements {@link LazyField} by invoking the annotated method once and storing the result.
 */
@Aspect
public class LazyFieldAspect {

  /**
   * Around advice that populates the named field if currently null, otherwise returns the cached value.
   *
   * @param thisJoinPoint proceeding join point representing the method call
   * @param lazyField     annotation describing the target field
   * @param called        instance containing the field
   * @return existing or newly computed field value
   * @throws Throwable if the underlying method or reflection operations fail
   */
  @Around(value = "execution(@org.smallmind.nutsnbolts.inject.LazyField * * (..)) && @annotation(lazyField) && this(called)", argNames = "thisJoinPoint, lazyField, called")
  public Object aroundLazyMethod (ProceedingJoinPoint thisJoinPoint, LazyField lazyField, Object called)
    throws Throwable {

    FieldAccessor fieldAccessor;

    if ((fieldAccessor = FieldUtility.getFieldAccessor(called.getClass(), lazyField.value())) == null) {
      throw new LazyError("Missing field(%s) in type(%s) with @%s annotated method(%s)", lazyField.value(), called.getClass().getName(), LazyField.class.getSimpleName(), thisJoinPoint.getSignature().getName());
    } else {

      Object fieldValue;

      if ((fieldValue = fieldAccessor.get(called)) == null) {
        fieldAccessor.set(called, fieldValue = thisJoinPoint.proceed());
      }

      return fieldValue;
    }
  }
}
