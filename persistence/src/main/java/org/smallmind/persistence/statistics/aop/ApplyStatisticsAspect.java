/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.statistics.aop;

import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.orm.VectorAwareORMDao;
import org.smallmind.persistence.statistics.StatisticsFactory;

@Aspect
public class ApplyStatisticsAspect {

  private static final StatisticsFactory STATISTICS_FACTORY = PersistenceManager.getPersistence().getStatisticsFactory();

  @Around(value = "@within(statisticsStopwatch) && execution(@org.smallmind.persistence.statistics.aop.ApplyStatistics * * (..)) && this(ormDao)", argNames = "thisJoinPoint, statisticsStopwatch, ormDao")
  public Object aroundApplyStatisticsMethod (ProceedingJoinPoint thisJoinPoint, StatisticsStopwatch statisticsStopwatch, VectorAwareORMDao ormDao)
    throws Throwable {

    Method executedMethod;
    boolean timingEnabled;
    long start = 0;
    long stop;

    if (timingEnabled = STATISTICS_FACTORY.isEnabled() && statisticsStopwatch.value()) {
      start = System.currentTimeMillis();
    }

    try {

      return thisJoinPoint.proceed();
    }
    finally {
      if (timingEnabled) {
        stop = System.currentTimeMillis();
        executedMethod = ((MethodSignature)thisJoinPoint.getSignature()).getMethod();
        STATISTICS_FACTORY.getStatistics().addStatLine(ormDao.getManagedClass(), executedMethod, ormDao.getStatisticsSource(), stop - start);
      }
    }
  }
}
