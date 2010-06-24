package org.smallmind.scribe.ink.jdk;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.Level;

public class JDKLevelTranslator {

   public static Level getLevel (java.util.logging.Level level) {

      if (level == null) {
         return null;
      }
      else if (level.equals(java.util.logging.Level.ALL)) {
         return Level.TRACE;
      }
      else if (level.equals(java.util.logging.Level.FINEST)) {
         return Level.TRACE;
      }
      else if (level.equals(java.util.logging.Level.FINER)) {
         return Level.TRACE;
      }
      else if (level.equals(java.util.logging.Level.FINE)) {
         return Level.DEBUG;
      }
      else if (level.equals(java.util.logging.Level.CONFIG)) {
         return Level.INFO;
      }
      else if (level.equals(java.util.logging.Level.INFO)) {
         return Level.INFO;
      }
      else if (level.equals(java.util.logging.Level.WARNING)) {
         return Level.WARN;
      }
      else if (level.equals(java.util.logging.Level.SEVERE)) {
         return Level.ERROR;
      }
      else if (level.equals(java.util.logging.Level.OFF)) {
         return Level.OFF;
      }
      else {
         throw new UnknownSwitchCaseException(level.getLocalizedName());
      }
   }

   public static java.util.logging.Level getLog4JLevel (Level level) {

      switch (level) {
         case TRACE:
            return java.util.logging.Level.FINER;
         case DEBUG:
            return java.util.logging.Level.FINE;
         case INFO:
            return java.util.logging.Level.INFO;
         case WARN:
            return java.util.logging.Level.WARNING;
         case ERROR:
            return java.util.logging.Level.SEVERE;
         case FATAL:
            return java.util.logging.Level.SEVERE;
         case OFF:
            return java.util.logging.Level.OFF;
         default:
            throw new UnknownSwitchCaseException(level.name());
      }
   }
}