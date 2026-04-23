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
package org.smallmind.persistence;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

/**
 * Thrown when a lookup by identifier finds no matching record in the persistence store.
 */
public class IdentifierNotFoundException extends FormattedRuntimeException {

  /**
   * Creates an {@code IdentifierNotFoundException} with no detail message.
   */
  public IdentifierNotFoundException () {

    super();
  }

  /**
   * Creates an {@code IdentifierNotFoundException} with a {@link String#format}-style message.
   *
   * @param message format string for the detail message
   * @param args    arguments substituted into {@code message}
   */
  public IdentifierNotFoundException (String message, Object... args) {

    super(message, args);
  }

  /**
   * Creates an {@code IdentifierNotFoundException} with a cause and a {@link String#format}-style message.
   *
   * @param throwable the underlying cause
   * @param message   format string for the detail message
   * @param args      arguments substituted into {@code message}
   */
  public IdentifierNotFoundException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  /**
   * Creates an {@code IdentifierNotFoundException} wrapping an existing throwable.
   *
   * @param throwable the underlying cause
   */
  public IdentifierNotFoundException (Throwable throwable) {

    super(throwable);
  }
}
