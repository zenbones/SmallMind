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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import jakarta.validation.ParameterNameProvider;

/**
 * Parameter name provider that prefers {@link EntityParam} annotation values when present,
 * falling back to indexed argument names for validation messages.
 */
public class EntityParameterNameProvider implements ParameterNameProvider {

  /**
   * Derives constructor parameter names based on {@link EntityParam} annotations.
   *
   * @param constructor the constructor being validated
   * @return ordered list of parameter names
   */
  @Override
  public List<String> getParameterNames (Constructor<?> constructor) {

    return getNames(constructor.getParameterAnnotations());
  }

  /**
   * Derives method parameter names based on {@link EntityParam} annotations.
   *
   * @param method the method being validated
   * @return ordered list of parameter names
   */
  @Override
  public List<String> getParameterNames (Method method) {

    return getNames(method.getParameterAnnotations());
  }

  /**
   * Resolves names from a two-dimensional annotation array, preferring {@link EntityParam} values when present.
   *
   * @param parameterAnnotationsArray annotations for each parameter position
   * @return list of resolved parameter names
   */
  private List<String> getNames (Annotation[][] parameterAnnotationsArray) {

    LinkedList<String> nameList = new LinkedList<>();

    for (int index = 0; index < parameterAnnotationsArray.length; index++) {

      EntityParam entityParam = null;

      for (Annotation parameterAnnotation : parameterAnnotationsArray[index]) {
        if (parameterAnnotation instanceof EntityParam) {
          entityParam = (EntityParam)parameterAnnotation;
          break;
        }
      }

      nameList.add((entityParam == null) ? "argument[" + index + "]" : entityParam.value());
    }

    return nameList;
  }
}
