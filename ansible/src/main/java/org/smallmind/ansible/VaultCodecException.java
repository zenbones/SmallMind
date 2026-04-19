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
package org.smallmind.ansible;

import org.smallmind.nutsnbolts.lang.FormattedException;

/**
 * Checked exception that signals a failure during Ansible vault encoding or decoding.
 *
 * <p>Covers structural problems such as an unrecognized header format, an unsupported cipher,
 * truncated or malformed encoded content, and low-level JCA failures.  Password-specific
 * failures are represented by the more specific subclass {@link VaultPasswordException}.
 *
 * <p>Extends {@link FormattedException} so callers can supply a {@link String#format}-style
 * message template and arguments without constructing the formatted string themselves.
 */
public class VaultCodecException extends FormattedException {

  /**
   * Creates an exception with a {@link String#format}-style message.
   *
   * @param message format string for the error message
   * @param args    arguments referenced by the format specifiers in {@code message}
   */
  public VaultCodecException (String message, Object... args) {

    super(message, args);
  }

  /**
   * Wraps an underlying JCA or I/O failure.
   *
   * @param throwable the root cause; its message is used as this exception's detail message
   */
  public VaultCodecException (Throwable throwable) {

    super(throwable);
  }
}
