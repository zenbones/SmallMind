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
package org.smallmind.scribe.pen.fluentbit;

import org.smallmind.nutsnbolts.lang.FormattedIOException;

// TODO: The underlying org.msgpack.jackson.dataformat will be dependent on jackson 2.x for the foreseeable future

/**
 * Specialization of {@link FormattedIOException} raised when a TCP connection to the Fluent Bit input
 * cannot be established or is lost and all retry attempts have been exhausted.
 */
public class FluentBitConnectionException extends FormattedIOException {

  /**
   * Constructs a {@code FluentBitConnectionException} with a root cause and a printf-style diagnostic message.
   *
   * @param throwable the underlying I/O exception that caused the connection failure
   * @param message   a printf-style message template describing the failure context
   * @param args      arguments substituted into the message template
   */
  public FluentBitConnectionException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }
}
