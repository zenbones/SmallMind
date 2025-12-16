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
package org.smallmind.persistence;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.springframework.beans.FatalBeanException;

/**
 * Utility methods to assist {@link ManagedDao} implementations in discovering durable types.
 */
public class ManagedDaoSupport {

  /**
   * Attempts to determine the {@link Durable} class managed by the supplied bean by
   * inspecting generics on {@code getManagedClass()} and superclasses.
   *
   * @param beanClass the DAO bean class to inspect
   * @return the durable class if one can be inferred, otherwise {@code null}
   * @throws FatalBeanException if the expected {@code getManagedClass()} method is missing
   */
  public static Class<?> findDurableClass (Class<?> beanClass) {

    Class<?> currentClass = beanClass;
    Type superType;
    Type returnType;

    try {
      if ((returnType = ((ParameterizedType)beanClass.getMethod("getManagedClass").getGenericReturnType()).getActualTypeArguments()[0]) instanceof Class) {

        return (Class<?>)returnType;
      }
    } catch (NoSuchMethodException noSuchMethodException) {
      throw new FatalBeanException("HibernateDao classes are expected to contain the method getManagedClass()", noSuchMethodException);
    }

    do {
      if (((superType = currentClass.getGenericSuperclass()) != null) && (superType instanceof ParameterizedType)) {
        for (Type genericType : ((ParameterizedType)superType).getActualTypeArguments()) {
          if ((genericType instanceof Class) && Durable.class.isAssignableFrom((Class<?>)genericType)) {

            return (Class<?>)genericType;
          }
        }
      }
    } while ((currentClass = currentClass.getSuperclass()) != null);

    return null;
  }
}
