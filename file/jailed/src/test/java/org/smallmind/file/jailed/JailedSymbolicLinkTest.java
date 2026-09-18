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
package org.smallmind.file.jailed;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Covers the escape that path arithmetic alone can not close: a link planted inside the jail
 * that points outside of it.
 *
 * <p>Under {@link JailPolicy#STRICT} the real location of a translated path is verified, so
 * reading through such a link is refused while operations that do not follow it - deleting it,
 * or reading its own attributes - still work. Under {@link JailPolicy#LENIENT} the link is
 * followed, which is the documented cost of skipping that verification. Links created through
 * the jail are confined by construction, and a link pointing out of the jail is refused rather
 * than having its target disclosed.
 *
 * <p>Creating a symbolic link is a privileged operation on Windows, so the tests that need one
 * are skipped where the platform will not create it. A directory can be linked without that
 * privilege by way of a junction, which is a different kind of reparse point - it does not even
 * report itself as a symbolic link - but escapes the jail in exactly the same way, so the
 * directory tests fall back to one and run everywhere.
 */
@Test(groups = "unit")
public class JailedSymbolicLinkTest {

  private Path nativeRoot;
  private Path outsideDirectory;
  private Path outsideFile;
  private FileSystem strictFileSystem;
  private FileSystem lenientFileSystem;

  @BeforeMethod
  public void beforeMethod ()
    throws IOException {

    nativeRoot = Files.createTempDirectory("jailed-link-test-");
    outsideDirectory = Files.createTempDirectory("jailed-link-outside-");
    outsideFile = Files.writeString(outsideDirectory.resolve("secret.txt"), "outside", StandardCharsets.UTF_8);

    strictFileSystem = new JailedFileSystemProvider("jailed", new RootedPathTranslator(nativeRoot, JailPolicy.STRICT)).getFileSystem(URI.create("jailed:///"));
    lenientFileSystem = new JailedFileSystemProvider("jailed", new RootedPathTranslator(nativeRoot, JailPolicy.LENIENT)).getFileSystem(URI.create("jailed:///"));
  }

  @AfterMethod
  public void afterMethod () {

    delete(nativeRoot);
    delete(outsideDirectory);
  }

  private void delete (Path directory) {

    if ((directory != null) && Files.exists(directory)) {
      try (Stream<Path> walk = Files.walk(directory)) {
        walk.sorted(Comparator.reverseOrder()).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException ignored) {
            // best-effort cleanup
          }
        });
      } catch (IOException ignored) {
        // best-effort cleanup
      }
    }
  }

  /**
   * Plants a symbolic link natively, the way something outside the jail would, skipping the test
   * when the platform refuses to create one.
   *
   * @param link   the native path of the link to create
   * @param target the native path the link should point at
   */
  private void plantNativeLink (Path link, Path target) {

    try {
      Files.createSymbolicLink(link, target);
    } catch (IOException | UnsupportedOperationException exception) {
      throw new SkipException("This platform will not create symbolic links: " + exception.getMessage());
    }
  }

  /**
   * Plants a link to a directory natively, falling back to a Windows junction where symbolic
   * links are privileged, and skipping the test only when neither can be created.
   *
   * @param link   the native path of the link to create
   * @param target the native directory the link should point at
   */
  private void plantNativeDirectoryLink (Path link, Path target) {

    try {
      Files.createSymbolicLink(link, target);
    } catch (IOException | UnsupportedOperationException exception) {
      if (!createJunction(link, target)) {
        throw new SkipException("This platform will not create a link to a directory: " + exception.getMessage());
      }
    }
  }

  /**
   * Creates a Windows junction, which is an unprivileged operation.
   *
   * @param link   the native path of the junction to create
   * @param target the native directory the junction should point at
   * @return {@code true} if the junction was created
   */
  private boolean createJunction (Path link, Path target) {

    if (!"\\".equals(FileSystems.getDefault().getSeparator())) {

      return false;
    } else {
      try {

        Process process = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), target.toString()).redirectErrorStream(true).start();

        return (process.waitFor() == 0) && Files.exists(link);
      } catch (IOException ioException) {

        return false;
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();

        return false;
      }
    }
  }

  public void testStrictPolicyRefusesReadingThroughAnEscapingLink () {

    plantNativeLink(nativeRoot.resolve("escape.txt"), outsideFile);

    try {
      Files.readString(strictFileSystem.getPath("/escape.txt"), StandardCharsets.UTF_8);
      Assert.fail("The escaping link should not have been followed");
    } catch (SecurityException securityException) {
      // the jail refused to follow the link out, as expected
    } catch (IOException ioException) {
      Assert.fail("Unexpected exception: " + ioException);
    }
  }

  public void testStrictPolicyRefusesListingThroughAnEscapingDirectoryLink () {

    plantNativeDirectoryLink(nativeRoot.resolve("escape"), outsideDirectory);

    try (Stream<Path> stream = Files.list(strictFileSystem.getPath("/escape"))) {
      Assert.fail("The escaping link should not have been followed, but listed " + stream.count() + " entries");
    } catch (SecurityException securityException) {
      // the jail refused to follow the link out, as expected
    } catch (IOException ioException) {
      Assert.fail("Unexpected exception: " + ioException);
    }
  }

  public void testStrictPolicyRefusesReadingBeneathAnEscapingDirectoryLink () {

    plantNativeDirectoryLink(nativeRoot.resolve("escape"), outsideDirectory);

    try {
      Files.readString(strictFileSystem.getPath("/escape/secret.txt"), StandardCharsets.UTF_8);
      Assert.fail("The escaping link should not have been followed");
    } catch (SecurityException securityException) {
      // the jail refused to follow the link out, as expected
    } catch (IOException ioException) {
      Assert.fail("Unexpected exception: " + ioException);
    }
  }

  public void testStrictPolicyRefusesWritingBeneathAnEscapingDirectoryLink () {

    plantNativeDirectoryLink(nativeRoot.resolve("escape"), outsideDirectory);

    try {
      Files.writeString(strictFileSystem.getPath("/escape/planted.txt"), "planted", StandardCharsets.UTF_8);
      Assert.fail("The escaping link should not have been followed");
    } catch (SecurityException securityException) {
      // the jail refused to follow the link out, as expected
    } catch (IOException ioException) {
      Assert.fail("Unexpected exception: " + ioException);
    }

    Assert.assertFalse(Files.exists(outsideDirectory.resolve("planted.txt")));
  }

  public void testStrictPolicyStillAllowsOperationsThatDoNotFollowTheLink ()
    throws IOException {

    plantNativeLink(nativeRoot.resolve("escape.txt"), outsideFile);

    BasicFileAttributes attributes = Files.readAttributes(strictFileSystem.getPath("/escape.txt"), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

    Assert.assertTrue(attributes.isSymbolicLink());

    Files.delete(strictFileSystem.getPath("/escape.txt"));

    Assert.assertFalse(Files.exists(nativeRoot.resolve("escape.txt"), LinkOption.NOFOLLOW_LINKS));
    Assert.assertTrue(Files.exists(outsideFile), "deleting the link must not delete its target");
  }

  public void testStrictPolicyStillAllowsAnEscapingDirectoryLinkToBeDeleted ()
    throws IOException {

    plantNativeDirectoryLink(nativeRoot.resolve("escape"), outsideDirectory);

    Files.delete(strictFileSystem.getPath("/escape"));

    Assert.assertFalse(Files.exists(nativeRoot.resolve("escape"), LinkOption.NOFOLLOW_LINKS));
    Assert.assertTrue(Files.exists(outsideFile), "deleting the link must not delete its target");
  }

  public void testStrictPolicyFollowsLinksThatStayInside ()
    throws IOException {

    Files.writeString(nativeRoot.resolve("data.txt"), "inside", StandardCharsets.UTF_8);
    plantNativeLink(nativeRoot.resolve("inside.txt"), nativeRoot.resolve("data.txt"));

    Assert.assertEquals(Files.readString(strictFileSystem.getPath("/inside.txt"), StandardCharsets.UTF_8), "inside");
  }

  public void testStrictPolicyFollowsDirectoryLinksThatStayInside ()
    throws IOException {

    Files.createDirectory(nativeRoot.resolve("data"));
    Files.writeString(nativeRoot.resolve("data").resolve("inside.txt"), "inside", StandardCharsets.UTF_8);
    plantNativeDirectoryLink(nativeRoot.resolve("shortcut"), nativeRoot.resolve("data"));

    Assert.assertEquals(Files.readString(strictFileSystem.getPath("/shortcut/inside.txt"), StandardCharsets.UTF_8), "inside");
  }

  public void testLenientPolicyFollowsAnEscapingDirectoryLink ()
    throws IOException {

    plantNativeDirectoryLink(nativeRoot.resolve("escape"), outsideDirectory);

    Assert.assertEquals(Files.readString(lenientFileSystem.getPath("/escape/secret.txt"), StandardCharsets.UTF_8), "outside");
  }

  public void testReadingAnEscapingLinkTargetIsRefused () {

    plantNativeLink(nativeRoot.resolve("escape.txt"), outsideFile);

    try {
      Files.readSymbolicLink(strictFileSystem.getPath("/escape.txt"));
      Assert.fail("The target outside the jail should not have been disclosed");
    } catch (SecurityException securityException) {
      // the jail refused to describe a location outside itself, as expected
    } catch (IOException ioException) {
      Assert.fail("Unexpected exception: " + ioException);
    }
  }

  public void testLinkCreatedThroughTheJailIsClampedToTheJail ()
    throws IOException {

    Files.createDirectory(strictFileSystem.getPath("/dir"));
    Files.writeString(strictFileSystem.getPath("/secret.txt"), "jailed", StandardCharsets.UTF_8);

    try {
      Files.createSymbolicLink(strictFileSystem.getPath("/dir/link.txt"), strictFileSystem.getPath("../../../secret.txt"));
    } catch (IOException | UnsupportedOperationException exception) {
      throw new SkipException("This platform will not create symbolic links: " + exception.getMessage());
    }

    Assert.assertEquals(Files.readString(strictFileSystem.getPath("/dir/link.txt"), StandardCharsets.UTF_8), "jailed");
    Assert.assertEquals(Files.readSymbolicLink(strictFileSystem.getPath("/dir/link.txt")).toString(), "../secret.txt");
  }

  public void testLinkCreatedThroughTheJailRoundTrips ()
    throws IOException {

    Files.createDirectory(strictFileSystem.getPath("/dir"));
    Files.writeString(strictFileSystem.getPath("/dir/data.txt"), "sibling", StandardCharsets.UTF_8);

    try {
      Files.createSymbolicLink(strictFileSystem.getPath("/dir/link.txt"), strictFileSystem.getPath("data.txt"));
    } catch (IOException | UnsupportedOperationException exception) {
      throw new SkipException("This platform will not create symbolic links: " + exception.getMessage());
    }

    Assert.assertEquals(Files.readSymbolicLink(strictFileSystem.getPath("/dir/link.txt")).toString(), "data.txt");
    Assert.assertEquals(Files.readString(strictFileSystem.getPath("/dir/link.txt"), StandardCharsets.UTF_8), "sibling");
  }
}
