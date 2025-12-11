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
package org.smallmind.persistence.cache.aop;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.persistence.AbstractVectorAwareManagedDao;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.UpdateMode;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.cache.praxis.intrinsic.IntrinsicRoster;
import org.smallmind.persistence.orm.aop.Timed;

@Aspect
public class CacheCoherentAspect {

  @Around(value = "execution(@CacheCoherent * * (..)) && this(durableDao)", argNames = "thisJoinPoint, durableDao")
  public Object aroundCacheCoherentMethod (ProceedingJoinPoint thisJoinPoint, AbstractVectorAwareManagedDao durableDao)
    throws Throwable {

    Timed timed = durableDao.getClass().getAnnotation(Timed.class);
    Method executedMethod = null;
    boolean timingEnabled;
    long start = 0;
    long stop;

    if (timingEnabled = ((timed != null) && timed.value())) {
      start = System.nanoTime();
    }

    try {

      VectoredDao vectoredDao = durableDao.getVectoredDao();
      Type returnType;

      if (durableDao.getManagedClass().isAssignableFrom(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {

        Durable durable;

        if ((durable = (Durable)thisJoinPoint.proceed()) != null) {
          if (vectoredDao == null) {

            return durable;
          }

          return vectoredDao.persist(durableDao.getManagedClass(), durable, UpdateMode.SOFT);
        }

        return null;
      } else if (List.class.isAssignableFrom(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {
        if ((!((returnType = (executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!durableDao.getManagedClass().isAssignableFrom((Class<?>)((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
          throw new CacheAutomationError("Methods annotated with @CacheCoherent which return a List type must be parameterized as <? extends List<? extends %s>>", durableDao.getManagedClass().getSimpleName());
        }

        List list;

        if ((list = (List)thisJoinPoint.proceed()) != null) {
          if (vectoredDao == null) {

            return list;
          }

          IntrinsicRoster<Durable> cacheConsistentElements;

          cacheConsistentElements = new IntrinsicRoster<>();
          for (Object element : list) {
            if (element != null) {
              cacheConsistentElements.add(vectoredDao.persist(durableDao.getManagedClass(), (Durable)element, UpdateMode.SOFT));
            } else {
              cacheConsistentElements.add(null);
            }
          }

          return cacheConsistentElements;
        }

        return null;
      } else if (Iterable.class.isAssignableFrom(((MethodSignature)thisJoinPoint.getSignature()).getReturnType())) {
        if ((!((returnType = (executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!durableDao.getManagedClass().isAssignableFrom((Class<?>)((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
          throw new CacheAutomationError("Methods annotated with @CacheCoherent which return an Iterable type must be parameterized as <? extends Iterable<? extends %s>>", durableDao.getManagedClass().getSimpleName());
        }

        Iterable iterable;

        if ((iterable = (Iterable)thisJoinPoint.proceed()) != null) {
          if (vectoredDao == null) {

            return iterable;
          }

          return new CacheCoherentIterable(iterable, durableDao.getManagedClass(), vectoredDao);
        }

        return null;
      } else {
        throw new CacheAutomationError("Methods annotated with @CacheCoherent must return either the managed Class(%s), a parameterized List <? extends List<%s>>, or a parameterized Iterable <? extends Iterable<%s>>", durableDao.getManagedClass().getSimpleName(), durableDao.getManagedClass().getSimpleName(), durableDao.getManagedClass().getSimpleName());
      }
    } catch (Throwable throwable) {
      timingEnabled = false;

      throw throwable;
    } finally {
      if (timingEnabled) {
        stop = System.nanoTime();

        if (executedMethod == null) {
          executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod();
        }

        Instrument.with(thisJoinPoint.getStaticPart().getSourceLocation().getWithinType(), MeterFactory.instance(SpeedometerBuilder::new), new Tag("durable", durableDao.getManagedClass().getSimpleName()), new Tag("method", executedMethod.getName()), new Tag("source", durableDao.getMetricSource())).update(stop - start, TimeUnit.NANOSECONDS);
      }
    }
  }
}
