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
 * Mutable configuration for Throng client behavior such as null storage and index creation.
 */
public class ThrongOptions {

  private boolean storeNulls;
  private boolean createIndexes;
  private boolean includeCollation;

  private ThrongOptions () {

  }

  /**
   * Creates a new options instance.
   *
   * @param storeNulls       whether null values are persisted
   * @param createIndexes    whether indexes are created automatically
   * @param includeCollation whether index creation includes collation details
   */
  public ThrongOptions (boolean storeNulls, boolean createIndexes, boolean includeCollation) {

    this.storeNulls = storeNulls;
    this.createIndexes = createIndexes;
    this.includeCollation = includeCollation;
  }

  /**
   * @return {@code true} if null values should be persisted
   */
  public boolean isStoreNulls () {

    return storeNulls;
  }

  /**
   * Sets whether null values should be stored.
   *
   * @param storeNulls flag indicating null storage
   * @return this options instance for chaining
   */
  public ThrongOptions setStoreNulls (boolean storeNulls) {

    this.storeNulls = storeNulls;

    return this;
  }

  /**
   * @return {@code true} if indexes are created automatically for entities
   */
  public boolean isCreateIndexes () {

    return createIndexes;
  }

  /**
   * Sets automatic index creation behavior.
   *
   * @param createIndexes whether to create indexes
   * @return this options instance for chaining
   */
  public ThrongOptions setCreateIndexes (boolean createIndexes) {

    this.createIndexes = createIndexes;

    return this;
  }

  /**
   * @return {@code true} if collation information should be included when creating indexes
   */
  public boolean isIncludeCollation () {

    return includeCollation;
  }

  /**
   * Sets whether collation is included in index creation.
   *
   * @param includeCollation flag indicating inclusion of collation
   * @return this options instance for chaining
   */
  public ThrongOptions setIncludeCollation (boolean includeCollation) {

    this.includeCollation = includeCollation;

    return this;
  }
}
