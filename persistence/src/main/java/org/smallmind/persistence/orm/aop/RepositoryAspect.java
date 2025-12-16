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
package org.smallmind.persistence.orm.aop;

import java.lang.reflect.InvocationTargetException;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.FieldUtility;
import org.smallmind.persistence.ManagedDao;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.OrmDaoManager;

/**
 * Injects registered ORM DAOs into fields annotated with {@link Repository} on service classes.
 */
@Aspect
public class RepositoryAspect {

  /**
   * After a service is constructed, scans its fields for @Repository and injects the matching ORMDao from the registry.
   *
   * @param constructed the newly constructed service instance
   * @throws IllegalAccessException    if a field cannot be set
   * @throws InvocationTargetException if reflection fails while accessing fields
   */
  @AfterReturning(value = "@within(org.smallmind.nutsnbolts.inject.Service) && initialization(new(..)) && this(constructed)", argNames = "constructed")
  public void afterInitializationOfService (Object constructed)
    throws IllegalAccessException, InvocationTargetException {

    for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(constructed.getClass())) {

      Repository repository;

      if ((repository = fieldAccessor.getField().getAnnotation(Repository.class)) != null) {

        ORMDao ormDao;

        if ((ormDao = OrmDaoManager.get(repository.value())) == null) {
          throw new RepositoryError("The type(%s) of the @%s for field(%s) has no registered %s", repository.value().getName(), Repository.class.getSimpleName(), fieldAccessor.getField().getName(), ManagedDao.class.getSimpleName());
        } else if (!ormDao.getClass().isAssignableFrom(fieldAccessor.getField().getType())) {
          throw new RepositoryError("The type(%s) of field(%s) does not match that of its @%s(%s)", fieldAccessor.getField().getType().getName(), fieldAccessor.getField().getName(), Repository.class.getSimpleName(), ormDao.getClass().getName());
        } else {
          fieldAccessor.set(constructed, ormDao);
        }
      }
    }
  }
}
