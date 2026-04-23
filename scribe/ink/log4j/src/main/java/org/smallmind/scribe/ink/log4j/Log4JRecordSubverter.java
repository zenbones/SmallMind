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
package org.smallmind.scribe.ink.log4j;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.FormattedMessage;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContext;
import org.smallmind.scribe.pen.ParameterAwareRecord;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.SequenceGenerator;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

/**
 * Log4j2 {@link Log4jLogEvent} subclass that simultaneously implements the scribe {@link RecordWrapper}
 * interface, allowing a single object to be appended by Log4j2 appenders while carrying scribe-specific
 * level, context, and parameter metadata.
 */
public class Log4JRecordSubverter extends Log4jLogEvent implements RecordWrapper<LogEvent> {

  private final Log4JRecord log4jRecord;
  private final LoggerContext loggerContext;
  private final Level level;

  /**
   * Builds a Log4j2 {@link Log4jLogEvent} whose level is translated from the scribe level, wraps the
   * message and arguments in a {@link FormattedMessage}, and creates the inner {@link Log4JRecord} view.
   *
   * @param loggerName      the name of the originating logger
   * @param loggerClassName the fully-qualified class name of the logger
   * @param level           the scribe severity level for this event
   * @param loggerContext   the captured caller context, or {@code null} if not available
   * @param throwable       the throwable to attach to the record, or {@code null}
   * @param message         the raw message template
   * @param args            arguments substituted into the message template
   */
  public Log4JRecordSubverter (String loggerName, String loggerClassName, Level level, LoggerContext loggerContext, Throwable throwable, String message, Object... args) {

    super(loggerName, null, loggerClassName, Log4JLevelTranslator.getLog4JLevel(level), new FormattedMessage(message, args), null, throwable);

    this.level = level;
    this.loggerContext = loggerContext;

    log4jRecord = new Log4JRecord(this);
  }

  /**
   * Returns the inner scribe {@link Record} view of this Log4j2 event.
   *
   * @return the inner {@link Log4JRecord} that wraps this object
   */
  public Record<LogEvent> getRecord () {

    return log4jRecord;
  }

  /**
   * Scribe {@link org.smallmind.scribe.pen.Record} view over the enclosing {@link Log4JRecordSubverter},
   * capturing a global sequence number and the originating thread id at construction time.
   */
  private class Log4JRecord extends ParameterAwareRecord<LogEvent> {

    private final LogEvent logEvent;
    private final long threadId;
    private final long sequenceNumber;

    /**
     * Builds the scribe record view, capturing the current thread id and a global sequence number
     * from {@link SequenceGenerator} at construction time.
     *
     * @param logEvent the Log4j2 event that this object wraps
     */
    public Log4JRecord (LogEvent logEvent) {

      this.logEvent = logEvent;

      threadId = Thread.currentThread().getId();
      sequenceNumber = SequenceGenerator.next();
    }

    /**
     * Returns the Log4j2 {@link LogEvent} that this scribe view wraps.
     *
     * @return the native Log4j2 event
     */
    @Override
    public LogEvent getNativeLogEntry () {

      return logEvent;
    }

    /**
     * Returns the logger name stored in the underlying Log4j2 event.
     *
     * @return the logger name
     */
    @Override
    public String getLoggerName () {

      return logEvent.getLoggerName();
    }

    /**
     * Returns the scribe severity level of the enclosing subverter, which may differ from the
     * Log4j2 level due to translation.
     *
     * @return the scribe severity level
     */
    @Override
    public Level getLevel () {

      return level;
    }

    /**
     * Returns the throwable attached to the underlying Log4j2 event.
     *
     * @return the throwable, or {@code null} if none was set
     */
    @Override
    public Throwable getThrown () {

      return logEvent.getThrown();
    }

    /**
     * Returns the fully formatted message produced by the Log4j2 event's {@link FormattedMessage}.
     *
     * @return the formatted message text
     */
    @Override
    public String getMessage () {

      return logEvent.getMessage().getFormattedMessage();
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
     * Returns the id of the thread that was executing when this record view was constructed.
     *
     * @return the originating thread id
     */
    @Override
    public long getThreadID () {

      return threadId;
    }

    /**
     * Returns the thread name stored in the underlying Log4j2 event.
     *
     * @return the originating thread name
     */
    @Override
    public String getThreadName () {

      return logEvent.getThreadName();
    }

    /**
     * Returns the monotonically increasing sequence number assigned by {@link SequenceGenerator}
     * at construction time of this record view.
     *
     * @return the global sequence number for this event
     */
    @Override
    public long getSequenceNumber () {

      return sequenceNumber;
    }

    /**
     * Returns the creation timestamp stored in the underlying Log4j2 event.
     *
     * @return epoch milliseconds recorded by Log4j2 at event creation
     */
    @Override
    public long getMillis () {

      return logEvent.getTimeMillis();
    }
  }
}
