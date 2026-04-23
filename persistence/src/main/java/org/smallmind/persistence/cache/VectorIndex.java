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
package org.smallmind.persistence.cache;

/**
 * Represents a single named index entry, including its field name, value, and optional alias, used
 * when composing a {@link VectorKey}.
 */
public class VectorIndex {

  private final String indexField;
  private final String indexAlias;
  private final Object indexValue;

  /**
   * Constructs an index entry with a field name, value, and alias.
   *
   * @param indexField field name on the durable that this index tracks
   * @param indexValue current value of the indexed field
   * @param indexAlias short alias substituted for the field name in the key; empty string means use the field name
   */
  public VectorIndex (String indexField, Object indexValue, String indexAlias) {

    this.indexField = indexField;
    this.indexValue = indexValue;
    this.indexAlias = indexAlias;
  }

  /**
   * Returns the durable field name this index tracks.
   *
   * @return index field name
   */
  public String getIndexField () {

    return indexField;
  }

  /**
   * Returns the value of the indexed field.
   *
   * @return index field value
   */
  public Object getIndexValue () {

    return indexValue;
  }

  /**
   * Returns the alias used in place of the field name when building the key string, or an empty
   * string if no alias was set.
   *
   * @return index alias, or empty string
   */
  public String getIndexAlias () {

    return indexAlias;
  }
}
