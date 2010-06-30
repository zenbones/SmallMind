package org.smallmind.scribe.pen;

public class LevelFilter implements Filter {

   private Level level = Level.TRACE;

   public LevelFilter () {
   }

   public LevelFilter (Level level) {

      this.level = level;
   }

   public void setLevel (Level level) {

      this.level = level;
   }

   public boolean willLog (Record record) {

      return record.getLevel().atLeast(level);
   }
}
