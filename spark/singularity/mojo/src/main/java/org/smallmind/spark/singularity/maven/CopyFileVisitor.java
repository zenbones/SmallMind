/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class CopyFileVisitor extends SimpleFileVisitor<Path> {

  private final SingularityIndex singularityIndex;
  private final Path targetPath;
  private Path sourcePath;

  public CopyFileVisitor (SingularityIndex singularityIndex, Path targetPath) {

    this.singularityIndex = singularityIndex;
    this.targetPath = targetPath;
  }

  @Override
  public FileVisitResult preVisitDirectory (final Path dir, final BasicFileAttributes attrs)
    throws IOException {

    if (sourcePath == null) {
      sourcePath = dir;
    }

    Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));

    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile (final Path file, final BasicFileAttributes attrs)
    throws IOException {

    Path jarPath;

    Files.copy(file, targetPath.resolve(jarPath = sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    singularityIndex.addFileName(PathUtility.asResourceString(jarPath));

    return FileVisitResult.CONTINUE;
  }
}
