/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.web.json.scaffold.dto;

import java.lang.reflect.Method;
import java.util.HashMap;
import org.smallmind.nutsnbolts.util.Mutation;
import org.smallmind.nutsnbolts.util.MutationUtility;

public class ArrayMutator {

  public static <T, U> U[] toEntityType (Class<U> entityClass, T[] dtoArray)
    throws DtoPropertyException {

    if (dtoArray == null) {

      return null;
    } else {
      try {

        HashMap<Class<?>, Method> factoryMethodMap = new HashMap<>();

        return MutationUtility.toArray(dtoArray, new Mutation<T, U>() {

          @Override
          public Class<U> getMutatedClass () {

            return entityClass;
          }

          @Override
          public U mutate (T dto)
            throws Exception {

            Method factoryMethod;

            if ((factoryMethod = factoryMethodMap.get(dto.getClass())) == null) {
              factoryMethodMap.put(dto.getClass(), factoryMethod = dto.getClass().getMethod("factory"));
            }

            return (U)factoryMethod.invoke(dto);
          }
        });
      } catch (Exception exception) {
        throw new DtoPropertyException(exception);
      }
    }
  }

  public static <T, U> U[] toDtoType (Class<? extends T> entityClass, Class<U> dtoClass, T[] entityArray)
    throws DtoPropertyException {

    if (entityArray == null) {

      return null;
    } else {
      try {

        Method instanceMethod = dtoClass.getMethod("instance", entityClass);

        return MutationUtility.toArray(entityArray, new Mutation<T, U>() {

          @Override
          public Class<U> getMutatedClass () {

            return dtoClass;
          }

          @Override
          public U mutate (T entity)
            throws Exception {

            return (U)instanceMethod.invoke(null, entity);
          }
        });
      } catch (Exception exception) {
        throw new DtoPropertyException(exception);
      }
    }
  }
}
