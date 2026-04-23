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
package org.smallmind.nutsnbolts.lang;

/**
 * {@link RuntimeException} that wraps a checked exception so it can cross boundaries that do not
 * declare checked exceptions, with a typed accessor for recovering the original exception.
 */
public class WrappedException extends RuntimeException {

  /**
   * Constructs a runtime wrapper around the given checked exception.
   *
   * @param exception the checked exception to wrap as the cause
   */
  public WrappedException (Exception exception) {

    super(exception);
  }

  /**
   * Returns the wrapped exception cast to the requested type.
   *
   * @param exceptionClass the class to which the wrapped cause should be cast
   * @param <E>            the expected exception type
   * @return the wrapped cause cast to {@code E}
   * @throws ClassCastException if the wrapped cause is not an instance of {@code exceptionClass}
   */
  public <E extends Exception> E convert (Class<E> exceptionClass) {

    return exceptionClass.cast(getCause());
  }
}
