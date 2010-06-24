package org.smallmind.persistence.cache.aop;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.DurableVector;
import org.smallmind.persistence.VectorKey;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.orm.WaterfallORMDao;

@Aspect
public class CacheAsAspect {

   @Around (value = "execution(@CacheAs * * (..)) && @annotation(cacheAs) && this(waterfallOrmDao)", argNames = "thisJoinPoint, cacheAs, waterfallOrmDao")
   public Object aroundCacheAsMethod (ProceedingJoinPoint thisJoinPoint, CacheAs cacheAs, WaterfallORMDao waterfallOrmDao)
      throws Throwable {

      Type returnType;
      Class<Durable> durableClass;

      if (!Iterable.class.isAssignableFrom(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {
         throw new CacheAutomationError("Methods annotated with @CacheAs must return a value of type <? extends Iterable>");
      }

      if ((!((returnType = ((MethodSignature)thisJoinPoint.getSignature()).getMethod().getGenericReturnType()) instanceof ParameterizedType)) || (!waterfallOrmDao.getManagedClass().equals(durableClass = (Class<Durable>)((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
         throw new CacheAutomationError("Methods annotated with @CacheAs must return a value of type <? extends Iterable<%s>>", waterfallOrmDao.getManagedClass().getSimpleName());
      }

      VectorKey vectorKey;
      VectoredDao nextDao = waterfallOrmDao.getNextDao();
      Iterable iterable;

      vectorKey = new VectorKey(cacheAs.value().with(), VectorIndex.getValue(thisJoinPoint, cacheAs.value().on()), durableClass, cacheAs.value().classifier());
      if (nextDao != null) {

         DurableVector vector;

         if ((vector = nextDao.getVector(vectorKey)) != null) {
            return vector.asList();
         }
      }

      iterable = (Iterable)thisJoinPoint.proceed();

      if (nextDao != null) {
         return nextDao.persistVector(vectorKey, iterable).asList();
      }

      return iterable;
   }
}