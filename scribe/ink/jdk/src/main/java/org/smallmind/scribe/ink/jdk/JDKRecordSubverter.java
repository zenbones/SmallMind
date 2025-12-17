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
package org.smallmind.scribe.ink.jdk;

import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContext;
import org.smallmind.scribe.pen.ParameterAwareRecord;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

/**
 * A JUL {@link LogRecord} that also implements the scribe {@link RecordWrapper} interface.
 * Subverts the JUL record to carry scribe metadata and contextual information.
 */
public class JDKRecordSubverter extends LogRecord implements RecordWrapper {

  private final JDKRecord jdkRecord;
  private final LoggerContext loggerContext;
  private final Level level;

  /**
   * Constructs a subverted JUL record with scribe metadata.
   *
   * @param loggerName    originating logger name
   * @param level         scribe level for the event
   * @param loggerContext captured context, possibly {@code null}
   * @param throwable     throwable to attach
   * @param message       message template
   * @param args          message arguments
   */
  public JDKRecordSubverter (String loggerName, Level level, LoggerContext loggerContext, Throwable throwable, String message, Object... args) {

    super(JDKLevelTranslator.getJDKLevel(level), message);

    setLoggerName(loggerName);
    setThrown(throwable);
    setParameters(args);

    this.level = level;
    this.loggerContext = loggerContext;

    jdkRecord = new JDKRecord(this);
  }

  /**
   * Returns the class name captured in the logger context, if available.
   *
   * @return the source class name or {@code null}
   */
  public String getSourceClassName () {

    if (loggerContext != null) {
      return loggerContext.getClassName();
    }

    return null;
  }

  /**
   * Returns the method name captured in the logger context, if available.
   *
   * @return the source method name or {@code null}
   */
  public String getSourceMethodName () {

    if (loggerContext != null) {
      return loggerContext.getMethodName();
    }

    return null;
  }

  /**
   * Returns the scribe record wrapper for this JUL record.
   *
   * @return the wrapped record
   */
  public Record<LogRecord> getRecord () {

    return jdkRecord;
  }

  /**
   * Scribe record view over the subverted JUL record.
   */
  private class JDKRecord extends ParameterAwareRecord<LogRecord> {

    private final LogRecord logRecord;
    private final String threadName;

    /**
     * Creates a record view around the JUL record.
     *
     * @param logRecord JUL record to expose
     */
    public JDKRecord (LogRecord logRecord) {

      this.logRecord = logRecord;

      threadName = Thread.currentThread().getName();
    }

    /**
     * Returns the underlying JUL record.
     *
     * @return the native log record
     */
    @Override
    public LogRecord getNativeLogEntry () {

      return logRecord;
    }

    /**
     * Returns the logger name from the JUL record.
     *
     * @return the logger name
     */
    @Override
    public String getLoggerName () {

      return logRecord.getLoggerName();
    }

    /**
     * Returns the scribe level associated with the record.
     *
     * @return the level
     */
    @Override
    public Level getLevel () {

      return level;
    }

    /**
     * Returns the throwable attached to the JUL record.
     *
     * @return the throwable, or {@code null}
     */
    @Override
    public Throwable getThrown () {

      return logRecord.getThrown();
    }

    /**
     * Returns the raw message from the JUL record.
     *
     * @return the message text
     */
    @Override
    public String getMessage () {

      return logRecord.getMessage();
    }

    /**
     * Returns the captured logger context.
     *
     * @return context information, possibly {@code null}
     */
    @Override
    public LoggerContext getLoggerContext () {

      return loggerContext;
    }

    /**
     * Returns the thread id recorded by JUL.
     *
     * @return the thread id
     */
    @Override
    public long getThreadID () {

      return logRecord.getThreadID();
    }

    /**
     * Returns the thread name recorded when the record was created.
     *
     * @return the thread name
     */
    @Override
    public String getThreadName () {

      return threadName;
    }

    /**
     * Returns the sequence number assigned by JUL.
     *
     * @return the sequence number
     */
    @Override
    public long getSequenceNumber () {

      return logRecord.getSequenceNumber();
    }

    /**
     * Returns the timestamp of the record.
     *
     * @return epoch milliseconds
     */
    @Override
    public long getMillis () {

      return logRecord.getMillis();
    }
  }
}
