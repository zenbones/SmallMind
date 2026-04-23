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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;

/**
 * Fluent builder that assembles the parameters for a recursive directory copy and executes it via {@link FileUtility}.
 */
public class CopyTreeConfigurationBuilder {

  private final Path source;
  private final Path destination;
  private PathFilter[] pathFilters;
  private FileAttribute<?>[] fileAttributes;
  private boolean includeSourceDirectory = false;

  /**
   * Constructs a builder for copying {@code source} to {@code destination}.
   *
   * @param source      root path or file to copy
   * @param destination destination directory or file path
   */
  public CopyTreeConfigurationBuilder (Path source, Path destination) {

    this.source = source;
    this.destination = destination;
  }

  /**
   * Specifies path filters to apply during the tree walk; a path is copied only when all filters accept it.
   *
   * @param pathFilters zero or more filters to apply
   * @return this builder for method chaining
   */
  public CopyTreeConfigurationBuilder filter (PathFilter... pathFilters) {

    this.pathFilters = pathFilters;

    return this;
  }

  /**
   * Specifies file attributes to apply when creating directories during the copy.
   *
   * @param fileAttributes attributes to set on newly created directories
   * @return this builder for method chaining
   */
  public CopyTreeConfigurationBuilder attributes (FileAttribute<?>... fileAttributes) {

    this.fileAttributes = fileAttributes;

    return this;
  }

  /**
   * Controls whether the source directory node is itself created under the destination.
   *
   * @param includeSourceDirectory {@code true} to reproduce the source directory name at the destination root
   * @return this builder for method chaining
   */
  public CopyTreeConfigurationBuilder includeSourceDirectory (boolean includeSourceDirectory) {

    this.includeSourceDirectory = includeSourceDirectory;

    return this;
  }

  /**
   * Performs the configured recursive copy operation.
   *
   * @throws IOException if walking the source tree or copying any entry fails
   */
  public void build ()
    throws IOException {

    FileUtility.copyTree(source, destination, includeSourceDirectory, pathFilters, fileAttributes);
  }
}
