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
package org.smallmind.scribe.ink.log4j;

import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class Log4JLevelTranslatorTest {

  public void testScribeToLog4JMapping () {

    Assert.assertEquals(Log4JLevelTranslator.getLog4JLevel(Level.TRACE), org.apache.logging.log4j.Level.TRACE);
    Assert.assertEquals(Log4JLevelTranslator.getLog4JLevel(Level.DEBUG), org.apache.logging.log4j.Level.DEBUG);
    Assert.assertEquals(Log4JLevelTranslator.getLog4JLevel(Level.INFO), org.apache.logging.log4j.Level.INFO);
    Assert.assertEquals(Log4JLevelTranslator.getLog4JLevel(Level.WARN), org.apache.logging.log4j.Level.WARN);
    Assert.assertEquals(Log4JLevelTranslator.getLog4JLevel(Level.ERROR), org.apache.logging.log4j.Level.ERROR);
    Assert.assertEquals(Log4JLevelTranslator.getLog4JLevel(Level.FATAL), org.apache.logging.log4j.Level.FATAL);
    Assert.assertEquals(Log4JLevelTranslator.getLog4JLevel(Level.OFF), org.apache.logging.log4j.Level.OFF);
  }

  public void testLog4JToScribeMapping () {

    Assert.assertEquals(Log4JLevelTranslator.getLevel(org.apache.logging.log4j.Level.TRACE), Level.TRACE);
    Assert.assertEquals(Log4JLevelTranslator.getLevel(org.apache.logging.log4j.Level.DEBUG), Level.DEBUG);
    Assert.assertEquals(Log4JLevelTranslator.getLevel(org.apache.logging.log4j.Level.INFO), Level.INFO);
    Assert.assertEquals(Log4JLevelTranslator.getLevel(org.apache.logging.log4j.Level.WARN), Level.WARN);
    Assert.assertEquals(Log4JLevelTranslator.getLevel(org.apache.logging.log4j.Level.ERROR), Level.ERROR);
    Assert.assertEquals(Log4JLevelTranslator.getLevel(org.apache.logging.log4j.Level.FATAL), Level.FATAL);
    Assert.assertEquals(Log4JLevelTranslator.getLevel(org.apache.logging.log4j.Level.OFF), Level.OFF);
  }

  public void testRoundTripIsStable () {

    for (Level level : new Level[] {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL, Level.OFF}) {
      Assert.assertEquals(Log4JLevelTranslator.getLevel(Log4JLevelTranslator.getLog4JLevel(level)), level);
    }
  }

  public void testNullLog4JLevelReturnsNull () {

    Assert.assertNull(Log4JLevelTranslator.getLevel(null));
  }
}
