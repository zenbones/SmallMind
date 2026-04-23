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
package org.smallmind.scribe.pen;

import java.io.Serializable;

/**
 * Serializable envelope for a single logging event, carrying the message, severity level, call-site context,
 * thread identity, timing information, and an optional native backing entry of type {@code N}.
 *
 * @param <N> the type of the underlying native log-entry object wrapped by this record
 */
public interface Record<N> extends Serializable {

  /**
   * Returns the underlying native log-entry object that this record wraps, if any.
   *
   * @return the native log entry, or {@code null} if there is no native backing object
   */
  N getNativeLogEntry ();

  /**
   * Returns the name of the logger that produced this record.
   *
   * @return the logger name; never {@code null}
   */
  String getLoggerName ();

  /**
   * Returns the severity level assigned to this record.
   *
   * @return the {@link Level} of this record; never {@code null}
   */
  Level getLevel ();

  /**
   * Returns the throwable that was attached to this record at creation time, if any.
   *
   * @return the associated {@link Throwable}, or {@code null} if no exception was logged
   */
  Throwable getThrown ();

  /**
   * Returns the formatted log message for this record.
   *
   * @return the message text; may be empty but never {@code null}
   */
  String getMessage ();

  /**
   * Returns the array of contextual key/value parameters carried by this record.
   *
   * @return the parameters array; never {@code null}, but may be empty
   */
  Parameter[] getParameters ();

  /**
   * Returns the call-site context that identifies where in the source code this record was created.
   *
   * @return the {@link LoggerContext}, or {@code null} if context capture was not performed
   */
  LoggerContext getLoggerContext ();

  /**
   * Returns the identifier of the thread that created this record.
   *
   * @return the thread ID
   */
  long getThreadID ();

  /**
   * Returns the name of the thread that created this record.
   *
   * @return the thread name; may be {@code null} if the thread had no name
   */
  String getThreadName ();

  /**
   * Returns the monotonically increasing sequence number assigned to this record within the logging session.
   *
   * @return the sequence number
   */
  long getSequenceNumber ();

  /**
   * Returns the wall-clock time at which this record was created, expressed as milliseconds since the Unix epoch.
   *
   * @return the creation timestamp in epoch milliseconds
   */
  long getMillis ();
}
