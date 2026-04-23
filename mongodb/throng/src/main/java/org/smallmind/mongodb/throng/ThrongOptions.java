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
package org.smallmind.mongodb.throng;

/**
 * Configures the runtime behaviour of a {@link ThrongClient}, controlling null storage, automatic index creation,
 * and whether collation settings are included when indexes are created.
 */
public class ThrongOptions {

  private boolean storeNulls;
  private boolean createIndexes;
  private boolean includeCollation;

  private ThrongOptions () {

  }

  /**
   * Constructs a fully initialised options instance.
   *
   * @param storeNulls       {@code true} to persist null property values as BSON null
   * @param createIndexes    {@code true} to create MongoDB indexes automatically during client initialisation
   * @param includeCollation {@code true} to apply collation settings when creating indexes
   */
  public ThrongOptions (boolean storeNulls, boolean createIndexes, boolean includeCollation) {

    this.storeNulls = storeNulls;
    this.createIndexes = createIndexes;
    this.includeCollation = includeCollation;
  }

  /**
   * Returns whether null property values are persisted as BSON null.
   *
   * @return {@code true} if null values are stored
   */
  public boolean isStoreNulls () {

    return storeNulls;
  }

  /**
   * Sets whether null property values should be persisted.
   *
   * @param storeNulls {@code true} to store nulls
   * @return this instance for method chaining
   */
  public ThrongOptions setStoreNulls (boolean storeNulls) {

    this.storeNulls = storeNulls;

    return this;
  }

  /**
   * Returns whether MongoDB indexes are created automatically during {@link ThrongClient} initialisation.
   *
   * @return {@code true} if automatic index creation is enabled
   */
  public boolean isCreateIndexes () {

    return createIndexes;
  }

  /**
   * Sets whether indexes are created automatically during client initialisation.
   *
   * @param createIndexes {@code true} to enable automatic index creation
   * @return this instance for method chaining
   */
  public ThrongOptions setCreateIndexes (boolean createIndexes) {

    this.createIndexes = createIndexes;

    return this;
  }

  /**
   * Returns whether collation settings are included when indexes are created.
   *
   * @return {@code true} if collation is applied to index creation
   */
  public boolean isIncludeCollation () {

    return includeCollation;
  }

  /**
   * Sets whether collation settings are included when creating indexes.
   *
   * @param includeCollation {@code true} to include collation
   * @return this instance for method chaining
   */
  public ThrongOptions setIncludeCollation (boolean includeCollation) {

    this.includeCollation = includeCollation;

    return this;
  }
}
