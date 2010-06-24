package org.smallmind.scribe.pen;

public class DefaultTemplate extends Template {

   public DefaultTemplate () {

      super();
   }

   public DefaultTemplate (Level level, boolean autoFillLogicalContext)
      throws LoggerException {

      super(level, autoFillLogicalContext);
   }

   public DefaultTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLogicalContext)
      throws LoggerException {

      super(filters, appenders, enhancers, level, autoFillLogicalContext);
   }

   public int matchLogger (String loggerName) {

      return NO_MATCH + 1;
   }
}