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
 * JUL {@link LogRecord} subclass that simultaneously implements the scribe {@link RecordWrapper}
 * interface, allowing a single object to travel through both the JUL pipeline and scribe appenders
 * while carrying scribe-specific level, context, and parameter metadata.
 */
public class JDKRecordSubverter extends LogRecord implements RecordWrapper {

  private final JDKRecord jdkRecord;
  private final LoggerContext loggerContext;
  private final Level level;

  /**
   * Builds a JUL {@link LogRecord} whose level is translated from the scribe level, attaches the
   * throwable and message arguments, and wraps itself in an inner {@link JDKRecord} for scribe use.
   *
   * @param loggerName    the name of the originating logger
   * @param level         the scribe severity level for this event
   * @param loggerContext the captured caller context, or {@code null} if not available
   * @param throwable     the throwable to attach to the record, or {@code null}
   * @param message       the raw message template
   * @param args          arguments substituted into the message template
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
   * Returns the source class name from the logger context, satisfying JUL's caller-location contract.
   *
   * @return the caller class name from the context, or {@code null} if no context is present
   */
  public String getSourceClassName () {

    if (loggerContext != null) {
      return loggerContext.getClassName();
    }

    return null;
  }

  /**
   * Returns the source method name from the logger context, satisfying JUL's caller-location contract.
   *
   * @return the caller method name from the context, or {@code null} if no context is present
   */
  public String getSourceMethodName () {

    if (loggerContext != null) {
      return loggerContext.getMethodName();
    }

    return null;
  }

  /**
   * Returns the inner scribe {@link Record} view of this JUL record.
   *
   * @return the inner {@link JDKRecord} that wraps this object
   */
  public Record<LogRecord> getRecord () {

    return jdkRecord;
  }

  /**
   * Scribe {@link org.smallmind.scribe.pen.Record} view over the enclosing {@link JDKRecordSubverter},
   * exposing the JUL {@link LogRecord} as the native log entry and the scribe level and context as
   * first-class properties.
   */
  private class JDKRecord extends ParameterAwareRecord<LogRecord> {

    private final LogRecord logRecord;
    private final String threadName;

    /**
     * Builds the scribe record view, capturing the current thread name at construction time.
     *
     * @param logRecord the JUL record that this object wraps
     */
    public JDKRecord (LogRecord logRecord) {

      this.logRecord = logRecord;

      threadName = Thread.currentThread().getName();
    }

    /**
     * Returns the JUL {@link LogRecord} that this scribe view wraps.
     *
     * @return the native JUL log record
     */
    @Override
    public LogRecord getNativeLogEntry () {

      return logRecord;
    }

    /**
     * Returns the logger name stored in the underlying JUL record.
     *
     * @return the logger name
     */
    @Override
    public String getLoggerName () {

      return logRecord.getLoggerName();
    }

    /**
     * Returns the scribe severity level of the enclosing subverter, which may differ from the
     * JUL level due to translation.
     *
     * @return the scribe severity level
     */
    @Override
    public Level getLevel () {

      return level;
    }

    /**
     * Returns the throwable attached to the underlying JUL record.
     *
     * @return the throwable, or {@code null} if none was set
     */
    @Override
    public Throwable getThrown () {

      return logRecord.getThrown();
    }

    /**
     * Returns the raw (un-substituted) message string stored in the underlying JUL record.
     *
     * @return the message template text
     */
    @Override
    public String getMessage () {

      return logRecord.getMessage();
    }

    /**
     * Returns the {@link LoggerContext} captured when the enclosing subverter was constructed.
     *
     * @return the logger context, or {@code null} if none was provided
     */
    @Override
    public LoggerContext getLoggerContext () {

      return loggerContext;
    }

    /**
     * Returns the thread id stored in the underlying JUL record.
     *
     * @return the originating thread id
     */
    @Override
    public long getThreadID () {

      return logRecord.getThreadID();
    }

    /**
     * Returns the thread name captured from the running thread when this record view was constructed.
     *
     * @return the originating thread name
     */
    @Override
    public String getThreadName () {

      return threadName;
    }

    /**
     * Returns the sequence number assigned by the JUL framework to the underlying record.
     *
     * @return the JUL sequence number
     */
    @Override
    public long getSequenceNumber () {

      return logRecord.getSequenceNumber();
    }

    /**
     * Returns the creation timestamp stored in the underlying JUL record.
     *
     * @return epoch milliseconds recorded by JUL at record creation
     */
    @Override
    public long getMillis () {

      return logRecord.getMillis();
    }
  }
}
