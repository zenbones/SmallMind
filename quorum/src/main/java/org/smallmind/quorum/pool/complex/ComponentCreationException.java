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
package org.smallmind.quorum.pool.complex;

import org.smallmind.quorum.pool.ComponentPoolException;

/**
 * Signals a failure that occurred while attempting to create a new pooled component instance.
 * <p>
 * Thrown by {@link ComponentPinManager} when the underlying factory call fails, when a
 * creation timeout expires, or when an in-progress creation worker is aborted. Extends
 * {@link ComponentPoolException} so callers that catch the base class will also handle this
 * subclass.
 */
public class ComponentCreationException extends ComponentPoolException {

  /**
   * Creates the exception with no message or cause.
   */
  public ComponentCreationException () {

    super();
  }

  /**
   * Creates the exception with a formatted message and no cause.
   *
   * @param message a {@link String#format}-style template
   * @param args    arguments substituted into the template
   */
  public ComponentCreationException (String message, Object... args) {

    super(message, args);
  }

  /**
   * Creates the exception with both a cause and a formatted message.
   *
   * @param throwable the underlying cause
   * @param message   a {@link String#format}-style template
   * @param args      arguments substituted into the template
   */
  public ComponentCreationException (Throwable throwable, String message, Object... args) {

    super(throwable, message, args);
  }

  /**
   * Creates the exception wrapping an existing cause with no additional message.
   *
   * @param throwable the underlying cause
   */
  public ComponentCreationException (Throwable throwable) {

    super(throwable);
  }
}
