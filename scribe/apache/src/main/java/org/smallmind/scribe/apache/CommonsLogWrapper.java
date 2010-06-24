package org.smallmind.scribe.apache;

import org.apache.commons.logging.Log;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;

public class CommonsLogWrapper implements Log {

   private String name;

   static {

      LoggerManager.addLoggingPackagePrefix("org.apache.commons.logging.");
   }

   public CommonsLogWrapper (String name) {

      this.name = name;
   }

   private Logger getLogger () {

      return LoggerManager.getLogger(name);
   }

   public boolean isDebugEnabled () {

      return getLogger().getLevel().noGreater(Level.DEBUG);
   }

   public boolean isErrorEnabled () {

      return getLogger().getLevel().noGreater(Level.ERROR);
   }

   public boolean isFatalEnabled () {

      return getLogger().getLevel().noGreater(Level.FATAL);
   }

   public boolean isInfoEnabled () {

      return getLogger().getLevel().noGreater(Level.INFO);
   }

   public boolean isTraceEnabled () {

      return getLogger().getLevel().noGreater(Level.TRACE);
   }

   public boolean isWarnEnabled () {

      return getLogger().getLevel().noGreater(Level.WARN);
   }

   public void trace (Object o) {

      getLogger().trace(o);
   }

   public void trace (Object o, Throwable throwable) {

      getLogger().trace(throwable, o);
   }

   public void debug (Object o) {

      getLogger().debug(o);
   }

   public void debug (Object o, Throwable throwable) {

      getLogger().debug(throwable, o);
   }

   public void info (Object o) {

      getLogger().info(o);
   }

   public void info (Object o, Throwable throwable) {

      getLogger().info(throwable, o);
   }

   public void warn (Object o) {

      getLogger().warn(o);
   }

   public void warn (Object o, Throwable throwable) {

      getLogger().warn(throwable, o);
   }

   public void error (Object o) {

      getLogger().error(o);
   }

   public void error (Object o, Throwable throwable) {

      getLogger().error(throwable, o);
   }

   public void fatal (Object o) {

      getLogger().fatal(o);
   }

   public void fatal (Object o, Throwable throwable) {

      getLogger().fatal(throwable, o);
   }
}