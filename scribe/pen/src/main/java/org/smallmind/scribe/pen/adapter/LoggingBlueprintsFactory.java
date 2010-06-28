package org.smallmind.scribe.pen.adapter;

import java.util.Iterator;
import java.util.ServiceLoader;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

public class LoggingBlueprintsFactory {

   private static LoggingBlueprints LOGGING_BLUEPRINTS;

   static {

      Iterator<LoggingBlueprints> blueprintsIter;

      blueprintsIter = ServiceLoader.load(LoggingBlueprints.class, Thread.currentThread().getContextClassLoader()).iterator();
      if (!blueprintsIter.hasNext()) {
         throw new StaticInitializationError("No provider found for LoggingBlueprints");
      }

      LOGGING_BLUEPRINTS = (LoggingBlueprints)blueprintsIter.next();
   }

   public static LoggingBlueprints getLoggingBlueprints () {

      return LOGGING_BLUEPRINTS;
   }
}