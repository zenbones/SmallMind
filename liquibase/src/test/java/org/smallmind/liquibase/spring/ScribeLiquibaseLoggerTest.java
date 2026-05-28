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
package org.smallmind.liquibase.spring;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ScribeLiquibaseLoggerTest {

  // Represents a JUL Level value not covered by any named constant, verifying the default mapping.
  private static final java.util.logging.Level UNNAMED_JUL_LEVEL = new java.util.logging.Level("UNNAMED", 350) {

  };

  public void testNullLevelTranslatesToNull () {

    Assert.assertNull(ScribeLiquibaseLogger.translateLevel(null));
  }

  public void testAllFinestAndFinerTranslateToTrace () {

    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(java.util.logging.Level.ALL), org.smallmind.scribe.pen.Level.TRACE);
    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(java.util.logging.Level.FINEST), org.smallmind.scribe.pen.Level.TRACE);
    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(java.util.logging.Level.FINER), org.smallmind.scribe.pen.Level.TRACE);
  }

  public void testFineLevelTranslatesToDebug () {

    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(java.util.logging.Level.FINE), org.smallmind.scribe.pen.Level.DEBUG);
  }

  public void testConfigAndInfoTranslateToInfo () {

    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(java.util.logging.Level.CONFIG), org.smallmind.scribe.pen.Level.INFO);
    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(java.util.logging.Level.INFO), org.smallmind.scribe.pen.Level.INFO);
  }

  public void testWarningTranslatesToWarn () {

    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(java.util.logging.Level.WARNING), org.smallmind.scribe.pen.Level.WARN);
  }

  public void testSevereTranslatesToError () {

    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(java.util.logging.Level.SEVERE), org.smallmind.scribe.pen.Level.ERROR);
  }

  public void testOffTranslatesToOff () {

    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(java.util.logging.Level.OFF), org.smallmind.scribe.pen.Level.OFF);
  }

  public void testUnrecognizedLevelDefaultsToInfo () {

    Assert.assertEquals(ScribeLiquibaseLogger.translateLevel(UNNAMED_JUL_LEVEL), org.smallmind.scribe.pen.Level.INFO);
  }
}
