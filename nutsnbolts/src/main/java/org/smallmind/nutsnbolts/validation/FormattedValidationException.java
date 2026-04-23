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
package org.smallmind.nutsnbolts.validation;

import jakarta.validation.ValidationException;

/**
 * {@link ValidationException} subclass whose constructors accept {@link String#format(String, Object...)} style message patterns.
 */
public class FormattedValidationException extends ValidationException {

  /**
   * Creates an exception with no detail message.
   */
  public FormattedValidationException () {

  }

  /**
   * Creates an exception whose detail message is produced by formatting the pattern with the supplied arguments.
   *
   * @param message the {@link String#format} pattern; may be {@code null}
   * @param args    the arguments to substitute into the pattern
   */
  public FormattedValidationException (String message, Object... args) {

    super(message == null ? null : String.format(message, args));
  }

  /**
   * Creates an exception with a formatted detail message and an underlying cause.
   *
   * @param cause   the underlying cause
   * @param message the {@link String#format} pattern; may be {@code null}
   * @param args    the arguments to substitute into the pattern
   */
  public FormattedValidationException (Throwable cause, String message, Object... args) {

    super(message == null ? null : String.format(message, args), cause);
  }

  /**
   * Creates an exception that wraps the supplied cause without an additional message.
   *
   * @param cause the underlying cause
   */
  public FormattedValidationException (Throwable cause) {

    super(cause);
  }
}
