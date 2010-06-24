package org.smallmind.persistence.orm.sql.pool;

import java.io.IOException;
import java.io.Writer;
import org.smallmind.quorum.pool.ConnectionPoolManager;
import org.smallmind.scribe.pen.Level;

public class PooledLogWriter extends Writer {

   private Level level;

   public PooledLogWriter () {

      this(Level.INFO);
   }

   public PooledLogWriter (Level level) {

      this.level = level;
   }

   public void write (char[] cbuf, int off, int len)
      throws IOException {

      ConnectionPoolManager.log(level, new String(cbuf, off, len));
   }

   public void flush () {
   }

   public void close () {
   }
}
