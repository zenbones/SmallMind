package org.smallmind.persistence.cache.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.Random;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.VectorKey;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.orm.WaterfallORMDao;
import org.smallmind.persistence.statistics.StatisticsFactory;
import org.smallmind.persistence.statistics.aop.StatisticsStopwatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class CacheAsAspect {

   private static final Random RANDOM = new SecureRandom();
   private static final StatisticsFactory STATISTICS_FACTORY = PersistenceManager.getPersistence().getStatisticsFactory();

   @Around (value = "execution(@CacheAs * * (..)) && @annotation(cacheAs) && this(waterfallOrmDao)", argNames = "thisJoinPoint, cacheAs, waterfallOrmDao")
   public Object aroundCacheAsMethod (ProceedingJoinPoint thisJoinPoint, CacheAs cacheAs, WaterfallORMDao waterfallOrmDao)
      throws Throwable {

      Annotation statisticsStopwatchAnnotation;
      Method executedMethod = null;
      String statisticsSource = null;
      boolean timingEnabled;
      long start = 0;
      long stop;

      statisticsStopwatchAnnotation = waterfallOrmDao.getClass().getAnnotation(StatisticsStopwatch.class);
      if (timingEnabled = STATISTICS_FACTORY.isEnabled() && (statisticsStopwatchAnnotation != null) && ((StatisticsStopwatch)statisticsStopwatchAnnotation).value()) {
         start = System.currentTimeMillis();
      }

      try {

         Type returnType;

         if (cacheAs.time().value() < 0) {
            throw new CacheAutomationError("The base time(%d) value of a @CacheAs annotation can not be negative", cacheAs.time().value());
         }

         if (cacheAs.time().stochastic() < 0) {
            throw new CacheAutomationError("The stochastic(%d) attribute of a @CacheAs annotation can not be negative", cacheAs.time().stochastic());
         }

         if (cacheAs.singular()) {
            if (cacheAs.ordered()) {
               throw new CacheAutomationError("A method annotated with @CacheAs (singular = true) can't be ordered", cacheAs.comparator().getClass().getName());
            }
            else if (cacheAs.max() > 0) {
               throw new CacheAutomationError("A method annotated with @CacheAs (singular = true) may not define a maximum size", cacheAs.comparator().getClass().getName());
            }
            else if (!cacheAs.comparator().equals(Comparator.class)) {
               throw new CacheAutomationError("A method annotated with @CacheAs (singular = true) can not register a comparator(%s)", cacheAs.comparator().getClass().getName());
            }

            if (!waterfallOrmDao.getManagedClass().equals(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {
               throw new CacheAutomationError("A method annotated with @CacheAs (singular = true) must return its managed type(%s)", waterfallOrmDao.getManagedClass().getSimpleName());
            }

            VectorKey vectorKey;
            VectoredDao nextDao = waterfallOrmDao.getNextDao();

            vectorKey = new VectorKey(VectorIndices.getVectorIndexes(cacheAs.value(), thisJoinPoint, waterfallOrmDao), waterfallOrmDao.getManagedClass(), Classifications.get(CacheAs.class, thisJoinPoint, cacheAs.value()));

            if (nextDao != null) {

               DurableVector vector;

               if ((vector = nextDao.getVector(vectorKey)) != null) {
                  if (!vector.isAlive()) {
                     nextDao.deleteVector(vectorKey);
                  }
                  else {
                     statisticsSource = nextDao.getStatisticsSource();

                     return vector.head();
                  }
               }
            }

            Durable durable;

            statisticsSource = waterfallOrmDao.getStatisticsSource();
            if ((durable = (Durable)thisJoinPoint.proceed()) != null) {
               if (nextDao != null) {

                  DurableVector vector = nextDao.createSingularVector(vectorKey, durable, getTimeToLive(cacheAs));

                  return nextDao.persistVector(vectorKey, vector).head();
               }
            }

            return durable;
         }
         else {
            if ((!cacheAs.comparator().equals(Comparator.class)) && (!cacheAs.ordered())) {
               throw new CacheAutomationError("A method annotated with @CacheAs has registered a comparator(%s) but is not ordered", cacheAs.comparator().getClass().getName());
            }

            if (!Iterable.class.isAssignableFrom(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {
               throw new CacheAutomationError("Methods annotated with @CacheAs (singular = false) must return a value of type <? extends Iterable>");
            }

            if ((!((returnType = (executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!waterfallOrmDao.getManagedClass().equals(((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
               throw new CacheAutomationError("Methods annotated with @CacheAs (singular = false) must return a value of type <? extends Iterable<%s>>", waterfallOrmDao.getManagedClass().getSimpleName());
            }

            VectorKey vectorKey;
            VectoredDao nextDao = waterfallOrmDao.getNextDao();

            vectorKey = new VectorKey(VectorIndices.getVectorIndexes(cacheAs.value(), thisJoinPoint, waterfallOrmDao), waterfallOrmDao.getManagedClass(), Classifications.get(CacheAs.class, thisJoinPoint, cacheAs.value()));

            if (nextDao != null) {

               DurableVector vector;

               if ((vector = nextDao.getVector(vectorKey)) != null) {
                  if (!vector.isAlive()) {
                     nextDao.deleteVector(vectorKey);
                  }
                  else {
                     statisticsSource = nextDao.getStatisticsSource();

                     return vector.asList();
                  }
               }
            }

            Iterable iterable;

            statisticsSource = waterfallOrmDao.getStatisticsSource();
            if ((iterable = (Iterable)thisJoinPoint.proceed()) != null) {
               if (nextDao != null) {

                  DurableVector vector = nextDao.createVector(vectorKey, iterable, cacheAs.comparator().equals(Comparator.class) ? null : cacheAs.comparator().newInstance(), cacheAs.max(), getTimeToLive(cacheAs), cacheAs.ordered());

                  return nextDao.persistVector(vectorKey, vector).asList();
               }
            }

            return iterable;
         }
      }
      finally {
         if (timingEnabled) {
            stop = System.currentTimeMillis();

            if (executedMethod == null) {
               executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod();
            }

            STATISTICS_FACTORY.getStatistics().addStatLine(waterfallOrmDao.getManagedClass(), executedMethod, statisticsSource, stop - start);
         }
      }
   }

   private long getTimeToLive (CacheAs cacheAs) {

      if ((cacheAs.time().value() == 0) && (cacheAs.time().stochastic() == 0)) {

         return 0;
      }

      long baseTime = cacheAs.time().unit().toMillis(cacheAs.time().value());

      if (cacheAs.time().stochastic() > 0) {
         baseTime += (cacheAs.time().unit().toMillis(cacheAs.time().stochastic()) * RANDOM.nextDouble()) + 1;
      }

      return baseTime;
   }
}
