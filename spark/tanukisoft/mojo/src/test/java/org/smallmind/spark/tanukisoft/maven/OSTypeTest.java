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
package org.smallmind.spark.tanukisoft.maven;

import org.testng.Assert;

// A wrong filename in the platform table silently produces a broken wrapper distribution, so the binding between an
// OSType's declared style and the shape of its executable/library names is the contract worth guarding here.
@org.testng.annotations.Test(groups = "unit")
public class OSTypeTest {

  public void testEveryPlatformCarriesNonBlankBinaries () {

    for (OSType osType : OSType.values()) {
      Assert.assertNotNull(osType.getOsStyle(), osType.name());
      Assert.assertFalse(osType.getExecutable().isBlank(), osType.name());
      Assert.assertFalse(osType.getLibrary().isBlank(), osType.name());
      Assert.assertTrue(osType.getExecutable().startsWith("wrapper-"), osType.name());
    }
  }

  public void testWindowsPlatformsUseWindowsBinaryExtensions () {

    for (OSType osType : OSType.values()) {
      if (OSStyle.WINDOWS.equals(osType.getOsStyle())) {
        Assert.assertTrue(osType.getExecutable().endsWith(".exe"), osType.name());
        Assert.assertTrue(osType.getLibrary().endsWith(".dll"), osType.name());
      }
    }
  }

  public void testUnixPlatformsAvoidWindowsBinaryExtensions () {

    for (OSType osType : OSType.values()) {
      if (OSStyle.UNIX.equals(osType.getOsStyle())) {
        Assert.assertFalse(osType.getExecutable().endsWith(".exe"), osType.name());
        Assert.assertFalse(osType.getLibrary().endsWith(".dll"), osType.name());
        Assert.assertTrue(osType.getLibrary().startsWith("libwrapper-"), osType.name());
      }
    }
  }

  public void testStyleSuppliesTheGenericLibraryName () {

    Assert.assertEquals(OSStyle.WINDOWS.getLibrary(), "wrapper.dll");
    Assert.assertEquals(OSStyle.UNIX.getLibrary(), "libwrapper.so");
  }
}
