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
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.persistence.Durable;

/**
 * Registry of {@link ORMDao} instances that bridges {@link OrmDaoScopeManager} and {@link OrmDaoContextManager}.
 * <p>
 * {@code register} writes to every per-application registry bound on the calling thread: the scope flavor when a
 * {@link PerApplicationScope} is bound, and the context flavor when a {@link PerApplicationContext} is attached, so a
 * registration made from a thread that has both is visible to threads that later carry only one of them. Lookups read
 * the scope flavor first and fall back to the context flavor when the scope holds nothing and a context is attached,
 * so a thread bound to a scope that was never written still finds what was registered through the context. A thread
 * with neither bound delegates to the context flavor and fails with {@code MissingPerApplicationContextException}.
 * Nothing is ever written to a registry whose binding is absent.
 */
public class OrmDaoManager {

  /**
   * Registers an {@link ORMDao} for the given durable class in every per-application registry bound on the current
   * thread.
   *
   * @param durableClass the durable type the DAO manages
   * @param ormDao       the DAO to register
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static void register (Class<? extends Durable> durableClass, ORMDao ormDao) {

    if (PerApplicationScope.isBound()) {
      OrmDaoScopeManager.register(durableClass, ormDao);
      if (PerApplicationContext.isBound()) {
        OrmDaoContextManager.register(durableClass, ormDao);
      }
    } else {
      OrmDaoContextManager.register(durableClass, ormDao);
    }
  }

  /**
   * Finds a registered durable class by simple or fully qualified name, scope flavor first, then context flavor.
   *
   * @param name the simple name (no {@code '.'}) or fully qualified name of the durable class
   * @return the matching registered class, or {@code null} if none matches in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static Class<? extends Durable> findDurableClass (String name) {

    if (PerApplicationScope.isBound()) {

      Class<? extends Durable> durableClass;

      if ((durableClass = OrmDaoScopeManager.findDurableClass(name)) != null) {

        return durableClass;
      }
      if (!PerApplicationContext.isBound()) {

        return null;
      }
    }

    return OrmDaoContextManager.findDurableClass(name);
  }

  /**
   * Resolves the {@link ORMDao} registered for the durable class with the given name, scope flavor first, then
   * context flavor.
   *
   * @param name the simple or fully qualified name of the durable class
   * @return the registered DAO, or {@code null} if no durable class matches in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static ORMDao get (String name) {

    if (PerApplicationScope.isBound()) {

      ORMDao ormDao;

      if ((ormDao = OrmDaoScopeManager.get(name)) != null) {

        return ormDao;
      }
      if (!PerApplicationContext.isBound()) {

        return null;
      }
    }

    return OrmDaoContextManager.get(name);
  }

  /**
   * Resolves the {@link ORMDao} registered for the given durable class, scope flavor first, then context flavor.
   *
   * @param durableClass the durable type
   * @param <I>          the durable identifier type
   * @param <D>          the durable type
   * @return the registered DAO, or {@code null} if none is registered in any bound registry
   * @throws org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException if neither a per-application scope nor a per-application context is bound on the calling thread
   */
  public static <I extends Serializable & Comparable<I>, D extends Durable<I>> ORMDao<I, D, ?, ?> get (Class<D> durableClass) {

    if (PerApplicationScope.isBound()) {

      ORMDao<I, D, ?, ?> ormDao;

      if ((ormDao = OrmDaoScopeManager.get(durableClass)) != null) {

        return ormDao;
      }
      if (!PerApplicationContext.isBound()) {

        return null;
      }
    }

    return OrmDaoContextManager.get(durableClass);
  }
}
