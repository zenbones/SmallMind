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
 * Native log-event record for the indigenous scribe backend that captures the originating thread id and name,
 * a global sequence number, and the creation timestamp at construction time, and resolves the formatted message
 * on demand via {@link MessageTranslator}.
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
   * Builds a record for a log event, capturing the current thread id and name, a global sequence number
   * from {@link SequenceGenerator}, and the current wall-clock time at construction.
   *
   * @param loggerName the name of the logger emitting this record
   * @param level      the severity level of the event
   * @param throwable  optional throwable associated with the event, or {@code null}
   * @param message    message template to be resolved by {@link MessageTranslator}
   * @param args       arguments substituted into the message template
   */
  public IndigenousRecord (String loggerName, Level level, Throwable throwable, String message, Object... args) {

    this.loggerName = loggerName;
    this.level = level;
    this.throwable = throwable;
    this.message = message;
    this.args = args;

    millis = System.currentTimeMillis();

    threadId = Thread.currentThread().threadId();
    threadName = Thread.currentThread().getName();
    sequenceNumber = SequenceGenerator.next();
  }

  /**
   * Returns the scribe {@link Record} view of this object; since this class is both record and wrapper,
   * the method returns {@code this}.
   *
   * @return this instance as a {@link Record}
   */
  @Override
  public Record<IndigenousRecord> getRecord () {

    return this;
  }

  /**
   * Returns the native log-entry object; for the indigenous backend this is the record itself.
   *
   * @return this instance as the native log entry
   */
  @Override
  public IndigenousRecord getNativeLogEntry () {

    return this;
  }

  /**
   * Returns the name of the logger that created this record.
   *
   * @return the logger name
   */
  @Override
  public String getLoggerName () {

    return loggerName;
  }

  /**
   * Returns the severity level assigned to this record at construction time.
   *
   * @return the severity level
   */
  @Override
  public Level getLevel () {

    return level;
  }

  /**
   * Returns the throwable associated with this record.
   *
   * @return the throwable, or {@code null} if none was provided
   */
  @Override
  public Throwable getThrown () {

    return throwable;
  }

  /**
   * Returns the fully formatted message by passing the template and arguments through
   * {@link MessageTranslator#translateMessage}.
   *
   * @return the resolved message string
   */
  @Override
  public String getMessage () {

    return MessageTranslator.translateMessage(message, args);
  }

  /**
   * Returns the logger context attached to this record, which may contain caller class and method
   * information if auto-fill was enabled.
   *
   * @return the associated {@link LoggerContext}, or {@code null} if none has been set
   */
  @Override
  public LoggerContext getLoggerContext () {

    return loggerContext;
  }

  /**
   * Associates a {@link LoggerContext} with this record; called by the adapter immediately after construction.
   *
   * @param loggerContext the context to attach to this record
   */
  public void setLoggerContext (LoggerContext loggerContext) {

    this.loggerContext = loggerContext;
  }

  /**
   * Returns the id of the thread that was executing when this record was constructed.
   *
   * @return the originating thread id
   */
  @Override
  public long getThreadID () {

    return threadId;
  }

  /**
   * Returns the name of the thread that was executing when this record was constructed.
   *
   * @return the originating thread name
   */
  @Override
  public String getThreadName () {

    return threadName;
  }

  /**
   * Returns the monotonically increasing sequence number assigned by {@link SequenceGenerator} at construction time.
   *
   * @return the global sequence number for this event
   */
  @Override
  public long getSequenceNumber () {

    return sequenceNumber;
  }

  /**
   * Returns the wall-clock time in epoch milliseconds recorded when this object was constructed.
   *
   * @return the creation timestamp in milliseconds since the epoch
   */
  @Override
  public long getMillis () {

    return millis;
  }
}
