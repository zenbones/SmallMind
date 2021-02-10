/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.forge.deploy;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.UserPrincipal;
import org.smallmind.nutsnbolts.io.FileUtility;
import org.smallmind.nutsnbolts.zip.CompressionType;

public class ApplicationUpdater {

  public static void update (OperatingSystem operatingSystem, String appUser, Path installPath, boolean progressBar, String nexusHost, String nexusUser, String nexusPassword, Repository repository, String groupId, String artifactId, String version, String classifier, String extension, String[] envVars, Decorator... decorators)
    throws Exception {

    final UserPrincipal owner = FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(appUser);
    Path applicationRootPath;
    Path applicationBinPath;
    Path applicationCntrlPath;
    Path zipPath;

    System.out.println("Downloading artifact(" + artifactId + ") from nexus...");
    NexusDownloader.download(zipPath = installPath.resolve(artifactId + ".zip"), nexusHost, nexusUser, nexusPassword, repository, groupId, artifactId, version, classifier, extension, progressBar);

    applicationRootPath = installPath.resolve(artifactId);
    applicationBinPath = applicationRootPath.resolve("bin");
    applicationCntrlPath = applicationBinPath.resolve(artifactId + operatingSystem.getBatchExtension());

    if (Files.isDirectory(applicationBinPath, LinkOption.NOFOLLOW_LINKS) && Files.isRegularFile(applicationCntrlPath)) {
      System.out.println("Shutting down current service installation...");
      new ProcessBuilder(applicationCntrlPath.toString(), "remove").inheritIO().start().waitFor();
    }

    if (Files.isDirectory(applicationRootPath, LinkOption.NOFOLLOW_LINKS)) {
      System.out.println("Removing current service installation...");
      FileUtility.deleteBuilder(applicationRootPath).build();
    } else {
      Files.createDirectories(installPath);
    }

    System.out.println("Exploding new service installation...");
    CompressionType.ZIP.explode(zipPath, installPath, (zipEntry) -> System.out.println("Expanding " + installPath.resolve(zipEntry.getName()).getFileName() + "..."));
    Files.deleteIfExists(zipPath);

    Files.createDirectories(applicationRootPath.resolve("log"));

    for (Decorator decorator : decorators) {
      System.out.println("Applying decorator(" + decorator.getClass().getSimpleName() + ")...");
      decorator.decorate(operatingSystem, appUser, installPath, nexusHost, nexusUser, nexusPassword, repository, groupId, artifactId, version, classifier, extension, envVars);
    }

    System.out.println("Setting user and permissions...");
    Files.walkFileTree(applicationRootPath, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile (Path file, BasicFileAttributes attrs)
        throws IOException {

        Files.setOwner(file, owner);

        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory (Path dir, IOException exc)
        throws IOException {

        if (exc != null) {
          throw exc;
        }

        Files.setOwner(dir, owner);

        return FileVisitResult.CONTINUE;
      }
    });

    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(applicationBinPath)) {
      for (Path filePath : directoryStream) {
        operatingSystem.makeExecutable(filePath);
      }
    }

    System.out.println("Installing new service...");
    new ProcessBuilder(applicationCntrlPath.toString(), "install").inheritIO().start().waitFor();
  }
}
