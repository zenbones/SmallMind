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
import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LastModifiedCleanupRuleTest {

  private static final long DAY_IN_MILLIS = 24L * 60 * 60 * 1000;

  private Path directory;

  @BeforeMethod
  public void createDirectory ()
    throws IOException {

    directory = Files.createTempDirectory("scribe-lastmodified-test");
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

  private Path createAgedFile (String name, long lastModifiedMillis)
    throws IOException {

    Path path = Files.createFile(directory.resolve(name));

    Files.setLastModifiedTime(path, FileTime.fromMillis(lastModifiedMillis));

    return path;
  }

  public void testFileYoungerThanStintIsRetained ()
    throws IOException {

    Path path = createAgedFile("rolled.log", System.currentTimeMillis());
    LastModifiedCleanupRule rule = new LastModifiedCleanupRule(new Stint(1, TimeUnit.DAYS));

    Assert.assertFalse(rule.willCleanup(path));
  }

  public void testFileOlderThanStintIsEligibleForCleanup ()
    throws IOException {

    Path path = createAgedFile("rolled.log", System.currentTimeMillis() - (2 * DAY_IN_MILLIS));
    LastModifiedCleanupRule rule = new LastModifiedCleanupRule(new Stint(1, TimeUnit.DAYS));

    Assert.assertTrue(rule.willCleanup(path));
  }

  public void testCopyPreservesTheConfiguredStint () {

    Stint stint = new Stint(7, TimeUnit.DAYS);
    LastModifiedCleanupRule copy = new LastModifiedCleanupRule(stint).copy();

    Assert.assertSame(copy.getStint(), stint);
  }

  public void testFinishIsANoOp ()
    throws IOException {

    Path path = createAgedFile("rolled.log", System.currentTimeMillis());
    LastModifiedCleanupRule rule = new LastModifiedCleanupRule(new Stint(1, TimeUnit.DAYS));

    rule.finish();

    // finish() deletes nothing for this rule; cleanup is decided eagerly in willCleanup.
    Assert.assertTrue(Files.exists(path));
  }
}
