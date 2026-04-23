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
package org.smallmind.nutsnbolts.reflection;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

/**
 * Thrown when ASM-based byte code inspection or proxy generation encounters an unrecoverable error.
 */
public class ByteCodeManipulationException extends FormattedRuntimeException {

  /**
   * Constructs an exception with no detail message.
   */
  public ByteCodeManipulationException () {

    super();
  }

  /**
   * Constructs an exception with a formatted detail message.
   *
   * @param message a {@link java.util.Formatter}-style format string describing the failure
   * @param args    arguments substituted into the format string
   */
  public ByteCodeManipulationException (String message, Object... args) {

    super(message, args);
  }

  /**
   * Constructs an exception with a formatted detail message and a cause.
   *
   * @param throwable the underlying failure that triggered this exception
   * @param message   a {@link java.util.Formatter}-style format string describing the failure
   * @param args      arguments substituted into the format string
   */
  public ByteCodeManipulationException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  /**
   * Constructs an exception that wraps an existing throwable with no additional message.
   *
   * @param throwable the underlying failure that triggered this exception
   */
  public ByteCodeManipulationException (Throwable throwable) {

    super(throwable);
  }
}
