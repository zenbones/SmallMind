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
package org.smallmind.mongodb.throng.index;

import org.smallmind.mongodb.throng.index.annotation.Indexed;

/**
 * Captures a field annotated for indexing along with its {@link Indexed} metadata.
 */
public class IndexedField {

  private final Indexed indexed;
  private final String field;

  /**
   * @param field   the field path designated for indexing
   * @param indexed annotation describing index parameters
   */
  public IndexedField (String field, Indexed indexed) {

    this.field = field;
    this.indexed = indexed;
  }

  /**
   * Prefixes the field path with the given prolog.
   *
   * @param prolog prefix to add to the field path
   * @return new {@link IndexedField} with adjusted path
   */
  public IndexedField accumulate (String prolog) {

    return new IndexedField((prolog == null || prolog.isEmpty()) ? field : prolog + "." + field, indexed);
  }

  /**
   * @return the field path marked for indexing
   */
  public String getField () {

    return field;
  }

  /**
   * @return the {@link Indexed} annotation associated with the field
   */
  public Indexed getIndexed () {

    return indexed;
  }
}
