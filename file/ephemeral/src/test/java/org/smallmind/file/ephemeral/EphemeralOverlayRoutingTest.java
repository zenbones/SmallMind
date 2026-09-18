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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Pins the decision that separates the in-memory overlay from the native file system.
 *
 * <p>Every case here is expressed with literal path strings rather than paths obtained from the
 * running platform, so the whole matrix is exercised on Linux and on Windows alike. That matters
 * because the two platforms disagree about what an absolute path looks like: a Linux absolute path
 * shares its shape with an ephemeral one, while a Windows absolute path does not, and a rule that is
 * only ever tested on one of them is a rule that only works on one of them.
 *
 * <p>The failure this guards against is specific. A Windows path such as
 * {@code C:\Program Files\...} does not begin with a separator, so it used to be taken for a
 * relative path, and a relative path belongs to whichever file system owns the working directory.
 * With an ephemeral working directory that handed every real file on the machine to the heap; as the
 * JVM default file system it left the platform unable to read {@code java.security}, which surfaced
 * as an {@code InternalError} during start-up.
 */
@Test(groups = "unit")
public class EphemeralOverlayRoutingTest {

  private static final String LINUX_JAVA_HOME = "/usr/lib/jvm/temurin-25";
  private static final String LINUX_JAVA_SECURITY = "/usr/lib/jvm/temurin-25/conf/security/java.security";
  private static final String WINDOWS_JAVA_SECURITY = "C:\\Program Files\\Java\\conf\\security\\java.security";
  private static final String WINDOWS_UNC_PATH = "\\\\fileserver\\share\\thing.txt";

  private EphemeralFileSystemConfiguration configuration (String... roots) {

    return new EphemeralFileSystemConfiguration(1024L, 16, roots);
  }

  public void testAnOverlayRootClaimsItselfAndItsDescendants () {

    EphemeralFileSystemConfiguration overlay = configuration("/overlay");

    Assert.assertTrue(overlay.isOurs("/overlay"));
    Assert.assertTrue(overlay.isOurs("/overlay/a/b"));
    Assert.assertTrue(overlay.isOurs("/overlay", "a", "b"));
  }

  public void testAnOverlayRootIsMatchedOnSegmentBoundaries () {

    EphemeralFileSystemConfiguration overlay = configuration("/overlay");

    // a shared textual prefix is not containment
    Assert.assertFalse(overlay.isOurs("/overlayed"));
    Assert.assertFalse(overlay.isOurs("/overlay-other/a"));
    Assert.assertFalse(overlay.isOurs("/over"));
    Assert.assertFalse(overlay.isOurs("/elsewhere/overlay"));
  }

  public void testAnOverlayRootLeavesLinuxSystemPathsAlone () {

    EphemeralFileSystemConfiguration overlay = configuration("/overlay");

    Assert.assertFalse(overlay.isOurs(LINUX_JAVA_HOME));
    Assert.assertFalse(overlay.isOurs(LINUX_JAVA_SECURITY));
    Assert.assertFalse(overlay.isOurs("/tmp"));
    Assert.assertFalse(overlay.isOurs("/home/someone/project"));
  }

  public void testAWindowsAbsolutePathIsNeverClaimed () {

    // true whatever the roots are, and whatever the working directory is
    for (EphemeralFileSystemConfiguration candidate : new EphemeralFileSystemConfiguration[] {configuration("/"), configuration("/overlay")}) {
      Assert.assertFalse(candidate.isOurs(WINDOWS_JAVA_SECURITY), "A Windows absolute path was claimed by the heap");
      Assert.assertFalse(candidate.isOurs("C:\\Users\\someone"), "A Windows absolute path was claimed by the heap");
      Assert.assertFalse(candidate.isOurs("C:/Users/someone"), "A Windows absolute path spelled with forward slashes was claimed by the heap");
      Assert.assertFalse(candidate.isOurs("D:\\data", "inner", "leaf.txt"), "A Windows absolute path was claimed by the heap");
      Assert.assertFalse(candidate.isOurs(WINDOWS_UNC_PATH), "A UNC path was claimed by the heap");
    }
  }

  public void testARelativePathFollowsTheWorkingDirectory () {

    Assert.assertTrue(configuration("/").isOurs("relative/leaf.txt"));
    Assert.assertTrue(configuration("/overlay").isOurs("relative/leaf.txt"));
    Assert.assertTrue(configuration("/overlay").isOurs(""));

    // with a working directory outside every root, a relative path belongs to the native side
    Assert.assertFalse(configuration("/overlay").withWorkingDirectory("/elsewhere").isOurs("relative/leaf.txt"));
  }

  public void testARootOfSeparatorClaimsEveryEphemeralPath () {

    EphemeralFileSystemConfiguration everything = configuration("/");

    Assert.assertTrue(everything.isOurs("/"));
    Assert.assertTrue(everything.isOurs(LINUX_JAVA_SECURITY));
    Assert.assertTrue(everything.isOurs("/anything/at/all"));
  }

  public void testARootOfSeparatorIsRefusedAsTheDefaultFileSystem () {

    try {
      configuration("/").checkFitToBeDefaultFileSystem();
      Assert.fail("A root that claims every path should be refused as the default file system");
    } catch (IllegalArgumentException illegalArgumentException) {
      Assert.assertTrue(illegalArgumentException.getMessage().contains("claims every path"), illegalArgumentException.getMessage());
    }
  }

  public void testARootClaimingJavaHomeIsRefusedAsTheDefaultFileSystem () {

    String javaHome = System.getProperty("java.home");
    EphemeralFileSystemConfiguration claimsJavaHome = configuration(javaHome.startsWith("/") ? javaHome : "/overlay", "/overlay");

    if (javaHome.startsWith("/")) {
      try {
        claimsJavaHome.checkFitToBeDefaultFileSystem();
        Assert.fail("A root that claims java.home should be refused as the default file system");
      } catch (IllegalArgumentException illegalArgumentException) {
        Assert.assertTrue(illegalArgumentException.getMessage().contains("java.home"), illegalArgumentException.getMessage());
      }
    } else {
      // on a platform whose java.home is not ephemeral-shaped, no ephemeral root can claim it
      claimsJavaHome.checkFitToBeDefaultFileSystem();
    }
  }

  public void testAnOverlayRootIsFitToBeTheDefaultFileSystem () {

    configuration("/overlay").checkFitToBeDefaultFileSystem();
    configuration("/overlay", "/scratch").checkFitToBeDefaultFileSystem();
  }

  public void testRoutingAgreesWithClaiming () {

    EphemeralFileSystemProvider provider = new EphemeralFileSystemProvider(FileSystems.getDefault().provider());
    EphemeralFileSystem fileSystem = new EphemeralFileSystem(provider, configuration("/overlay"));

    Assert.assertTrue(fileSystem.getPath("/overlay/a") instanceof EphemeralPath);
    Assert.assertTrue(fileSystem.getPath("relative.txt") instanceof EphemeralPath);
    Assert.assertTrue(fileSystem.getPath("/elsewhere/a") instanceof NativePath);

    // whatever the platform calls an absolute path of its own must go to the native side
    Assert.assertTrue(fileSystem.getPath(System.getProperty("java.home")) instanceof NativePath);
  }

  public void testTheWorkingDirectoryExistsFromTheOutset () {

    EphemeralFileSystemProvider provider = new EphemeralFileSystemProvider("seeded");
    EphemeralFileSystem fileSystem = new EphemeralFileSystem(provider, configuration("/overlay").withWorkingDirectory("/overlay/work"));

    Assert.assertTrue(Files.isDirectory(fileSystem.getPath("/overlay/work")), "The working directory was not seeded into the heap");
  }
}
