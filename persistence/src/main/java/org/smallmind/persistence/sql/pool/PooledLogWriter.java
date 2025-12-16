/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.sql.pool;

import java.io.IOException;
import java.io.Writer;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * {@link Writer} implementation that routes output to the SmallMind logging system at a specified
 * {@link Level}. Used by pooled data sources for logging JDBC messages.
 */
public class PooledLogWriter extends Writer {

  private final Level level;

  /**
   * Creates a log writer that logs at {@link Level#INFO}.
   */
  public PooledLogWriter () {

    this(Level.INFO);
  }

  /**
   * Creates a log writer at the given log level.
   *
   * @param level logging level to use
   */
  public PooledLogWriter (Level level) {

    this.level = level;
  }

  /**
   * Writes characters by emitting a log entry.
   *
   * @param cbuf buffer
   * @param off  offset
   * @param len  length
   * @throws IOException never thrown; required by signature
   */
  public void write (char[] cbuf, int off, int len)
    throws IOException {

    LoggerManager.getLogger(PooledLogWriter.class).log(level, new String(cbuf, off, len));
  }

  /**
   * No-op flush since logging is immediate.
   */
  public void flush () {

  }

  /**
   * No-op close.
   */
  public void close () {

  }
}
