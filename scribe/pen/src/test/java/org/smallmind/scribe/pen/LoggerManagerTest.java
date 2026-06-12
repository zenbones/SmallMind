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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Covers {@link LoggerManager}'s logger caching and template-association behavior. Because the
 * manager's registries are static and process-wide, each test resets them via
 * {@link LoggerManagerTestSupport} to avoid cross-test leakage.
 */
@Test(groups = "unit")
public class LoggerManagerTest {

  @BeforeMethod
  public void reset () {

    LoggerManagerTestSupport.reset();
  }

  @AfterMethod
  public void tearDown () {

    LoggerManagerTestSupport.reset();
  }

  public void testGetLoggerCachesByName () {

    Logger first = LoggerManager.getLogger("cache.by.name");
    Logger second = LoggerManager.getLogger("cache.by.name");

    Assert.assertSame(second, first);
  }

  public void testGetLoggerByClassUsesCanonicalName () {

    Assert.assertEquals(LoggerManager.getLogger(LoggerManagerTest.class).getName(), LoggerManagerTest.class.getCanonicalName());
  }

  public void testMoreSpecificTemplateWinsOnPriority ()
    throws LoggerException {

    new DefaultTemplate(Level.WARN, false).register();
    new ClassNameTemplate(Level.DEBUG, false, "tenant.service.*").register();

    Logger governed = LoggerManager.getLogger("tenant.service.Widget");
    Assert.assertEquals(governed.getLevel(), Level.DEBUG);

    Logger fallback = LoggerManager.getLogger("other.module.Thing");
    Assert.assertEquals(fallback.getLevel(), Level.WARN);
  }

  public void testRegisteringTemplateReassociatesExistingLogger ()
    throws LoggerException {

    Logger logger = LoggerManager.getLogger("late.bound.Widget");
    Assert.assertNull(logger.getTemplate());

    ClassNameTemplate template = new ClassNameTemplate(Level.ERROR, false, "late.bound.*");
    template.register();

    Assert.assertSame(logger.getTemplate(), template);
    Assert.assertEquals(logger.getLevel(), Level.ERROR);
  }

  public void testRemovingTemplateUnassociatesLogger ()
    throws LoggerException {

    ClassNameTemplate template = new ClassNameTemplate(Level.ERROR, false, "removable.*");
    template.register();

    Logger logger = LoggerManager.getLogger("removable.Widget");
    Assert.assertSame(logger.getTemplate(), template);

    LoggerManager.removeTemplate(template);
    Assert.assertNull(logger.getTemplate());
  }
}
