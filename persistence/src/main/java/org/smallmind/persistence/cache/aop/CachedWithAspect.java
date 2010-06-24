package org.smallmind.persistence.cache.aop;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.VectorKey;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.orm.WaterfallORMDao;

@Aspect
public class CachedWithAspect {

   private static final ConcurrentHashMap<MethodKey, Method> METHOD_MAP = new ConcurrentHashMap<MethodKey, Method>();

   @AfterReturning (value = "execution(* persist (..)) && this(waterfallOrmDao)", returning = "durable", argNames = "waterfallOrmDao, durable")
   public void afterReturningPersistMethod (WaterfallORMDao waterfallOrmDao, Durable durable) {

      CachedWith cachedWith;
      Iterable<Durable> finderIterable;
      VectoredDao nextDao = waterfallOrmDao.getNextDao();

      if (nextDao != null) {
         if ((cachedWith = waterfallOrmDao.getClass().getAnnotation(CachedWith.class)) != null) {
            for (Update update : cachedWith.updates()) {
               if (executeFilter(update.filter(), waterfallOrmDao, durable)) {
                  nextDao.updateInVector(new VectorKey(update.value().with(), VectorIndex.getValue(durable, update.value().on()), waterfallOrmDao.getManagedClass(), update.value().classifier()), durable);
               }
            }

            for (Finder finder : cachedWith.finders()) {
               if ((finderIterable = executeFinder(finder, waterfallOrmDao, durable)) != null) {
                  for (Durable indexingDurable : finderIterable) {
                     if (executeFilter(finder.filter(), waterfallOrmDao, durable)) {
                        nextDao.updateInVector(new VectorKey(finder.vector().with(), VectorIndex.getValue(indexingDurable, finder.vector().on()), waterfallOrmDao.getManagedClass(), finder.vector().classifier()), durable);
                     }
                  }
               }
            }
         }
      }
   }

   @AfterReturning (value = "execution(void delete (..)) && args(durable) && this(waterfallOrmDao)", argNames = "waterfallOrmDao, durable")
   public void afterReturningDeleteMethod (WaterfallORMDao waterfallOrmDao, Durable durable) {

      CachedWith cachedWith;
      Iterable<Durable> finderIterable;
      VectoredDao nextDao = waterfallOrmDao.getNextDao();

      if (nextDao != null) {
         if ((cachedWith = waterfallOrmDao.getClass().getAnnotation(CachedWith.class)) != null) {

            for (Update update : cachedWith.updates()) {
               if (executeFilter(update.filter(), waterfallOrmDao, durable)) {
                  nextDao.removeFromVector(new VectorKey(update.value().with(), VectorIndex.getValue(durable, update.value().on()), waterfallOrmDao.getManagedClass(), update.value().classifier()), durable);
               }
            }

            for (Finder finder : cachedWith.finders()) {
               if ((finderIterable = executeFinder(finder, waterfallOrmDao, durable)) != null) {
                  for (Durable indexingDurable : finderIterable) {
                     if (executeFilter(finder.filter(), waterfallOrmDao, durable)) {
                        nextDao.removeFromVector(new VectorKey(finder.vector().with(), VectorIndex.getValue(indexingDurable, finder.vector().on()), waterfallOrmDao.getManagedClass(), finder.vector().classifier()), durable);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean executeFilter (String filterMethodName, WaterfallORMDao waterfallORMDao, Durable durable) {

      Method filterMethod;

      if (filterMethodName.length() > 0) {

         if ((filterMethod = locateMethod(waterfallORMDao, filterMethodName, durable)) == null) {
            throw new CacheAutomationError("The filter Method(%s) referenced within @CachedWith does not exist", filterMethodName);
         }

         if (!(filterMethod.getReturnType().equals(boolean.class) || filterMethod.getReturnType().equals(Boolean.class))) {
            throw new CacheAutomationError("A filter Method(%s) referenced by @CachedWith must return a value of type 'boolean'", filterMethodName);
         }

         try {

            return (Boolean)filterMethod.invoke(waterfallORMDao, durable);
         }
         catch (Exception exception) {
            throw new CacheAutomationError(exception);
         }
      }

      return true;
   }

   private Iterable<Durable> executeFinder (Finder finder, WaterfallORMDao waterfallORMDao, Durable durable) {

      Method finderMethod;
      Type finderReturnType;

      if ((finderMethod = locateMethod(waterfallORMDao, finder.method(), durable)) == null) {
         throw new CacheAutomationError("Method(%s) referenced by @Finder does not exist", finder.method());
      }

      if (!Iterable.class.isAssignableFrom(finderMethod.getReturnType())) {
         throw new CacheAutomationError("A Method(%s) referenced by @Finder must return a value of type <? extends Iterable>", finder.method());
      }

      if ((!((finderReturnType = finderMethod.getGenericReturnType()) instanceof ParameterizedType)) || (!((Class)((ParameterizedType)finderReturnType).getActualTypeArguments()[0]).isAssignableFrom(finder.vector().with()))) {
         throw new CacheAutomationError("A Method(%s) referenced by @Finder must return a value of type <? extends Iterable<%s>>", finder.method(), finder.vector().with().getSimpleName());
      }

      try {

         return (Iterable<Durable>)finderMethod.invoke(waterfallORMDao, durable);
      }
      catch (Exception exception) {
         throw new CacheAutomationError(exception);
      }
   }

   private Method locateMethod (WaterfallORMDao waterfallORMDao, String methodName, Durable durable) {

      Method aspectMethod;
      MethodKey methodKey;
      Class[] parameterTypes;

      if ((aspectMethod = METHOD_MAP.get(methodKey = new MethodKey(waterfallORMDao.getClass(), methodName))) == null) {
         for (Method method : waterfallORMDao.getClass().getMethods()) {

            if (method.getName().equals(methodName) && ((parameterTypes = method.getParameterTypes()).length == 1) && (parameterTypes[0].isAssignableFrom(durable.getClass()))) {
               METHOD_MAP.put(methodKey, aspectMethod = method);
               break;
            }
         }
      }

      return aspectMethod;
   }

   private static class MethodKey {

      private Class methodClass;
      private String methodName;

      private MethodKey (Class methodClass, String methodName) {

         this.methodClass = methodClass;
         this.methodName = methodName;
      }

      public Class getMethodClass () {

         return methodClass;
      }

      public String getMethodName () {

         return methodName;
      }

      @Override
      public int hashCode () {

         return methodClass.hashCode() ^ methodName.hashCode();
      }

      @Override
      public boolean equals (Object obj) {

         return (obj instanceof MethodKey) && methodClass.equals(((MethodKey)obj).getMethodClass()) && methodName.equals(((MethodKey)obj).getMethodName());
      }
   }
}