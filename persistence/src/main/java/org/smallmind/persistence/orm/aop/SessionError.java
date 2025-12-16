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
package org.smallmind.persistence.orm.aop;

import org.smallmind.nutsnbolts.lang.FormattedError;

/**
 * Base error type for session boundary failures, tracking how many boundaries remain to close.
 */
public abstract class SessionError extends FormattedError {

  private final int closure;

  /**
   * Creates a session error with no message.
   *
   * @param closure number of remaining boundaries to close
   */
  public SessionError (int closure) {

    super();

    this.closure = closure;
  }

  /**
   * Creates a session error with a formatted message.
   *
   * @param closure number of remaining boundaries to close
   * @param message error message format
   * @param args    message arguments
   */
  public SessionError (int closure, String message, Object... args) {

    super(message, args);

    this.closure = closure;
  }

  /**
   * Creates a session error wrapping another throwable with a formatted message.
   *
   * @param closure   number of remaining boundaries to close
   * @param throwable underlying cause
   * @param message   error message format
   * @param args      message arguments
   */
  public SessionError (int closure, Throwable throwable, String message, Object... args) {

    super(throwable, message, args);

    this.closure = closure;
  }

  /**
   * Creates a session error wrapping another throwable.
   *
   * @param closure   number of remaining boundaries to close
   * @param throwable underlying cause
   */
  public SessionError (int closure, Throwable throwable) {

    super(throwable);

    this.closure = closure;
  }

  /**
   * @return the remaining boundary depth expected when this error was raised
   */
  public int getClosure () {

    return closure;
  }
}
