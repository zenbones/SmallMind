/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.persistence.AbstractVectorAwareManagedDao;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.smallmind.persistence.instrument.aop.Instrumented;

@Aspect
public class CacheCoherentAspect {

  @Around(value = "execution(@CacheCoherent * * (..)) && this(durableDao)", argNames = "thisJoinPoint, durableDao")
  public Object aroundCacheCoherentMethod (ProceedingJoinPoint thisJoinPoint, AbstractVectorAwareManagedDao durableDao)
    throws Throwable {

    Annotation instrumentedAnnotation;
    Method executedMethod = null;
    boolean timingEnabled;
    long start = 0;
    long stop;

    instrumentedAnnotation = durableDao.getClass().getAnnotation(Instrumented.class);
    if (timingEnabled = (instrumentedAnnotation != null) && ((Instrumented)instrumentedAnnotation).value()) {
      start = System.currentTimeMillis();
    }

    try {

      VectoredDao vectoredDao = durableDao.getVectoredDao();
      Type returnType;

      if (durableDao.getManagedClass().equals(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {

        Durable durable;

        if ((durable = (Durable)thisJoinPoint.proceed()) != null) {
          if (vectoredDao == null) {

            return durable;
          }

          return vectoredDao.persist(durableDao.getManagedClass(), durable, UpdateMode.SOFT);
        }

        return null;
      }
      else if (List.class.isAssignableFrom(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {
        if ((!((returnType = (executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!durableDao.getManagedClass().equals(((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
          throw new CacheAutomationError("Methods annotated with @CacheCoherent which return a List type must be parameterized as <? extends List<%s>>", durableDao.getManagedClass().getSimpleName());
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
              cacheConsistentElements.add(vectoredDao.persist(durableDao.getManagedClass(), (Durable)element, UpdateMode.SOFT));
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
        if ((!((returnType = (executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!durableDao.getManagedClass().equals(((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
          throw new CacheAutomationError("Methods annotated with @CacheCoherent which return an Iterable type must be parameterized as <? extends Iterable<%s>>", durableDao.getManagedClass().getSimpleName());
        }

        Iterable iterable;

        if ((iterable = (Iterable)thisJoinPoint.proceed()) != null) {
          if (vectoredDao == null) {

            return iterable;
          }

          return new CacheCoherentIterator(iterable.iterator(), durableDao.getManagedClass(), vectoredDao);
        }

        return null;
      }
      else {
        throw new CacheAutomationError("Methods annotated with @CacheCoherent must return either the managed Class(%s), a parameterized List <? extends List<%s>>, or a parameterized Iterable <? extends Iterable<%s>>", durableDao.getManagedClass().getSimpleName(), durableDao.getManagedClass().getSimpleName(), durableDao.getManagedClass().getSimpleName());
      }
    }
    finally {
      if (timingEnabled) {
        stop = System.currentTimeMillis();

        if (executedMethod == null) {
          executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod();
        }

        InstrumentationManager.instrumentWithChronometer(PersistenceManager.getPersistence(), stop - start, TimeUnit.MILLISECONDS, new MetricProperty("durable", durableDao.getManagedClass().getSimpleName()), new MetricProperty("method", executedMethod.getName()), new MetricProperty("source", durableDao.getMetricSource()));
      }
    }
  }
}