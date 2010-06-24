package org.smallmind.scribe.pen;

public enum Level {

   TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF;

   public boolean atLeast (Level level) {

      return this.ordinal() >= level.ordinal();
   }

   public boolean noGreater (Level level) {

      return this.ordinal() <= level.ordinal();
   }
}
