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

/**
 * Fluent builder that assembles the parameters for a recursive deletion and executes it via {@link FileUtility}.
 */
public class DeleteTreeConfigurationBuilder {

  private final Path target;
  private PathFilter[] pathFilters;
  private boolean includeTargetDirectory = true;
  private boolean throwErrorOnDirectoryNotEmpty = true;

  /**
   * Constructs a builder targeting the given path for deletion.
   *
   * @param target root directory or file to delete
   */
  public DeleteTreeConfigurationBuilder (Path target) {

    this.target = target;
  }

  /**
   * Specifies path filters to apply during the tree walk; a path is deleted only when all filters accept it.
   *
   * @param pathFilters zero or more filters to apply
   * @return this builder for method chaining
   */
  public DeleteTreeConfigurationBuilder filter (PathFilter... pathFilters) {

    this.pathFilters = pathFilters;

    return this;
  }

  /**
   * Controls whether the root target directory is itself removed after its contents are deleted.
   *
   * @param includeTargetDirectory {@code true} to delete the root; {@code false} to leave it in place
   * @return this builder for method chaining
   */
  public DeleteTreeConfigurationBuilder includeTargetDirectory (boolean includeTargetDirectory) {

    this.includeTargetDirectory = includeTargetDirectory;

    return this;
  }

  /**
   * Controls whether an attempt to delete a non-empty directory raises an error.
   *
   * @param throwErrorOnDirectoryNotEmpty {@code true} to propagate {@code DirectoryNotEmptyException}; {@code false} to silently skip
   * @return this builder for method chaining
   */
  public DeleteTreeConfigurationBuilder throwErrorOnDirectoryNotEmpty (boolean throwErrorOnDirectoryNotEmpty) {

    this.throwErrorOnDirectoryNotEmpty = throwErrorOnDirectoryNotEmpty;

    return this;
  }

  /**
   * Performs the configured recursive deletion.
   *
   * @throws IOException if walking the tree or deleting any entry fails
   */
  public void build ()
    throws IOException {

    FileUtility.deleteTree(target, includeTargetDirectory, throwErrorOnDirectoryNotEmpty, pathFilters);
  }
}
