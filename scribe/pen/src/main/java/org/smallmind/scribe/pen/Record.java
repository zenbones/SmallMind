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
 * Abstraction of a log record with a native backing entry.
 *
 * @param <N> native record type
 */
public interface Record<N> extends Serializable {

  /**
   * Returns the native backing record.
   *
   * @return native log entry
   */
  N getNativeLogEntry ();

  /**
   * Returns the originating logger name.
   *
   * @return logger name
   */
  String getLoggerName ();

  /**
   * Returns the severity level for this record.
   *
   * @return log level
   */
  Level getLevel ();

  /**
   * Returns the throwable attached to this record, if any.
   *
   * @return throwable or {@code null}
   */
  Throwable getThrown ();

  /**
   * Returns the formatted message.
   *
   * @return message text
   */
  String getMessage ();

  /**
   * Returns contextual parameters captured for this record.
   *
   * @return array of parameters
   */
  Parameter[] getParameters ();

  /**
   * Returns the logger context captured when the record was created.
   *
   * @return logger context or {@code null}
   */
  LoggerContext getLoggerContext ();

  /**
   * Returns the originating thread id.
   *
   * @return thread id
   */
  long getThreadID ();

  /**
   * Returns the originating thread name.
   *
   * @return thread name
   */
  String getThreadName ();

  /**
   * Returns the monotonically increasing sequence number of the record.
   *
   * @return sequence number
   */
  long getSequenceNumber ();

  /**
   * Returns the creation time in epoch milliseconds.
   *
   * @return record timestamp
   */
  long getMillis ();
}
