package org.smallmind.scribe.ink.log4j;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.Level;

public class Log4JLevelTranslator {

   public static Level getLevel (org.apache.log4j.Level level) {

      if (level == null) {
         return null;
      }
      else if (level.equals(org.apache.log4j.Level.TRACE)) {
         return Level.TRACE;
      }
      else if (level.equals(org.apache.log4j.Level.DEBUG)) {
         return Level.DEBUG;
      }
      else if (level.equals(org.apache.log4j.Level.INFO)) {
         return Level.INFO;
      }
      else if (level.equals(org.apache.log4j.Level.WARN)) {
         return Level.WARN;
      }
      else if (level.equals(org.apache.log4j.Level.ERROR)) {
         return Level.ERROR;
      }
      else if (level.equals(org.apache.log4j.Level.FATAL)) {
         return Level.FATAL;
      }
      else if (level.equals(org.apache.log4j.Level.OFF)) {
         return Level.OFF;
      }
      else {
         throw new UnknownSwitchCaseException(String.valueOf(level.toInt()));
      }
   }

   public static org.apache.log4j.Level getLog4JLevel (Level level) {

      switch (level) {
         case TRACE:
            return org.apache.log4j.Level.TRACE;
         case DEBUG:
            return org.apache.log4j.Level.DEBUG;
         case INFO:
            return org.apache.log4j.Level.INFO;
         case WARN:
            return org.apache.log4j.Level.WARN;
         case ERROR:
            return org.apache.log4j.Level.ERROR;
         case FATAL:
            return org.apache.log4j.Level.FATAL;
         case OFF:
            return org.apache.log4j.Level.OFF;
         default:
            throw new UnknownSwitchCaseException(level.name());
      }
   }
}
