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
package org.smallmind.persistence.orm;

import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;

/**
 * Raised when an ORM operation fails at runtime.
 */
public class ORMOperationException extends FormattedRuntimeException {

  /**
   * Creates an ORM operation exception with no message.
   */
  public ORMOperationException () {

    super();
  }

  /**
   * Creates an ORM operation exception with a formatted message.
   *
   * @param message the message template
   * @param args    arguments referenced by the format specifiers in the message
   */
  public ORMOperationException (String message, Object... args) {

    super(message, args);
  }

  /**
   * Creates an ORM operation exception with a cause and formatted message.
   *
   * @param throwable the underlying cause
   * @param message   the message template
   * @param args      arguments referenced by the format specifiers in the message
   */
  public ORMOperationException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  /**
   * Creates an ORM operation exception with a cause.
   *
   * @param throwable the underlying cause
   */
  public ORMOperationException (Throwable throwable) {

    super(throwable);
  }
}
