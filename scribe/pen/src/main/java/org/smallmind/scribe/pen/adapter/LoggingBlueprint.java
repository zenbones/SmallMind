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
package org.smallmind.scribe.pen.adapter;

import org.smallmind.scribe.pen.Record;

/**
 * Abstract SPI base class that concrete logging backends implement to integrate with the Scribe framework;
 * instances are discovered at runtime through {@link java.util.ServiceLoader} and vended to the rest of
 * the framework by {@link LoggingBlueprintFactory}.
 *
 * @param <N> the native record type produced by the backend
 */
public abstract class LoggingBlueprint<N> {

  /**
   * Creates and returns a {@link LoggerAdapter} that routes log calls to the backend under the given name.
   *
   * @param name the logger name used to look up or create the backend logger
   * @return a configured adapter wrapping the named backend logger
   */
  public abstract LoggerAdapter getLoggingAdapter (String name);

  /**
   * Constructs a backend-native {@link Record} representing an internal error condition, suitable for use
   * when the normal logging path itself has failed.
   *
   * @param loggerName the name of the logger originating the error record
   * @param throwable  the throwable to attach to the record
   * @param message    a printf-style message template describing the error
   * @param args       arguments substituted into the message template
   * @return a fully constructed backend record at error severity
   */
  public abstract Record<N> errorRecord (String loggerName, Throwable throwable, String message, Object... args);
}
