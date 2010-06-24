package org.smallmind.scribe.pen.adapter;

import java.util.Iterator;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;
import sun.misc.Service;

public class LoggingBlueprintsFactory {

   private static LoggingBlueprints LOGGING_BLUEPRINTS;

   static {

      Iterator providerIter;

      providerIter = Service.providers(LoggingBlueprints.class, Thread.currentThread().getContextClassLoader());
      if (!providerIter.hasNext()) {
         throw new StaticInitializationError("No provider found for LoggingBlueprints");
      }

      LOGGING_BLUEPRINTS = (LoggingBlueprints)providerIter.next();
   }

   public static LoggingBlueprints getLoggingBlueprints () {

      return LOGGING_BLUEPRINTS;
   }
}