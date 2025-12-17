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
 * A Log4j2 {@link LogEvent} that also implements the scribe {@link RecordWrapper} interface,
 * allowing scribe metadata to be attached to native Log4j2 events.
 */
public class Log4JRecordSubverter extends Log4jLogEvent implements RecordWrapper<LogEvent> {

  private final Log4JRecord log4jRecord;
  private final LoggerContext loggerContext;
  private final Level level;

  /**
   * Constructs a subverted Log4j2 event with scribe metadata.
   *
   * @param loggerName      originating logger name
   * @param loggerClassName class name of the logger
   * @param level           scribe level for the event
   * @param loggerContext   captured context, possibly {@code null}
   * @param throwable       throwable to attach
   * @param message         message template
   * @param args            message arguments
   */
  public Log4JRecordSubverter (String loggerName, String loggerClassName, Level level, LoggerContext loggerContext, Throwable throwable, String message, Object... args) {

    super(loggerName, null, loggerClassName, Log4JLevelTranslator.getLog4JLevel(level), new FormattedMessage(message, args), null, throwable);

    this.level = level;
    this.loggerContext = loggerContext;

    log4jRecord = new Log4JRecord(this);
  }

  /**
   * Returns the scribe record wrapper for this Log4j2 event.
   *
   * @return the wrapped record
   */
  public Record<LogEvent> getRecord () {

    return log4jRecord;
  }

  /**
   * Scribe record view over the subverted Log4j2 event.
   */
  private class Log4JRecord extends ParameterAwareRecord<LogEvent> {

    private final LogEvent logEvent;
    private final long threadId;
    private final long sequenceNumber;

    /**
     * Creates a record view around the Log4j2 event.
     *
     * @param logEvent Log4j2 event to expose
     */
    public Log4JRecord (LogEvent logEvent) {

      this.logEvent = logEvent;

      threadId = Thread.currentThread().getId();
      sequenceNumber = SequenceGenerator.next();
    }

    /**
     * Returns the underlying Log4j2 event.
     *
     * @return the native event
     */
    @Override
    public LogEvent getNativeLogEntry () {

      return logEvent;
    }

    /**
     * Returns the logger name from the event.
     *
     * @return the logger name
     */
    @Override
    public String getLoggerName () {

      return logEvent.getLoggerName();
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
     * Returns the throwable attached to the event, if any.
     *
     * @return the throwable, or {@code null}
     */
    @Override
    public Throwable getThrown () {

      return logEvent.getThrown();
    }

    /**
     * Returns the formatted message from the event.
     *
     * @return the formatted message text
     */
    @Override
    public String getMessage () {

      return logEvent.getMessage().getFormattedMessage();
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
     * Returns the id of the thread that produced the record.
     *
     * @return the thread id
     */
    @Override
    public long getThreadID () {

      return threadId;
    }

    /**
     * Returns the name of the thread that produced the record.
     *
     * @return the thread name
     */
    @Override
    public String getThreadName () {

      return logEvent.getThreadName();
    }

    /**
     * Returns a sequence number assigned by the scribe generator.
     *
     * @return the sequence number
     */
    @Override
    public long getSequenceNumber () {

      return sequenceNumber;
    }

    /**
     * Returns the timestamp of the event.
     *
     * @return epoch milliseconds
     */
    @Override
    public long getMillis () {

      return logEvent.getTimeMillis();
    }
  }
}
