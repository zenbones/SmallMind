/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.lang.StaticManager;
import org.smallmind.persistence.Durable;

public class DaoManager implements StaticManager {

   private static final InheritableThreadLocal<Map<Class<? extends Durable>, ORMDao>> DAO_MAP_LOCAL = new InheritableThreadLocal<Map<Class<? extends Durable>, ORMDao>>() {

      @Override
      protected Map<Class<? extends Durable>, ORMDao> initialValue () {

         return new ConcurrentHashMap<Class<? extends Durable>, ORMDao>();
      }
   };

   public static void register (Class<? extends Durable> durableClass, ORMDao ormDao) {

      DAO_MAP_LOCAL.get().put(durableClass, ormDao);
   }

   public static Class<? extends Durable> findDurableClass (String name) {

      boolean isSimple = name.indexOf('.') < 0;

      for (Class<? extends Durable> durableClass : DAO_MAP_LOCAL.get().keySet()) {
         if ((isSimple) ? durableClass.getSimpleName().equals(name) : durableClass.getName().equals(name)) {

            return durableClass;
         }
      }

      return null;
   }

   public static ORMDao get (String name) {

      Class<? extends Durable> durableClass;

      if ((durableClass = findDurableClass(name)) != null) {

         return DAO_MAP_LOCAL.get().get(durableClass);
      }

      return null;
   }

   public static <I extends Serializable & Comparable<I>, D extends Durable<I>> ORMDao<I, D> get (Class<D> durableClass) {

      return DAO_MAP_LOCAL.get().get(durableClass);
   }
}