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
package org.smallmind.scribe.ink.jdk;

import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JDKLevelTranslatorTest {

  public void testGetLevelNullReturnsNull () {

    Assert.assertNull(JDKLevelTranslator.getLevel(null));
  }

  public void testGetLevelAllMapsToTrace () {

    Assert.assertEquals(JDKLevelTranslator.getLevel(java.util.logging.Level.ALL), Level.TRACE);
  }

  public void testGetLevelFinestMapsToTrace () {

    Assert.assertEquals(JDKLevelTranslator.getLevel(java.util.logging.Level.FINEST), Level.TRACE);
  }

  public void testGetLevelFinerMapsToTrace () {

    Assert.assertEquals(JDKLevelTranslator.getLevel(java.util.logging.Level.FINER), Level.TRACE);
  }

  public void testGetLevelFineMapsToDebug () {

    Assert.assertEquals(JDKLevelTranslator.getLevel(java.util.logging.Level.FINE), Level.DEBUG);
  }

  public void testGetLevelConfigMapsToInfo () {

    Assert.assertEquals(JDKLevelTranslator.getLevel(java.util.logging.Level.CONFIG), Level.INFO);
  }

  public void testGetLevelInfoMapsToInfo () {

    Assert.assertEquals(JDKLevelTranslator.getLevel(java.util.logging.Level.INFO), Level.INFO);
  }

  public void testGetLevelWarningMapsToWarn () {

    Assert.assertEquals(JDKLevelTranslator.getLevel(java.util.logging.Level.WARNING), Level.WARN);
  }

  public void testGetLevelSevereMapsToError () {

    Assert.assertEquals(JDKLevelTranslator.getLevel(java.util.logging.Level.SEVERE), Level.ERROR);
  }

  public void testGetLevelOffMapsToOff () {

    Assert.assertEquals(JDKLevelTranslator.getLevel(java.util.logging.Level.OFF), Level.OFF);
  }

  public void testGetJDKLevelTraceMapsToFiner () {

    Assert.assertEquals(JDKLevelTranslator.getJDKLevel(Level.TRACE), java.util.logging.Level.FINER);
  }

  public void testGetJDKLevelDebugMapsToFine () {

    Assert.assertEquals(JDKLevelTranslator.getJDKLevel(Level.DEBUG), java.util.logging.Level.FINE);
  }

  public void testGetJDKLevelInfoMapsToInfo () {

    Assert.assertEquals(JDKLevelTranslator.getJDKLevel(Level.INFO), java.util.logging.Level.INFO);
  }

  public void testGetJDKLevelWarnMapsToWarning () {

    Assert.assertEquals(JDKLevelTranslator.getJDKLevel(Level.WARN), java.util.logging.Level.WARNING);
  }

  public void testGetJDKLevelErrorMapsToSevere () {

    Assert.assertEquals(JDKLevelTranslator.getJDKLevel(Level.ERROR), java.util.logging.Level.SEVERE);
  }

  public void testGetJDKLevelFatalMapsToSevere () {

    Assert.assertEquals(JDKLevelTranslator.getJDKLevel(Level.FATAL), java.util.logging.Level.SEVERE);
  }

  public void testGetJDKLevelOffMapsToOff () {

    Assert.assertEquals(JDKLevelTranslator.getJDKLevel(Level.OFF), java.util.logging.Level.OFF);
  }
}
