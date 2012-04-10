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
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.Random;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.smallmind.persistence.Durable;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.cache.DurableVector;
import org.smallmind.persistence.cache.VectorKey;
import org.smallmind.persistence.cache.VectoredDao;
import org.smallmind.persistence.orm.VectorAwareORMDao;
import org.smallmind.persistence.statistics.StatisticsFactory;
import org.smallmind.persistence.statistics.aop.StatisticsStopwatch;

@Aspect
public class CacheAsAspect {

  private static final Random RANDOM = new SecureRandom();
  private static final StatisticsFactory STATISTICS_FACTORY;

  static {

    if (PersistenceManager.getPersistence() == null) {
      throw new ExceptionInInitializerError("No Persistence instance has been registered with the PersistenceManager");
    }

    STATISTICS_FACTORY = PersistenceManager.getPersistence().getStatisticsFactory();
  }

  @Around(value = "execution(@CacheAs * * (..)) && @annotation(cacheAs) && this(ormDao)", argNames = "thisJoinPoint, cacheAs, ormDao")
  public Object aroundCacheAsMethod (ProceedingJoinPoint thisJoinPoint, CacheAs cacheAs, VectorAwareORMDao ormDao)
    throws Throwable {

    Annotation statisticsStopwatchAnnotation;
    MethodSignature methodSignature;
    Method executedMethod = null;
    String statisticsSource = null;
    boolean timingEnabled;
    long start = 0;
    long stop;

    statisticsStopwatchAnnotation = ormDao.getClass().getAnnotation(StatisticsStopwatch.class);
    if (timingEnabled = STATISTICS_FACTORY.isEnabled() && (statisticsStopwatchAnnotation != null) && ((StatisticsStopwatch)statisticsStopwatchAnnotation).value()) {
      start = System.currentTimeMillis();
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

      if (ormDao.getManagedClass().equals(methodSignature.getReturnType())) {
        if (cacheAs.ordered()) {
          throw new CacheAutomationError("A method annotated with @CacheAs which does not return an Iterable type can't be ordered", cacheAs.comparator().getClass().getName());
        }
        else if (cacheAs.max() > 0) {
          throw new CacheAutomationError("A method annotated with @CacheAs which does not return an Iterable type may not define a maximum size", cacheAs.comparator().getClass().getName());
        }
        else if (!cacheAs.comparator().equals(Comparator.class)) {
          throw new CacheAutomationError("A method annotated with @CacheAs which does not return an Iterable type can not register a comparator(%s)", cacheAs.comparator().getClass().getName());
        }

        VectoredDao vectoredDao;

        if ((vectoredDao = ormDao.getVectoredDao()) == null) {
          statisticsSource = ormDao.getStatisticsSource();

          return thisJoinPoint.proceed();
        }
        else {

          VectorKey vectorKey;
          DurableVector vector;

          vectorKey = new VectorKey(VectorIndices.getVectorIndexes(cacheAs.value(), thisJoinPoint), ormDao.getManagedClass(), Classifications.get(CacheAs.class, thisJoinPoint, cacheAs.value()));

          if ((vector = vectoredDao.getVector(vectorKey)) != null) {
            if (!vector.isAlive()) {
              vectoredDao.deleteVector(vectorKey);
            }
            else {
              statisticsSource = vectoredDao.getStatisticsSource();

              return vector.head();
            }
          }

          Durable durable;

          statisticsSource = ormDao.getStatisticsSource();

          if ((durable = (Durable)thisJoinPoint.proceed()) != null) {

            return vectoredDao.persistVector(vectorKey, vectoredDao.createSingularVector(vectorKey, durable, getTimeToLiveSeconds(cacheAs))).head();
          }

          return null;
        }
      }
      else if (Iterable.class.isAssignableFrom(methodSignature.getReturnType())) {
        if ((!cacheAs.comparator().equals(Comparator.class)) && (!cacheAs.ordered())) {
          throw new CacheAutomationError("A method annotated with @CacheAs has registered a comparator(%s) but is not ordered", cacheAs.comparator().getClass().getName());
        }

        if ((!((returnType = (executedMethod = methodSignature.getMethod()).getGenericReturnType()) instanceof ParameterizedType)) || (!ormDao.getManagedClass().equals(((ParameterizedType)returnType).getActualTypeArguments()[0]))) {
          throw new CacheAutomationError("Methods annotated with @CacheAs which return an Iterable type must be parameterized to <? extends Iterable<%s>>", ormDao.getManagedClass().getSimpleName());
        }

        VectoredDao vectoredDao;

        if ((vectoredDao = ormDao.getVectoredDao()) == null) {
          statisticsSource = ormDao.getStatisticsSource();

          return thisJoinPoint.proceed();
        }
        else {

          VectorKey vectorKey;
          DurableVector vector;

          vectorKey = new VectorKey(VectorIndices.getVectorIndexes(cacheAs.value(), thisJoinPoint), ormDao.getManagedClass(), Classifications.get(CacheAs.class, thisJoinPoint, cacheAs.value()));

          if ((vector = vectoredDao.getVector(vectorKey)) != null) {
            if (!vector.isAlive()) {
              vectoredDao.deleteVector(vectorKey);
            }
            else {
              statisticsSource = vectoredDao.getStatisticsSource();

              return vector.asList();
            }
          }

          Iterable iterable;

          statisticsSource = ormDao.getStatisticsSource();

          if ((iterable = (Iterable)thisJoinPoint.proceed()) != null) {

            return vectoredDao.persistVector(vectorKey, vectoredDao.createVector(vectorKey, iterable, cacheAs.comparator().equals(Comparator.class) ? null : cacheAs.comparator().newInstance(), cacheAs.max(), getTimeToLiveSeconds(cacheAs), cacheAs.ordered())).asList();
          }

          return null;
        }
      }
      else {
        throw new CacheAutomationError("Methods annotated with @CacheAs must either return their managed type(%s), or an Iterable parameterized to their managed type <? extends Iterable<%s>>", ormDao.getManagedClass().getSimpleName(), ormDao.getManagedClass().getSimpleName());
      }
    }
    catch (Throwable throwable) {
      timingEnabled = false;

      throw throwable;
    }
    finally {
      if (timingEnabled) {
        stop = System.currentTimeMillis();

        if (executedMethod == null) {
          executedMethod = methodSignature.getMethod();
        }

        STATISTICS_FACTORY.getStatistics().addStatLine(ormDao.getManagedClass(), executedMethod, statisticsSource, stop - start);
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
