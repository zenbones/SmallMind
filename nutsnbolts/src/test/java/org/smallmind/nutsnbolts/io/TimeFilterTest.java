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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.smallmind.nutsnbolts.time.TimeArithmetic;
import org.smallmind.nutsnbolts.time.TimeOperation;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "integration")
public class TimeFilterTest {

  private Path tempFile;

  @BeforeMethod
  public void setUp ()
    throws IOException {

    tempFile = Files.createTempFile("time-filter-", ".tmp");
  }

  @AfterMethod
  public void tearDown ()
    throws IOException {

    Files.deleteIfExists(tempFile);
  }

  public void testFilterAcceptsFileOlderThanReferenceWhenUsingBefore ()
    throws IOException {

    Instant oneHourAgo = Instant.now().minusSeconds(3600);

    Files.setLastModifiedTime(tempFile, FileTime.from(oneHourAgo));

    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    TimeArithmetic before = new TimeArithmetic(now, TimeOperation.BEFORE);
    TimeFilter filter = new TimeFilter(before);

    Assert.assertTrue(filter.accept(tempFile));
  }

  public void testFilterRejectsFileOlderThanReferenceWhenUsingAfter ()
    throws IOException {

    Instant oneHourAgo = Instant.now().minusSeconds(3600);

    Files.setLastModifiedTime(tempFile, FileTime.from(oneHourAgo));

    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    TimeArithmetic after = new TimeArithmetic(now, TimeOperation.AFTER);
    TimeFilter filter = new TimeFilter(after);

    Assert.assertFalse(filter.accept(tempFile));
  }
}
