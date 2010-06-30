package org.smallmind.persistence.orm;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.persistence.Durable;

public class DaoManager {

   private static final ConcurrentHashMap<Class<? extends Durable>, ORMDao> DAO_MAP = new ConcurrentHashMap<Class<? extends Durable>, ORMDao>();

   public static void register (Class<? extends Durable> durableClass, ORMDao ormDao) {

      DAO_MAP.put(durableClass, ormDao);
   }

   public static Class<? extends Durable> findDurableClass (String simpleName) {

      for (Class<? extends Durable> durableClass : DAO_MAP.keySet()) {
         if (durableClass.getSimpleName().equals(simpleName)) {

            return durableClass;
         }
      }

      return null;
   }

   public static ORMDao get (String simpleName) {

      Class<? extends Durable> durableClass;

      if ((durableClass = findDurableClass(simpleName)) != null) {

         return DAO_MAP.get(durableClass);
      }

      return null;
   }

   public static <I extends Serializable & Comparable<I>, D extends Durable<I>> ORMDao<I, D> get (Class<D> durableClass) {

      return DAO_MAP.get(durableClass);
   }
}