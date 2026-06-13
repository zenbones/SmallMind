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

import org.apache.maven.plugin.MojoExecutionException;
import org.testng.Assert;

// Fast validation branches that reject bad configuration before any staging happens; the full distribution build is
// covered by WrapperDistributionIntegrationTest.
@org.testng.annotations.Test(groups = "unit")
public class GenerateWrapperMojoTest {

  private GenerateWrapperMojo mojo (boolean skip, String operatingSystem, String compression)
    throws Exception {

    GenerateWrapperMojo mojo = new GenerateWrapperMojo();

    TanukiMojoSupport.setField(mojo, "skip", skip);
    TanukiMojoSupport.setField(mojo, "operatingSystem", operatingSystem);
    TanukiMojoSupport.setField(mojo, "compression", compression);
    TanukiMojoSupport.setField(mojo, "verbose", false);

    return mojo;
  }

  public void testSkipShortCircuitsTheGoal ()
    throws Exception {

    mojo(true, "bogus", "bogus").execute();
  }

  public void testUnknownOperatingSystemIsReported ()
    throws Exception {

    Assert.assertThrows(MojoExecutionException.class, () -> mojo(false, "PALM_OS", "zip").execute());
  }

  public void testUnknownCompressionIsReported ()
    throws Exception {

    Assert.assertThrows(MojoExecutionException.class, () -> mojo(false, "LINUX_X86_64", "rar").execute());
  }
}
