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
package org.smallmind.persistence.orm;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.persistence.Durable;

/**
 * Application-wide registry for {@link ORMDao} instances keyed by their durable classes.
 * Provides lookup by class or class name to help wire DAOs in environments that cannot
 * rely on injection alone.
 */
public class OrmDaoContextManager implements PerApplicationDataManager {

  /**
   * Registers an ORM DAO for a durable class in the current application context.
   *
   * @param durableClass the durable class the DAO manages
   * @param ormDao       the DAO instance to register
   */
  public static void register (Class<? extends Durable> durableClass, ORMDao ormDao) {

    ConcurrentHashMap<Class<? extends Durable>, ORMDao> ormDaoMap;

    if ((ormDaoMap = getOrmDaoMap()) == null) {
      ormDaoMap = PerApplicationContext.setPerApplicationDataIfAbsent(OrmDaoContextManager.class, new ConcurrentHashMap<>());
    }
    ormDaoMap.put(durableClass, ormDao);
  }

  /**
   * Resolves a durable class by its simple or fully qualified name among registered DAOs.
   *
   * @param name simple or fully qualified class name
   * @return the matching durable class, or {@code null} if none match
   */
  public static Class<? extends Durable> findDurableClass (String name) {

    ConcurrentHashMap<Class<? extends Durable>, ORMDao> ormDaoMap;

    if ((ormDaoMap = getOrmDaoMap()) != null) {

      boolean isSimple = name.indexOf('.') < 0;

      for (Class<? extends Durable> durableClass : ormDaoMap.keySet()) {
        if ((isSimple) ? durableClass.getSimpleName().equals(name) : durableClass.getName().equals(name)) {

          return durableClass;
        }
      }
    }

    return null;
  }

  /**
   * Retrieves a registered DAO by the name of its durable class.
   *
   * @param name simple or fully qualified class name
   * @return the DAO instance, or {@code null} if none is registered
   */
  public static ORMDao get (String name) {

    Class<? extends Durable> durableClass;

    if ((durableClass = findDurableClass(name)) != null) {

      return getOrmDaoMap().get(durableClass);
    }

    return null;
  }

  /**
   * Retrieves a registered DAO by durable class.
   *
   * @param durableClass the class managed by the DAO
   * @param <I>          identifier type
   * @param <D>          durable type
   * @return the DAO instance, or {@code null} if none is registered
   */
  public static <I extends Serializable & Comparable<I>, D extends Durable<I>> ORMDao<I, D, ?, ?> get (Class<D> durableClass) {

    ConcurrentHashMap<Class<? extends Durable>, ORMDao> ormDaoMap;

    return ((ormDaoMap = getOrmDaoMap()) == null) ? null : ormDaoMap.get(durableClass);
  }

  private static ConcurrentHashMap<Class<? extends Durable>, ORMDao> getOrmDaoMap () {

    return PerApplicationContext.getPerApplicationData(OrmDaoContextManager.class, ConcurrentHashMap.class);
  }
}
