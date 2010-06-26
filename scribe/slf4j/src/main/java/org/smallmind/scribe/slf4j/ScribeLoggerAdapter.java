package org.smallmind.scribe.slf4j;

import org.slf4j.Marker;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.spi.LocationAwareLogger;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.Level;

public class ScribeLoggerAdapter extends MarkerIgnoringBase implements LocationAwareLogger {

   private org.smallmind.scribe.pen.Logger logger;

   public ScribeLoggerAdapter (org.smallmind.scribe.pen.Logger logger) {

      this.logger = logger;
   }

   public String getName () {

      return logger.getName();
   }

   public boolean isTraceEnabled () {

      return logger.getLevel().noGreater(Level.TRACE);
   }

   public void trace (String msg) {

      logger.trace(msg);
   }

   public void trace (String format, Object arg1) {

      logger.trace(format, arg1);
   }

   public void trace (String format, Object arg1, Object arg2) {

      logger.trace(format, arg1, arg2);
   }

   public void trace (String format, Object[] args) {

      logger.trace(format, args);
   }

   public void trace (String msg, Throwable throwable) {

      logger.trace(throwable, msg);
   }

   public boolean isDebugEnabled () {

      return logger.getLevel().noGreater(Level.DEBUG);
   }

   public void debug (String msg) {

      logger.debug(msg);
   }

   public void debug (String format, Object arg1) {

      logger.debug(format, arg1);
   }

   public void debug (String format, Object arg1, Object arg2) {

      logger.debug(format, arg1, arg2);
   }

   public void debug (String format, Object[] args) {

      logger.debug(format, args);
   }

   public void debug (String msg, Throwable throwable) {

      logger.debug(throwable, msg);
   }

   public void info (String msg) {

      logger.info(msg);
   }

   public void info (String format, Object arg1) {

      logger.info(format, arg1);
   }

   public void info (String format, Object arg1, Object arg2) {

      logger.info(format, arg1, arg2);
   }

   public void info (String format, Object[] args) {

      logger.info(format, args);
   }

   public void info (String msg, Throwable throwable) {

      logger.info(throwable, msg);
   }

   public boolean isWarnEnabled () {
      return logger.getLevel().noGreater(Level.WARN);
   }

   public void warn (String msg) {

      logger.warn(msg);
   }

   public void warn (String format, Object arg1) {

      logger.warn(format, arg1);
   }

   public void warn (String format, Object arg1, Object arg2) {

      logger.warn(format, arg1, arg2);
   }

   public void warn (String format, Object[] args) {

      logger.warn(format, args);
   }

   public void warn (String msg, Throwable throwable) {

      logger.warn(throwable, msg);
   }

   public boolean isErrorEnabled () {
      return logger.getLevel().noGreater(Level.ERROR);
   }

   public void error (String msg) {

      logger.error(msg);
   }

   public void error (String format, Object arg1) {

      logger.error(format, arg1);
   }

   public void error (String format, Object arg1, Object arg2) {

      logger.error(format, arg1, arg2);
   }

   public void error (String format, Object[] args) {

      logger.error(format, args);
   }

   public void error (String msg, Throwable throwable) {

      logger.error(throwable, msg);
   }

   public boolean isInfoEnabled () {

      return logger.getLevel().noGreater(Level.INFO);
   }

   public void log (Marker marker, String fqcn, int level, String msg, Object[] objects, Throwable throwable) {

      Level scribeLevel;

      switch (level) {
         case LocationAwareLogger.TRACE_INT:
            scribeLevel = Level.TRACE;
            break;
         case LocationAwareLogger.DEBUG_INT:
            scribeLevel = Level.DEBUG;
            break;
         case LocationAwareLogger.INFO_INT:
            scribeLevel = Level.INFO;
            break;
         case LocationAwareLogger.WARN_INT:
            scribeLevel = Level.WARN;
            break;
         case LocationAwareLogger.ERROR_INT:
            scribeLevel = Level.ERROR;
            break;
         default:
            throw new UnknownSwitchCaseException(String.valueOf(level));
      }

      logger.log(scribeLevel, throwable, msg, objects);
   }
}