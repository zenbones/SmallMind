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
package org.smallmind.nutsnbolts.util;

import java.io.Serial;

/**
 * Singleton empty {@link Option} that represents the absence of a value.
 *
 * @param <T> element type
 */
public class None<T> implements Option<T> {

  private static final None<?> NONE = new None<>();

  private None () {

  }

  /**
   * Returns the singleton instance typed to the caller's element type. The cast is unchecked but safe
   * because this option never holds a value of type {@code T}.
   *
   * @param <T> element type
   * @return the shared {@link None} instance
   */
  public static <T> None<T> none () {

    return (None<T>)NONE;
  }

  /**
   * Preserves singleton identity across deserialization.
   *
   * @return the shared {@link None} instance
   */
  @Serial
  private Object readResolve () {

    return NONE;
  }

  /**
   * Always returns {@code true} because this is the absent case.
   *
   * @return {@code true}
   */
  @Override
  public boolean isNone () {

    return true;
  }

  /**
   * Always returns {@code null}, as this option carries no value.
   *
   * @return {@code null}
   */
  @Override
  public T get () {

    return null;
  }
}
