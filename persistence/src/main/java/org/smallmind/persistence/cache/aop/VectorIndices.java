package org.smallmind.persistence.cache.aop;

import org.smallmind.persistence.Durable;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.VectorIndex;
import org.smallmind.persistence.model.aop.AOPUtility;
import org.smallmind.persistence.model.bean.BeanUtility;
import org.smallmind.persistence.model.reflect.ReflectionUtility;
import org.smallmind.persistence.orm.DaoManager;
import org.smallmind.persistence.orm.ORMDao;
import org.aspectj.lang.JoinPoint;

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

         indices[count] = new VectorIndex(vector.value()[count].with(), indexValue);
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

         indices[count] = new VectorIndex(vector.value()[count].with(), indexValue);
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

      if (!ReflectionUtility.isEssentiallyTheSameAs(fieldType, returnValue.getClass())) {
         throw new CacheAutomationError("The getter for field(%s) on cache helper (%s) must return a type assignable to '%s'", fieldName, durable.getClass().getName(), fieldType.getName());
      }

      return (Comparable)returnValue;
   }
}
