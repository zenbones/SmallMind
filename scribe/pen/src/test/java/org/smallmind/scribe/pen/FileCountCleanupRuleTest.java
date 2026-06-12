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
package org.smallmind.scribe.pen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class FileCountCleanupRuleTest {

  private Path directory;

  @BeforeMethod
  public void createDirectory ()
    throws IOException {

    directory = Files.createTempDirectory("scribe-filecount-test");
  }

  @AfterMethod
  public void removeDirectory ()
    throws IOException {

    if (directory != null) {
      try (java.util.stream.Stream<Path> walk = Files.walk(directory)) {
        walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException ioException) {
            throw new RuntimeException(ioException);
          }
        });
      }
    }
  }

  private Path createAgedFile (String name, long lastModifiedMillis )
    throws IOException {

    Path path = Files.createFile(directory.resolve(name));

    Files.setLastModifiedTime(path, FileTime.fromMillis(lastModifiedMillis));

    return path;
  }

  public void testWillCleanupAccumulatesWithoutDeleting ()
    throws IOException {

    Path path = createAgedFile("rolled-1.log", 1000L);
    FileCountCleanupRule rule = new FileCountCleanupRule(2);

    // The iteration pass never deletes; deletion is deferred entirely to finish().
    Assert.assertFalse(rule.willCleanup(path));
    Assert.assertTrue(Files.exists(path));
  }

  public void testFinishKeepsTheOldestAndDeletesTheNewestExcess ()
    throws IOException {

    Path oldest = createAgedFile("rolled-0.log", 1_000L);
    Path older = createAgedFile("rolled-1.log", 2_000L);
    Path newer = createAgedFile("rolled-2.log", 3_000L);
    Path newest = createAgedFile("rolled-3.log", 4_000L);

    FileCountCleanupRule rule = new FileCountCleanupRule(2);

    rule.willCleanup(oldest);
    rule.willCleanup(older);
    rule.willCleanup(newer);
    rule.willCleanup(newest);
    rule.finish();

    // Retains the two oldest archives; deletes the two newest beyond the maximum.
    Assert.assertTrue(Files.exists(oldest));
    Assert.assertTrue(Files.exists(older));
    Assert.assertFalse(Files.exists(newer));
    Assert.assertFalse(Files.exists(newest));
  }

  public void testFinishDeletesNothingWhenCountIsAtOrBelowMaximum ()
    throws IOException {

    Path first = createAgedFile("rolled-0.log", 1_000L);
    Path second = createAgedFile("rolled-1.log", 2_000L);

    FileCountCleanupRule rule = new FileCountCleanupRule(5);

    rule.willCleanup(first);
    rule.willCleanup(second);
    rule.finish();

    Assert.assertTrue(Files.exists(first));
    Assert.assertTrue(Files.exists(second));
  }

  public void testCopyCarriesMaximumButNotAccumulatedState ()
    throws IOException {

    FileCountCleanupRule rule = new FileCountCleanupRule(3);

    rule.willCleanup(createAgedFile("rolled-0.log", 1_000L));

    FileCountCleanupRule copy = rule.copy();

    Assert.assertEquals(copy.getMaximum(), 3);
    Assert.assertNotSame(copy, rule);
    // A fresh copy carries no accumulated paths, so finish() is a guaranteed no-op.
    copy.finish();
  }
}
