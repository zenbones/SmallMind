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
package org.smallmind.spark.singularity.maven;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import org.smallmind.nutsnbolts.io.PathUtility;
import org.smallmind.spark.singularity.boot.SingularityIndex;

/**
 * {@link SimpleFileVisitor} used while walking the project's compiled {@code classes} directory. Every directory is
 * mirrored below the Singularity build root and every file is copied into it, with the file's relative path
 * simultaneously registered with a {@link SingularityIndex} so that the boot loader can serve it.
 * <p>The source root is captured on the first directory visit, which is assumed to be the top of the tree passed to
 * {@link Files#walkFileTree}.
 */
public class CopyFileVisitor extends SimpleFileVisitor<Path> {

  private final SingularityIndex singularityIndex;
  private final Path targetPath;
  private Path sourcePath;

  /**
   * Captures the index to update and the directory under which copied files should be placed.
   *
   * @param singularityIndex index that should learn about every file copied by this visitor
   * @param targetPath       destination root; subdirectories are created beneath it as the walk progresses
   */
  public CopyFileVisitor (SingularityIndex singularityIndex, Path targetPath) {

    this.singularityIndex = singularityIndex;
    this.targetPath = targetPath;
  }

  /**
   * Ensures the destination directory exists before files inside the source directory are visited. The first
   * directory encountered is remembered as the anchor for all relative path calculations.
   *
   * @param dir   the directory about to be visited
   * @param attrs filesystem attributes of {@code dir} (unused)
   * @return {@link FileVisitResult#CONTINUE} to keep walking
   * @throws IOException if the mirrored directory cannot be created
   */
  @Override
  public FileVisitResult preVisitDirectory (final Path dir, final BasicFileAttributes attrs)
    throws IOException {

    if (sourcePath == null) {
      sourcePath = dir;
    }

    Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));

    return FileVisitResult.CONTINUE;
  }

  /**
   * Copies a single file into the mirrored target directory and registers its resource-style path with the index.
   *
   * @param file  the file being visited
   * @param attrs filesystem attributes of {@code file} (unused)
   * @return {@link FileVisitResult#CONTINUE} to keep walking
   * @throws IOException if the file cannot be copied to its destination
   */
  @Override
  public FileVisitResult visitFile (final Path file, final BasicFileAttributes attrs)
    throws IOException {

    Path jarPath;

    Files.copy(file, targetPath.resolve(jarPath = sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    singularityIndex.addFileName(PathUtility.asResourceString(jarPath));

    return FileVisitResult.CONTINUE;
  }
}
