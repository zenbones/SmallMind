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
package org.smallmind.scribe.pen.spring.plan;

import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.CapturingAppender;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.pen.LoggerManagerTestSupport;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LoggingPlanTest {

  private static class CapturingLoggingPlan extends LoggingPlan {

    @Override
    public Appender getAppender () {

      return new CapturingAppender();
    }
  }

  @BeforeMethod
  public void resetLoggerManager () {

    LoggerManagerTestSupport.reset();
  }

  @AfterMethod
  public void clearLoggerManager () {

    LoggerManagerTestSupport.reset();
  }

  public void testDefaultTemplateIsRegisteredAtDefaultLevel ()
    throws Exception {

    CapturingLoggingPlan plan = new CapturingLoggingPlan();

    plan.setDefaultLogLevel(Level.WARN);
    plan.afterPropertiesSet();

    Logger logger = LoggerManager.getLogger("any.unmatched.Logger");

    Assert.assertNotNull(logger.getTemplate());
    Assert.assertEquals(logger.getLevel(), Level.WARN);
  }

  public void testClassNameTemplateIsRegisteredForEachLogEntry ()
    throws Exception {

    CapturingLoggingPlan plan = new CapturingLoggingPlan();

    plan.setDefaultLogLevel(Level.INFO);
    plan.setLogs(new Log[] {new Log(Level.ERROR, "com.example.special.*")});
    plan.afterPropertiesSet();

    Logger matchedLogger = LoggerManager.getLogger("com.example.special.Widget");

    Assert.assertNotNull(matchedLogger.getTemplate());
    Assert.assertEquals(matchedLogger.getLevel(), Level.ERROR);

    Logger defaultLogger = LoggerManager.getLogger("com.other.Thing");

    Assert.assertEquals(defaultLogger.getLevel(), Level.INFO);
  }
}
