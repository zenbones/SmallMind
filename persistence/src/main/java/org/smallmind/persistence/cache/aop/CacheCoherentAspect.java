/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.smallmind.instrument.Clocks;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.MetricRegistry;
import org.smallmind.instrument.MetricRegistryFactory;
import org.smallmind.instrument.Metrics;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.Persistence;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.PersistenceMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.smallmind.persistence.orm.VectorAwareORMDao;
import org.smallmind.persistence.statistics.aop.StatisticsStopwatch;

@Aspect
public class CacheCoherentAspect {

  private static final Persistence PERSISTENCE;
  private static final MetricRegistry METRIC_REGISTRY;

  static {

    if (((PERSISTENCE = PersistenceManager.getPersistence()) == null) || (!PERSISTENCE.getStatistics().isStaticsEnabled())) {
      METRIC_REGISTRY = null;
    }
    else {
      if ((METRIC_REGISTRY = MetricRegistryFactory.getMetricRegistry()) == null) {
        throw new ExceptionInInitializerError("No MetricRegistry instance has been registered with the MetricRegistryFactory");
      }
    }
  }

  @Around(value = "execution(@CacheCoherent * * (..)) && this(ormDao)", argNames = "thisJoinPoint, ormDao")
  public Object aroundCacheCoherentMethod (ProceedingJoinPoint thisJoinPoint, VectorAwareORMDao ormDao)
    throws Throwable {

    Annotation statisticsStopwatchAnnotation;
    Method executedMethod = null;
    boolean timingEnabled;
    long start = 0;
    long stop;

    statisticsStopwatchAnnotation = ormDao.getClass().getAnnotation(StatisticsStopwatch.class);
    if (timingEnabled = (METRIC_REGISTRY != null) && (statisticsStopwatchAnnotation != null) && ((StatisticsStopwatch)statisticsStopwatchAnnotation).value()) {
      start = System.currentTimeMillis();
    }

    try {

      VectoredDao vectoredDao = ormDao.getVectoredDao();
      Type returnType;

      if (ormDao.getManagedClass().equals(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {

        Durable durable;

        if ((durable = (Durable)thisJoinPoint.proceed()) != null) {
          if (vectoredDao == null) {

            return durable;
          }

          return vectoredDao.persist(ormDao.getManagedClass(), durable, PersistenceMode.SOFT);
        }

        return null;
      }
      else if (List.class.isAssignableFrom(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {
        if ((!((returnType = (executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!ormDao.getManagedClass().equals(((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
          throw new CacheAutomationError("Methods annotated with @CacheCoherent which return a List type must be parameterized as <? extends List<%s>>", ormDao.getManagedClass().getSimpleName());
        }

        List list;

        if ((list = (List)thisJoinPoint.proceed()) != null) {
          if (vectoredDao == null) {

            return list;
          }

          IntrinsicRoster<Durable> cacheConsistentElements;

          cacheConsistentElements = new IntrinsicRoster<Durable>();
          for (Object element : list) {
            if (element != null) {
              cacheConsistentElements.add(vectoredDao.persist(ormDao.getManagedClass(), (Durable)element, PersistenceMode.SOFT));
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
        if ((!((returnType = (executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!ormDao.getManagedClass().equals(((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
          throw new CacheAutomationError("Methods annotated with @CacheCoherent which return an Iterable type must be parameterized as <? extends Iterable<%s>>", ormDao.getManagedClass().getSimpleName());
        }

        Iterable iterable;

        if ((iterable = (Iterable)thisJoinPoint.proceed()) != null) {
          if (vectoredDao == null) {

            return iterable;
          }

          return new CacheCoherentIterator(iterable.iterator(), ormDao.getManagedClass(), vectoredDao);
        }

        return null;
      }
      else {
        throw new CacheAutomationError("Methods annotated with @CacheCoherent must return either the managed Class(%s), a parameterized List <? extends List<%s>>, or a parameterized Iterable <? extends Iterable<%s>>", ormDao.getManagedClass().getSimpleName(), ormDao.getManagedClass().getSimpleName(), ormDao.getManagedClass().getSimpleName());
      }
    }
    finally {
      if (timingEnabled) {
        stop = System.currentTimeMillis();

        if (executedMethod == null) {
          executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod();
        }

        METRIC_REGISTRY.ensure(Metrics.buildChronometer(TimeUnit.MILLISECONDS, PERSISTENCE.getStatistics().getTickInterval(), PERSISTENCE.getStatistics().getTickTimeUnit(), Clocks.NANO), PERSISTENCE.getStatistics().getMetricDomain(), new MetricProperty("durable", ormDao.getManagedClass().getSimpleName()), new MetricProperty("method", executedMethod.getName()), new MetricProperty("source", ormDao.getStatisticsSource())).update(stop - start, TimeUnit.MILLISECONDS);
      }
    }
  }
}