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
package org.smallmind.scribe.ink.indigenous;

import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContext;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.ParameterAwareRecord;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.SequenceGenerator;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

/**
 * Native record implementation used by the indigenous logger backend.
 * Captures message details, context, and sequence metadata for a log event.
 */
public class IndigenousRecord extends ParameterAwareRecord<IndigenousRecord> implements RecordWrapper<IndigenousRecord> {

  private final Level level;
  private final Throwable throwable;
  private final String loggerName;
  private final String message;
  private final String threadName;
  private final Object[] args;
  private final long millis;
  private final long threadId;
  private final long sequenceNumber;
  private LoggerContext loggerContext;

  /**
   * Constructs a new record representing a log event.
   *
   * @param loggerName name of the logger emitting the record
   * @param level      severity level
   * @param throwable  optional throwable to attach
   * @param message    message template
   * @param args       message arguments
   */
  public IndigenousRecord (String loggerName, Level level, Throwable throwable, String message, Object... args) {

    this.loggerName = loggerName;
    this.level = level;
    this.throwable = throwable;
    this.message = message;
    this.args = args;

    millis = System.currentTimeMillis();

    threadId = Thread.currentThread().getId();
    threadName = Thread.currentThread().getName();
    sequenceNumber = SequenceGenerator.next();
  }

  /**
   * Returns this record for compatibility with downstream handlers.
   *
   * @return this record instance
   */
  @Override
  public Record<IndigenousRecord> getRecord () {

    return this;
  }

  /**
   * Returns the native representation of the log entry.
   *
   * @return this record instance
   */
  @Override
  public IndigenousRecord getNativeLogEntry () {

    return this;
  }

  /**
   * Returns the logger name that produced the record.
   *
   * @return the logger name
   */
  @Override
  public String getLoggerName () {

    return loggerName;
  }

  /**
   * Returns the severity level for this record.
   *
   * @return the log level
   */
  @Override
  public Level getLevel () {

    return level;
  }

  /**
   * Returns the throwable attached to this record, if any.
   *
   * @return the throwable or {@code null}
   */
  @Override
  public Throwable getThrown () {

    return throwable;
  }

  /**
   * Returns the translated message string after applying arguments.
   *
   * @return the formatted message
   */
  @Override
  public String getMessage () {

    return MessageTranslator.translateMessage(message, args);
  }

  /**
   * Returns logger context data if populated.
   *
   * @return the logger context, or {@code null} if none was provided
   */
  @Override
  public LoggerContext getLoggerContext () {

    return loggerContext;
  }

  /**
   * Assigns context captured at log time.
   *
   * @param loggerContext context to associate with the record
   */
  public void setLoggerContext (LoggerContext loggerContext) {

    this.loggerContext = loggerContext;
  }

  /**
   * Returns the identifier of the thread that produced the record.
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

    return threadName;
  }

  /**
   * Returns the monotonically increasing sequence number assigned to the event.
   *
   * @return the sequence number
   */
  @Override
  public long getSequenceNumber () {

    return sequenceNumber;
  }

  /**
   * Returns the timestamp in milliseconds when the record was created.
   *
   * @return the epoch millis of the record
   */
  @Override
  public long getMillis () {

    return millis;
  }
}
