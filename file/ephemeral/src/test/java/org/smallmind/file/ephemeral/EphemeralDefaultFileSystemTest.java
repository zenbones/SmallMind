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
package org.smallmind.file.ephemeral;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Verifies that the provider really can be installed as the JVM default file system through
 * {@code -Djava.nio.file.spi.DefaultFileSystemProvider}.
 *
 * <p>Installing a default file system is a decision the JVM makes once, while it is starting, so it
 * cannot be exercised from inside a running test. Each case here therefore forks a JVM with the
 * property set and inspects what that JVM reports about itself.
 *
 * <p>What is being checked is the awkward part of the arrangement: the platform reads its own
 * configuration through {@link Files} while it is starting up, so an overlay that answers for every
 * path leaves it unable to start. A configuration that carves out an overlay root has to keep the
 * real file system reachable, and a configuration that swallows everything has to be refused in
 * terms that name the problem.
 */
@Test(groups = "unit")
public class EphemeralDefaultFileSystemTest {

  private static final String OVERLAY_ROOT = "/overlay";

  /**
   * Forks a JVM with this provider installed as the default file system.
   *
   * @param roots the value for the roots property, or {@code null} to leave it unset
   * @return the combined output of the forked JVM
   * @throws IOException          if the JVM cannot be started
   * @throws InterruptedException if waiting for it is interrupted
   */
  private String fork (String roots)
    throws IOException, InterruptedException {

    Path javaCommand = Paths.get(System.getProperty("java.home"), "bin", System.getProperty("os.name").toLowerCase().startsWith("windows") ? "java.exe" : "java");

    if (!Files.isRegularFile(javaCommand)) {
      throw new SkipException("No java executable was found at " + javaCommand);
    }

    List<String> command = new LinkedList<>();

    command.add(javaCommand.toString());
    command.add("-Djava.nio.file.spi.DefaultFileSystemProvider=" + EphemeralFileSystemProvider.class.getName());
    if (roots != null) {
      command.add("-Dorg.smallmind.file.ephemeral.configuration.roots=" + roots);
    }
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add(Harness.class.getName());

    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    Assert.assertTrue(process.waitFor(2, TimeUnit.MINUTES), "The forked JVM did not finish");

    return output;
  }

  public void testInstallsAsTheDefaultFileSystemWithAnOverlayRoot ()
    throws IOException, InterruptedException {

    String output = fork(OVERLAY_ROOT);

    Assert.assertTrue(output.contains("defaultFileSystem=" + EphemeralFileSystem.class.getName()), output);
    Assert.assertTrue(output.contains("scheme=file"), output);
  }

  public void testTheOverlayRootIsServedFromTheHeap ()
    throws IOException, InterruptedException {

    String output = fork(OVERLAY_ROOT);

    Assert.assertTrue(output.contains("overlayPathKind=EphemeralPath"), output);
    Assert.assertTrue(output.contains("overlayRoundTrip=in-memory"), output);
    Assert.assertTrue(output.contains("overlayAbsentFromDisk=true"), output);
  }

  /**
   * The case that made the whole arrangement unusable on Windows: a native absolute path was taken
   * for a relative one and claimed by the heap, so the platform could not read {@code java.security}
   * and start-up died with an {@code InternalError}.
   */
  public void testTheRealFileSystemStaysReachable ()
    throws IOException, InterruptedException {

    String output = fork(OVERLAY_ROOT);

    Assert.assertTrue(output.contains("javaHomePathKind=NativePath"), output);
    Assert.assertTrue(output.contains("javaHomeIsDirectory=true"), output);
    Assert.assertTrue(output.contains("javaSecurityReadable=true"), output);
    Assert.assertTrue(output.contains("createTempFile=ok"), output);
  }

  public void testRelativePathsAreServedFromTheHeap ()
    throws IOException, InterruptedException {

    String output = fork(OVERLAY_ROOT);

    Assert.assertTrue(output.contains("relativeRoundTrip=relative"), output);
  }

  /**
   * A configuration that claims every path is refused in terms that name the cause, rather than
   * being allowed to fail later inside the platform's own start-up.
   */
  public void testAConfigurationThatClaimsEveryPathIsRefused ()
    throws IOException, InterruptedException {

    String output = fork(null);

    Assert.assertFalse(output.contains("defaultFileSystem="), "The JVM should not have started with an overlay claiming every path: " + output);
    Assert.assertTrue(output.contains("claims every path"), output);
    Assert.assertTrue(output.contains("org.smallmind.file.ephemeral.configuration.roots"), output);
  }

  /**
   * Runs in the forked JVM and reports what it observes. Each line is a {@code key=value} pair so
   * that the parent can assert on it without depending on wording.
   */
  public static class Harness {

    public static void main (String[] args) {

      System.out.println("defaultFileSystem=" + FileSystems.getDefault().getClass().getName());
      System.out.println("scheme=" + FileSystems.getDefault().provider().getScheme());

      // the overlay root is served from the heap
      try {

        Path overlayFile = Paths.get(OVERLAY_ROOT, "greeting.txt");

        Files.createDirectories(overlayFile.getParent());
        Files.writeString(overlayFile, "in-memory", StandardCharsets.UTF_8);

        System.out.println("overlayRoundTrip=" + Files.readString(overlayFile));
        System.out.println("overlayPathKind=" + overlayFile.getClass().getSimpleName());
        System.out.println("overlayAbsentFromDisk=" + (!new File(OVERLAY_ROOT + "/greeting.txt").exists()));
      } catch (Exception exception) {
        System.out.println("overlayRoundTrip=" + exception.getClass().getSimpleName());
      }

      // everything outside it still reaches the real file system
      try {

        Path javaHome = Paths.get(System.getProperty("java.home"));

        System.out.println("javaHomePathKind=" + javaHome.getClass().getSimpleName());
        System.out.println("javaHomeIsDirectory=" + Files.isDirectory(javaHome));
        System.out.println("javaSecurityReadable=" + (Files.size(javaHome.resolve("conf").resolve("security").resolve("java.security")) > 0));
      } catch (Exception exception) {
        System.out.println("javaSecurityReadable=" + exception.getClass().getSimpleName());
      }

      // the machinery that first exposed the problem, since it needs SecureRandom, which needs
      // java.security, which is read through the default file system
      try {

        Path temporaryFile = Files.createTempFile("probe", ".tmp");

        Files.delete(temporaryFile);
        System.out.println("createTempFile=ok");
      } catch (Throwable throwable) {
        System.out.println("createTempFile=" + throwable.getClass().getSimpleName());
      }

      // a relative path is served from the heap, and its directory is there from the outset
      try {

        Path relativeFile = Paths.get("relative.txt");

        Files.writeString(relativeFile, "relative", StandardCharsets.UTF_8);
        System.out.println("relativeRoundTrip=" + Files.readString(relativeFile));
      } catch (Exception exception) {
        System.out.println("relativeRoundTrip=" + exception.getClass().getSimpleName());
      }
    }
  }
}
