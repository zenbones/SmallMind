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
package org.smallmind.memcached.cubby;

import org.smallmind.nutsnbolts.lang.FormattedException;

/**
 * Checked exception thrown when a Cubby memcached operation cannot be completed successfully.
 *
 * <p>Extends {@link FormattedException} to support printf-style formatted messages, making it
 * straightforward to include diagnostic context such as the failing host name or key in the
 * exception message without manual string concatenation.</p>
 *
 * <p>Common scenarios that produce this exception include: the client not yet started or already
 * stopped, no available connection for a key's target host, and server-side error responses.</p>
 */
public class CubbyOperationException extends FormattedException {

  /**
   * Constructs an exception with no detail message or cause.
   */
  public CubbyOperationException () {

  }

  /**
   * Constructs an exception with a formatted detail message.
   *
   * @param message a format string (as accepted by {@link FormattedException})
   * @param args    arguments substituted into the format string
   */
  public CubbyOperationException (String message, Object... args) {

    super(message, args);
  }

  /**
   * Constructs an exception with a cause and a formatted detail message.
   *
   * @param throwable the underlying cause of this exception
   * @param message   a format string (as accepted by {@link FormattedException})
   * @param args      arguments substituted into the format string
   */
  public CubbyOperationException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  /**
   * Constructs an exception that wraps another throwable without adding a new message.
   *
   * @param throwable the underlying cause of this exception
   */
  public CubbyOperationException (Throwable throwable) {

    super(throwable);
  }
}
