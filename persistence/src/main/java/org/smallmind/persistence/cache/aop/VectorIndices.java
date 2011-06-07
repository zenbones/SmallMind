/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.cache.aop;

import org.aspectj.lang.JoinPoint;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.nutsnbolts.reflection.type.TypeUtility;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.aop.AOPUtility;
import org.smallmind.persistence.cache.VectorIndex;
import org.smallmind.persistence.orm.DaoManager;
import org.smallmind.persistence.orm.ORMDao;

public class VectorIndices {

  public static VectorIndex[] getVectorIndexes (Vector vector, Durable durable, ORMDao ormDao) {

    VectorIndex[] indices;

    indices = new VectorIndex[vector.value().length];
    for (int count = 0; count < vector.value().length; count++) {

      Comparable indexValue;

      if (vector.value()[count].constant()) {
        try {
          indexValue = (Comparable)BeanUtility.convertFromString(PersistenceManager.getPersistence().getStringConverterFactory(), (!vector.value()[count].type().equals(Nothing.class)) ? vector.value()[count].type() : getIdClass(vector.value()[count], ormDao), vector.value()[count].on());
        }
        catch (Exception exception) {
          throw new CacheAutomationError(exception);
        }
      }
      else {
        indexValue = getValue(durable, (!vector.value()[count].type().equals(Nothing.class)) ? vector.value()[count].type() : getIdClass(vector.value()[count], ormDao), vector.value()[count].on());
      }

      indices[count] = new VectorIndex(vector.value()[count].with(), vector.value()[count].on(), indexValue);
    }

    return indices;
  }

  public static VectorIndex[] getVectorIndexes (Vector vector, JoinPoint joinPoint, ORMDao ormDao) {

    VectorIndex[] indices;

    indices = new VectorIndex[vector.value().length];
    for (int count = 0; count < vector.value().length; count++) {

      Comparable indexValue;

      if (vector.value()[count].constant()) {
        try {
          indexValue = (Comparable)BeanUtility.convertFromString(PersistenceManager.getPersistence().getStringConverterFactory(), (!vector.value()[count].type().equals(Nothing.class)) ? vector.value()[count].type() : getIdClass(vector.value()[count], ormDao), vector.value()[count].on());
        }
        catch (Exception exception) {
          throw new CacheAutomationError(exception);
        }
      }
      else {
        indexValue = getValue(joinPoint, (!vector.value()[count].type().equals(Nothing.class)) ? vector.value()[count].type() : getIdClass(vector.value()[count], ormDao), vector.value()[count].on());
      }

      indices[count] = new VectorIndex(vector.value()[count].with(), vector.value()[count].on(), indexValue);
    }

    return indices;
  }

  private static Class getIdClass (Index index, ORMDao ormDao) {

    ORMDao indexingDao;

    if (Durable.class.equals(index.with())) {

      return ormDao.getIdClass();
    }

    if ((indexingDao = DaoManager.get(index.with())) == null) {
      throw new CacheAutomationError("Unable to locate an implementation of ORMDao within DaoManager for the requested index(%s)", index.with().getName());
    }

    return indexingDao.getIdClass();
  }

  public static Comparable getValue (JoinPoint joinPoint, Class parameterType, String parameterName) {

    try {
      return (Comparable)AOPUtility.getParameterValue(joinPoint, parameterType, parameterName);
    }
    catch (Exception exception) {
      throw new CacheAutomationError(exception);
    }
  }

  public static Comparable getValue (Durable durable, Class fieldType, String fieldName) {

    Object returnValue;

    try {
      returnValue = BeanUtility.executeGet(durable, fieldName);
    }
    catch (Exception exception) {
      throw new CacheAutomationError(exception);
    }

    if (!TypeUtility.isEssentiallyTheSameAs(fieldType, returnValue.getClass())) {
      throw new CacheAutomationError("The getter for field(%s) on cache helper (%s) must return a type assignable to '%s'", fieldName, durable.getClass().getName(), fieldType.getName());
    }

    return (Comparable)returnValue;
  }
}
