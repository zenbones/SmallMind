package org.smallmind.persistence;

import org.smallmind.persistence.model.type.converter.StringConverterFactory;
import org.smallmind.persistence.statistics.StatisticsFactory;

public class Persistence {

   private StatisticsFactory statisticsFactory;
   private StringConverterFactory stringConverterFactory;

   public Persistence (StatisticsFactory statisticsFactory, StringConverterFactory stringConverterFactory) {

      this.statisticsFactory = statisticsFactory;
      this.stringConverterFactory = stringConverterFactory;
   }

   public void register () {

      PersistenceManager.register(this);
   }

   public StatisticsFactory getStatisticsFactory () {

      return statisticsFactory;
   }

   public StringConverterFactory getStringConverterFactory () {

      return stringConverterFactory;
   }
}
