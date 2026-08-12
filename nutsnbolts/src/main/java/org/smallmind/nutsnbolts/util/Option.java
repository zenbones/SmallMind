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

import java.io.Serializable;

/**
 * Lightweight optional value container with concrete {@link Some} (present) and {@link None} (absent) implementations.
 *
 * <p>Presence and emptiness are distinct notions here. {@link #isNone()} asks which of the two cases this
 * option is, while {@link #isEmpty()} asks whether the wrapped value is {@code null}. Because {@link Some}
 * accepts a {@code null} value, an option may be present yet empty.
 *
 * @param <T> element type
 */
public interface Option<T> extends Serializable {

  /**
   * Returns the shared singleton representing the absence of a value.
   *
   * @param <T> element type
   * @return the shared {@link None} instance
   */
  static <T> None<T> none () {

    return None.none();
  }

  /**
   * Wraps the supplied value in a {@link Some}.
   *
   * @param value value to wrap; may be {@code null}, in which case the result is present but {@link #isEmpty() empty}
   * @param <T>   element type
   * @return a new {@link Some} containing {@code value}
   */
  static <T> Some<T> of (T value) {

    return new Some<>(value);
  }

  /**
   * Returns {@code true} if the wrapped value is {@code null}. This always holds for {@link None}, but also
   * for a {@link Some} constructed around a {@code null} value, so an empty option is not necessarily the
   * absent case.
   *
   * @return {@code true} when {@link #get()} would return {@code null}
   */
  default boolean isEmpty () {

    return get() == null;
  }

  /**
   * Returns {@code true} if this option is the absent case, that is, the {@link None} singleton. A
   * {@link Some} wrapping a {@code null} value returns {@code false} here even though it is
   * {@link #isEmpty() empty}.
   *
   * @return {@code true} when the option is absent
   */
  boolean isNone ();

  /**
   * Returns the contained value. This is always {@code null} for {@link None}, and may also be {@code null}
   * for a {@link Some} constructed around a {@code null} value.
   *
   * @return the wrapped value, which may be {@code null}
   */
  T get ();
}
