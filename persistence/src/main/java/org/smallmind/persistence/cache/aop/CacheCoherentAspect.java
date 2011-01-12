/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.VectoredDao;
import org.smallmind.quorum.util.ConcurrentList;
import org.smallmind.persistence.orm.WaterfallORMDao;
import org.smallmind.persistence.statistics.StatisticsFactory;
import org.smallmind.persistence.statistics.aop.StatisticsStopwatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class CacheCoherentAspect {

   private static final StatisticsFactory STATISTICS_FACTORY = PersistenceManager.getPersistence().getStatisticsFactory();

   @Around (value = "execution(@CacheCoherent * * (..)) && this(waterfallOrmDao)", argNames = "thisJoinPoint, waterfallOrmDao")
   public Object aroundCacheCoherentMethod (ProceedingJoinPoint thisJoinPoint, WaterfallORMDao waterfallOrmDao)
      throws Throwable {

      Annotation statisticsStopwatchAnnotation;
      Method executedMethod = null;
      boolean timingEnabled;
      long start = 0;
      long stop;

      statisticsStopwatchAnnotation = waterfallOrmDao.getClass().getAnnotation(StatisticsStopwatch.class);
      if (timingEnabled = STATISTICS_FACTORY.isEnabled() && (statisticsStopwatchAnnotation != null) && ((StatisticsStopwatch)statisticsStopwatchAnnotation).value()) {
         start = System.currentTimeMillis();
      }

      try {

         VectoredDao nextDao = waterfallOrmDao.getNextDao();
         Type returnType;

         if (waterfallOrmDao.getManagedClass().equals(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {

            Durable durable;

            if ((durable = (Durable)thisJoinPoint.proceed()) != null) {
               if (nextDao == null) {

                  return durable;
               }

               return nextDao.persist(waterfallOrmDao.getManagedClass(), durable);
            }

            return null;
         }
         else if (List.class.isAssignableFrom(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {
            if ((!((returnType = (executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!waterfallOrmDao.getManagedClass().equals(((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
               throw new CacheAutomationError("Methods annotated with @CacheCoherent which return a List type must be parameterized as <? extends List<%s>>", waterfallOrmDao.getManagedClass().getSimpleName());
            }

            List list;

            if ((list = (List)thisJoinPoint.proceed()) != null) {
               if (nextDao == null) {

                  return list;
               }

               ConcurrentList<Durable> cacheConsistentElements;

               cacheConsistentElements = new ConcurrentList<Durable>();
               for (Object element : list) {
                  if (element != null) {
                     cacheConsistentElements.add((Durable)nextDao.persist(waterfallOrmDao.getManagedClass(), element));
                  }
                  else {
                     cacheConsistentElements.add(null);
                  }
               }

               return cacheConsistentElements;
            }

            return null;
         }
         else if (Iterable.class.isAssignableFrom(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {
            if ((!((returnType = (executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!waterfallOrmDao.getManagedClass().equals(((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
               throw new CacheAutomationError("Methods annotated with @CacheCoherent which return an Iterable type must be parameterized as <? extends Iterable<%s>>", waterfallOrmDao.getManagedClass().getSimpleName());
            }

            Iterable iterable;

            if ((iterable = (Iterable)thisJoinPoint.proceed()) != null) {
               if (nextDao == null) {

                  return iterable;
               }

               return new CacheCoherentIterator(iterable.iterator(), waterfallOrmDao.getManagedClass(), nextDao);
            }

            return null;
         }
         else {
            throw new CacheAutomationError("Methods annotated with @CacheCoherent must return either the managed Class(%s), a parameterized List <? extends List<%s>>, or a parameterized Iterable <? extends Iterable<%s>>", waterfallOrmDao.getManagedClass().getSimpleName(), waterfallOrmDao.getManagedClass().getSimpleName(), waterfallOrmDao.getManagedClass().getSimpleName());
         }
      }
      finally {
         if (timingEnabled) {
            stop = System.currentTimeMillis();

            if (executedMethod == null) {
               executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod();
            }

            STATISTICS_FACTORY.getStatistics().addStatLine(waterfallOrmDao.getManagedClass(), executedMethod, waterfallOrmDao.getStatisticsSource(), stop - start);
         }
      }
   }
}