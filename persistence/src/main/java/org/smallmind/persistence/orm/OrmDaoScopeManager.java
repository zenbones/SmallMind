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
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.persistence.Durable;

/**
 * Application-scoped registry of {@link ORMDao} instances keyed by {@link Durable} class, resolved through a
 * {@link PerApplicationScope}. This is the scope flavor of {@link OrmDaoContextManager}: the calling thread must be
 * inside the scope's {@code run} / {@code call}, an {@code ApplicationCarrier}, or a thread created by
 * {@code PerApplicationScope.wrapThreadFactory} or the scope's own {@code boundThreadFactory} / {@code boundExecutor},
 * otherwise {@code MissingPerApplicationScopeException} is thrown.
 * Library code in this module ({@code ORMDao.register()}, the repository aspect, and the cache vectors) resolves
 * through {@link OrmDaoManager}, which writes here whenever a scope is bound on the calling thread and reads here first.
 */
public class OrmDaoScopeManager implements PerApplicationDataManager {

  /**
   * Registers an {@link ORMDao} for the given durable class in the per-application scope bound to the current thread.
   *
   * @param durableClass the durable type the DAO manages
   * @param ormDao       the DAO to register
   */
  public static void register (Class<? extends Durable> durableClass, ORMDao ormDao) {

    ConcurrentHashMap<Class<? extends Durable>, ORMDao> ormDaoMap;

    if ((ormDaoMap = getOrmDaoMap()) == null) {
      ormDaoMap = PerApplicationScope.setPerApplicationDataIfAbsent(OrmDaoScopeManager.class, new ConcurrentHashMap<>());
    }
    ormDaoMap.put(durableClass, ormDao);
  }

  /**
   * Finds a registered durable class by simple or fully qualified name.
   *
   * @param name the simple name (no {@code '.'}) or fully qualified name of the durable class
   * @return the matching registered class, or {@code null} if none matches
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
   * Resolves the {@link ORMDao} registered for the durable class with the given name.
   *
   * @param name the simple or fully qualified name of the durable class
   * @return the registered DAO, or {@code null} if no durable class matches
   */
  public static ORMDao get (String name) {

    Class<? extends Durable> durableClass;

    if ((durableClass = findDurableClass(name)) != null) {

      return getOrmDaoMap().get(durableClass);
    }

    return null;
  }

  /**
   * Resolves the {@link ORMDao} registered for the given durable class.
   *
   * @param durableClass the durable type
   * @param <I>          the durable identifier type
   * @param <D>          the durable type
   * @return the registered DAO, or {@code null} if none is registered
   */
  public static <I extends Serializable & Comparable<I>, D extends Durable<I>> ORMDao<I, D, ?, ?> get (Class<D> durableClass) {

    ConcurrentHashMap<Class<? extends Durable>, ORMDao> ormDaoMap;

    return ((ormDaoMap = getOrmDaoMap()) == null) ? null : ormDaoMap.get(durableClass);
  }

  private static ConcurrentHashMap<Class<? extends Durable>, ORMDao> getOrmDaoMap () {

    return PerApplicationScope.getPerApplicationData(OrmDaoScopeManager.class, ConcurrentHashMap.class);
  }
}
