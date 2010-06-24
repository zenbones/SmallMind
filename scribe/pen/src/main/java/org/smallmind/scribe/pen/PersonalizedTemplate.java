package org.smallmind.scribe.pen;

import java.util.concurrent.atomic.AtomicReference;

public class PersonalizedTemplate extends Template {

   private AtomicReference<String> loggerNameRef = new AtomicReference<String>();

   public PersonalizedTemplate () {

      super();
   }

   public PersonalizedTemplate (String loggerName) {

      super();

      loggerNameRef.set(loggerName);
   }

   public PersonalizedTemplate (Level level, boolean autoFillLogicalContext, String loggerName)
      throws LoggerException {

      super(level, autoFillLogicalContext);

      loggerNameRef.set(loggerName);
   }

   public PersonalizedTemplate (Filter[] filters, Appender[] appenders, Enhancer[] enhancers, Level level, boolean autoFillLogicalContext, String loggerName)
      throws LoggerException {

      super(filters, appenders, enhancers, level, autoFillLogicalContext);

      loggerNameRef.set(loggerName);
   }

   public void setLoggerName (String loggerName) {

      if (!loggerNameRef.compareAndSet(null, loggerName)) {
         throw new LoggerRuntimeException("PersonalizedTemplate has been previously initialized with a logger name");
      }
   }

   public int matchLogger (String loggerName) {

      if (loggerNameRef.get() == null) {
         throw new LoggerRuntimeException("PersonalizedTemplate was never initialized with a logger name");
      }

      return loggerNameRef.get().equals(loggerName) ? Integer.MAX_VALUE : NO_MATCH;
   }
}

