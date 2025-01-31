/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.ORMDao;
import org.smallmind.persistence.orm.aop.Timed;

@Aspect
public class CacheAsAspect {

  private static final Random RANDOM = new SecureRandom();

  @Around(value = "execution(@CacheAs * * (..)) && @annotation(cacheAs) && this(ormDao)", argNames = "thisJoinPoint, cacheAs, ormDao")
  public Object aroundCacheAsMethod (ProceedingJoinPoint thisJoinPoint, CacheAs cacheAs, ORMDao ormDao)
    throws Throwable {

    Timed timed = ormDao.getClass().getAnnotation(Timed.class);
    MethodSignature methodSignature;
    Method executedMethod = null;
    String metricSource = null;
    boolean timingEnabled;
    long start = 0;
    long stop;

    if (timingEnabled = ((timed != null) && timed.value())) {
      start = System.nanoTime();
    }

    methodSignature = (MethodSignature)thisJoinPoint.getSignature();

    try {

      Type returnType;

      if (cacheAs.time().value() < 0) {
        throw new CacheAutomationError("The base time(%d) value of a @CacheAs annotation can not be negative", cacheAs.time().value());
      }

      if (cacheAs.time().stochastic() < 0) {
        throw new CacheAutomationError("The stochastic(%d) attribute of a @CacheAs annotation can not be negative", cacheAs.time().stochastic());
      }

      if (ormDao.getManagedClass().isAssignableFrom(methodSignature.getReturnType())) {
        if (cacheAs.ordered()) {
          throw new CacheAutomationError("A method annotated with @CacheAs which does not return an Iterable type can't be ordered", cacheAs.comparator().getName());
        } else if (cacheAs.max() > 0) {
          throw new CacheAutomationError("A method annotated with @CacheAs which does not return an Iterable type may not define a maximum size", cacheAs.comparator().getName());
        } else if (!cacheAs.comparator().equals(Comparator.class)) {
          throw new CacheAutomationError("A method annotated with @CacheAs which does not return an Iterable type can not register a comparator(%s)", cacheAs.comparator().getName());
        }

        VectoredDao vectoredDao;

        if ((vectoredDao = ormDao.getVectoredDao()) == null) {
          metricSource = ormDao.getMetricSource();

          return thisJoinPoint.proceed();
        } else {

          VectorKey vectorKey;
          DurableVector vector;

          vectorKey = new VectorKey(VectorCalculator.getVectorArtifact(cacheAs.value(), thisJoinPoint), ormDao.getManagedClass(), Classifications.get(CacheAs.class, thisJoinPoint, cacheAs.value()));

          if ((vector = vectoredDao.getVector(vectorKey)) != null) {
            if (!vector.isAlive()) {
              vectoredDao.deleteVector(vectorKey);
            } else {
              metricSource = vectoredDao.getMetricSource();

              return vector.head();
            }
          }

          Durable durable;

          metricSource = ormDao.getMetricSource();

          if ((durable = (Durable)thisJoinPoint.proceed()) != null) {

            return vectoredDao.persistVector(vectorKey, vectoredDao.createSingularVector(vectorKey, durable, getTimeToLiveSeconds(cacheAs))).head();
          }

          return null;
        }
      } else if (Iterable.class.isAssignableFrom(methodSignature.getReturnType())) {
        if ((!cacheAs.comparator().equals(Comparator.class)) && (!cacheAs.ordered())) {
          throw new CacheAutomationError("A method annotated with @CacheAs has registered a comparator(%s) but is not ordered", cacheAs.comparator().getName());
        }

        if ((!((returnType = (executedMethod = methodSignature.getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!ormDao.getManagedClass().isAssignableFrom((Class<?>)((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
          throw new CacheAutomationError("Methods annotated with @CacheAs which return an Iterable type must be parameterized to <? extends Iterable<? extends %s>>", ormDao.getManagedClass().getSimpleName());
        }

        VectoredDao vectoredDao;

        if ((vectoredDao = ormDao.getVectoredDao()) == null) {
          metricSource = ormDao.getMetricSource();

          return thisJoinPoint.proceed();
        } else {

          VectorKey vectorKey;
          DurableVector vector;

          vectorKey = new VectorKey(VectorCalculator.getVectorArtifact(cacheAs.value(), thisJoinPoint), ormDao.getManagedClass(), Classifications.get(CacheAs.class, thisJoinPoint, cacheAs.value()));

          if ((vector = vectoredDao.getVector(vectorKey)) != null) {
            if (!vector.isAlive()) {
              vectoredDao.deleteVector(vectorKey);
            } else {
              metricSource = vectoredDao.getMetricSource();

              return List.class.isAssignableFrom(methodSignature.getReturnType()) ? vector.asBestEffortPreFetchedList() : vector.asBestEffortLazyList();
            }
          }

          Iterable iterable;

          metricSource = ormDao.getMetricSource();

          if ((iterable = (Iterable)thisJoinPoint.proceed()) != null) {
            vector = vectoredDao.persistVector(vectorKey, vectoredDao.createVector(vectorKey, iterable, cacheAs.comparator().equals(Comparator.class) ? null : cacheAs.comparator().newInstance(), cacheAs.max(), getTimeToLiveSeconds(cacheAs), cacheAs.ordered()));

            return List.class.isAssignableFrom(methodSignature.getReturnType()) ? vector.asBestEffortPreFetchedList() : vector.asBestEffortLazyList();
          }

          return null;
        }
      } else {
        throw new CacheAutomationError("Methods annotated with @CacheAs must either return their managed type(%s), or an Iterable parameterized to their managed type <? extends Iterable<? extends %s>>", ormDao.getManagedClass().getSimpleName(), ormDao.getManagedClass().getSimpleName());
      }
    } catch (Throwable throwable) {
      timingEnabled = false;

      throw throwable;
    } finally {
      if (timingEnabled) {
        stop = System.nanoTime();

        if (executedMethod == null) {
          executedMethod = methodSignature.getMethod();
        }

        Instrument.with(thisJoinPoint.getStaticPart().getSourceLocation().getWithinType(), MeterFactory.instance(SpeedometerBuilder::new), new Tag("durable", ormDao.getManagedClass().getSimpleName()), new Tag("method", executedMethod.getName()), new Tag("source", metricSource)).update(stop - start, TimeUnit.NANOSECONDS);
      }
    }
  }

  private int getTimeToLiveSeconds (CacheAs cacheAs) {

    if ((cacheAs.time().value() == 0) && (cacheAs.time().stochastic() == 0)) {

      return 0;
    }

    int baseTime = (int)cacheAs.time().unit().toSeconds(cacheAs.time().value());

    if (cacheAs.time().stochastic() > 0) {
      baseTime += (cacheAs.time().unit().toSeconds(cacheAs.time().stochastic()) * RANDOM.nextDouble()) + 1;
    }

    return baseTime;
  }
}
