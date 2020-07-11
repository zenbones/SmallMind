/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.web.json.scaffold.property;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import org.smallmind.nutsnbolts.util.MutationUtility;

public class ListMutator {

  public static <T, U> List<U> toEntityType (List<? extends T> dtoList)
    throws PropertyException {

    if (dtoList == null) {

      return null;
    } else {

      HashMap<Class<?>, Method> factoryMethodMap = new HashMap<>();

      try {
        return MutationUtility.toList(dtoList, (dto) -> {

          Method factoryMethod;

          if ((factoryMethod = factoryMethodMap.get(dto.getClass())) == null) {
            factoryMethodMap.put(dto.getClass(), factoryMethod = dto.getClass().getMethod("factory"));
          }

          return (U)factoryMethod.invoke(dto);
        });
      } catch (Exception exception) {
        throw new PropertyException(exception);
      }
    }
  }

  public static <T, U> List<U> toDtoType (Class<? extends T> entityClass, Class<U> dtoClass, List<? extends T> entityList)
    throws PropertyException {

    if (entityList == null) {

      return null;
    } else {
      try {

        Method instanceMethod = dtoClass.getMethod("instance", entityClass);

        return MutationUtility.toList(entityList, (entity) -> (U)instanceMethod.invoke(null, entity));
      } catch (Exception exception) {
        throw new PropertyException(exception);
      }
    }
  }
}
