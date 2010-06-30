package org.smallmind.persistence.statistics.aop;

import java.lang.reflect.Method;
import org.smallmind.persistence.PersistenceManager;
import org.smallmind.persistence.orm.WaterfallORMDao;
import org.smallmind.persistence.statistics.StatisticsFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class ApplyStatisticsAspect {

   private static final StatisticsFactory STATISTICS_FACTORY = PersistenceManager.getPersistence().getStatisticsFactory();

   @Around (value = "@within(statisticsStopwatch) && execution(@org.smallmind.persistence.statistics.aop.ApplyStatistics * * (..)) && this(waterfallOrmDao)", argNames = "thisJoinPoint, statisticsStopwatch, waterfallOrmDao")
   public Object aroundApplyStatisticsMethod (ProceedingJoinPoint thisJoinPoint, StatisticsStopwatch statisticsStopwatch, WaterfallORMDao waterfallOrmDao)
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
            STATISTICS_FACTORY.getStatistics().addStatLine(waterfallOrmDao.getManagedClass(), executedMethod, waterfallOrmDao.getStatisticsSource(), stop - start);
         }
      }
   }
}
