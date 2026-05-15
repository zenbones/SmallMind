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

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EphemeralFileSystemConfigurationTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNonPositiveCapacityRejected () {

    new EphemeralFileSystemConfiguration(0L, 1024, "/");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNonPositiveBlockSizeRejected () {

    new EphemeralFileSystemConfiguration(1024L, 0, "/");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyRootsRejected () {

    new EphemeralFileSystemConfiguration(1024L, 1024);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRootMissingLeadingSlashRejected () {

    new EphemeralFileSystemConfiguration(1024L, 1024, "opt");
  }

  public void testAccessors () {

    EphemeralFileSystemConfiguration cfg = new EphemeralFileSystemConfiguration(2048L, 512, "/opt", "/var");

    Assert.assertEquals(cfg.getCapacity(), 2048L);
    Assert.assertEquals(cfg.getBlockSize(), 512);
    Assert.assertEquals(cfg.getRoots(), new String[] {"/opt", "/var"});
  }

  public void testIsOursMatches () {

    EphemeralFileSystemConfiguration cfg = new EphemeralFileSystemConfiguration(1024L, 1024, "/opt/whatsit");

    Assert.assertTrue(cfg.isOurs("/opt/whatsit"));
    Assert.assertTrue(cfg.isOurs("/opt/whatsit/x"));
  }

  public void testIsOursDoesNotMatch () {

    EphemeralFileSystemConfiguration cfg = new EphemeralFileSystemConfiguration(1024L, 1024, "/opt/whatsit");

    Assert.assertFalse(cfg.isOurs("/var"));
    Assert.assertFalse(cfg.isOurs("/opt"));
    Assert.assertFalse(cfg.isOurs("/opt/something-else"));
  }

  public void testIsOursMultipleRoots () {

    EphemeralFileSystemConfiguration cfg = new EphemeralFileSystemConfiguration(1024L, 1024, "/a", "/b/c");

    Assert.assertTrue(cfg.isOurs("/a"));
    Assert.assertTrue(cfg.isOurs("/a/x"));
    Assert.assertTrue(cfg.isOurs("/b/c"));
    Assert.assertTrue(cfg.isOurs("/b/c/y"));
    Assert.assertFalse(cfg.isOurs("/b"));
    Assert.assertFalse(cfg.isOurs("/c"));
  }
}
