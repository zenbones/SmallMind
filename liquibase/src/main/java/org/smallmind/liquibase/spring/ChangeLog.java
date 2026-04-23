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
 * Describes a single Liquibase change log file processed by {@link SpringLiquibase}.
 *
 * <p>A change log pairs an {@code input} location (the change log to read) with an optional
 * {@code output} location used when the active {@link Goal} produces a file artifact, such
 * as {@link Goal#GENERATE}. For goals that do not write a file ({@link Goal#PREVIEW},
 * {@link Goal#UPDATE}, {@link Goal#DOCUMENT}), the {@code output} field is ignored.</p>
 *
 * <p>The {@code input} path is interpreted according to the {@link Source} configured on
 * {@link SpringLiquibase}: a {@link Source#FILE} source resolves it relative to the user's
 * home directory, while a {@link Source#CLASSPATH} source resolves it through the class
 * loader.</p>
 */
public class ChangeLog {

  private String input;
  private String output;

  /**
   * Returns the source location of the change log to process.
   *
   * <p>The returned value is a path or classpath resource name whose exact interpretation
   * depends on the {@link Source} set on {@link SpringLiquibase}.</p>
   *
   * @return the input path or classpath location of the change log, or {@code null} if not set
   */
  public String getInput () {

    return input;
  }

  /**
   * Sets the source location of the change log to process.
   *
   * @param input path or classpath resource name of the change log to read;
   *              must not be {@code null} when a non-{@link Goal#NONE} goal is configured
   */
  public void setInput (String input) {

    this.input = input;
  }

  /**
   * Returns the destination path for generated output produced by this change log.
   *
   * <p>Only meaningful when the active goal writes a file artifact (e.g., {@link Goal#GENERATE}).
   * Returns {@code null} when no output path has been configured.</p>
   *
   * @return the output file path, or {@code null} if not set
   */
  public String getOutput () {

    return output;
  }

  /**
   * Sets the destination path for generated output produced by this change log.
   *
   * @param output filesystem path where the generated artifact should be written;
   *               ignored for goals that do not produce a file
   */
  public void setOutput (String output) {

    this.output = output;
  }
}
