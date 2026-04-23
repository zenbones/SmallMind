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
package org.smallmind.liquibase.spring;

/**
 * Enumerates the Liquibase actions that {@link SpringLiquibase} can execute during application startup.
 *
 * <p>The chosen goal determines what {@link SpringLiquibase#afterPropertiesSet()} does with each
 * configured {@link ChangeLog}. Only one goal applies per {@code SpringLiquibase} bean instance;
 * all change logs in that instance are processed with the same goal.</p>
 */
public enum Goal {

  /**
   * Perform no Liquibase operation.
   *
   * <p>Custom data types registered via {@link SpringLiquibase#setDataTypes} are still applied,
   * but no change log is read and no database interaction occurs.</p>
   */
  NONE,

  /**
   * Generate the SQL that would be executed without applying it to the database.
   *
   * <p>The SQL is written to the stream configured via {@link SpringLiquibase#setPreviewStream},
   * or to {@link System#out} if no stream has been set. The target database is contacted only
   * to determine its type; no DDL or DML is applied.</p>
   */
  PREVIEW,

  /**
   * Apply all pending change sets in each change log to the target database.
   *
   * <p>This is the standard production use case. Liquibase tracks which change sets have already
   * been applied via its {@code DATABASECHANGELOG} table and skips them on subsequent runs.</p>
   */
  UPDATE,

  /**
   * Reverse-engineer the current database schema into a Liquibase change log file.
   *
   * <p>One output file is produced per distinct catalog encountered across the configured
   * change logs. The file is written to the directory set via {@link SpringLiquibase#setOutputDir},
   * falling back to the system temporary directory when that property is blank or null.
   * The change set author is set to {@code "auto.generated"}.</p>
   */
  GENERATE,

  /**
   * Produce HTML database documentation for the supplied change log.
   *
   * <p>Documentation is written to the directory configured via {@link SpringLiquibase#setOutputDir},
   * or to the system temporary directory when that property is blank or null.</p>
   */
  DOCUMENT
}
