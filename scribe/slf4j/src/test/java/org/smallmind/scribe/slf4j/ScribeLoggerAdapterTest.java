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
package org.smallmind.scribe.slf4j;

import org.slf4j.spi.LocationAwareLogger;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ScribeLoggerAdapterTest {

  private static int sequence = 0;

  private CapturingAppender attach (Logger logger, Level level) {

    CapturingAppender appender = new CapturingAppender();

    logger.clearAppenders();
    logger.addAppender(appender);
    logger.setLevel(level);

    return appender;
  }

  private Logger uniqueLogger () {

    return LoggerManager.getLogger("slf4j.test." + Thread.currentThread().getId() + "." + (sequence++) + "." + System.nanoTime());
  }

  public void testSinglePlaceholderTranslation () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    adapter.info("hello {}", "world");

    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getLast().getMessage(), "hello world");
  }

  public void testTwoPlaceholderTranslation () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    adapter.info("{} and {}", "a", "b");

    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getLast().getMessage(), "a and b");
  }

  public void testObjectArrayVarargsTranslation () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    adapter.info("{}-{}-{}", new Object[] {"x", "y", "z"});

    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getLast().getMessage(), "x-y-z");
  }

  public void testLiteralMessagePassThrough () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    adapter.info("no placeholders here");

    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getLast().getMessage(), "no placeholders here");
  }

  public void testEscapedBraceIsLiteral () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    adapter.info("literal \\{} and {}", "value");

    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getLast().getMessage(), "literal {} and value");
  }

  public void testDebugTranslation () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    adapter.debug("count is {}", 7);

    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getLast().getMessage(), "count is 7");
  }

  public void testNameDelegatesToLogger () {

    Logger logger = uniqueLogger();
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    Assert.assertEquals(adapter.getName(), logger.getName());
  }

  public void testEnablementReflectsInfoLevel () {

    Logger logger = uniqueLogger();
    attach(logger, Level.INFO);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    Assert.assertTrue(adapter.isInfoEnabled());
    Assert.assertTrue(adapter.isWarnEnabled());
    Assert.assertTrue(adapter.isErrorEnabled());
    Assert.assertFalse(adapter.isDebugEnabled());
    Assert.assertFalse(adapter.isTraceEnabled());
  }

  public void testEnablementReflectsDebugLevel () {

    Logger logger = uniqueLogger();
    attach(logger, Level.DEBUG);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    Assert.assertTrue(adapter.isDebugEnabled());
    Assert.assertTrue(adapter.isInfoEnabled());
    Assert.assertFalse(adapter.isTraceEnabled());
  }

  public void testBelowThresholdIsNotPublished () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.WARN);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    adapter.info("suppressed {}", "message");

    Assert.assertEquals(appender.size(), 0);
  }

  public void testLocationAwareLogTranslatesLevelAndArgs () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    adapter.log(null, ScribeLoggerAdapter.class.getName(), org.slf4j.spi.LocationAwareLogger.WARN_INT, "warned %s", new Object[] {"now"}, null);

    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getLast().getLevel(), Level.WARN);
    Assert.assertEquals(appender.getLast().getMessage(), "warned now");
  }

  public void testTraceOverloadsAllPublishAtTraceLevel () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);
    RuntimeException boom = new RuntimeException("boom");

    adapter.trace("literal");
    adapter.trace("one {}", "1");
    adapter.trace("{} {}", "a", "b");
    adapter.trace("{}-{}-{}", new Object[] {"x", "y", "z"});
    adapter.trace("with throwable", boom);

    Assert.assertEquals(appender.size(), 5);
    Assert.assertEquals(appender.getRecords().get(1).getMessage(), "one 1");
    Assert.assertEquals(appender.getRecords().get(2).getMessage(), "a b");
    Assert.assertEquals(appender.getRecords().get(3).getMessage(), "x-y-z");
    Assert.assertEquals(appender.getLast().getMessage(), "with throwable");
    Assert.assertSame(appender.getLast().getThrown(), boom);
  }

  public void testWarnOverloadsAllPublish () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);
    RuntimeException boom = new RuntimeException("boom");

    adapter.warn("literal");
    adapter.warn("one {}", "1");
    adapter.warn("{} {}", "a", "b");
    adapter.warn("{}-{}-{}", new Object[] {"x", "y", "z"});
    adapter.warn("with throwable", boom);

    Assert.assertEquals(appender.size(), 5);
    Assert.assertEquals(appender.getRecords().get(2).getMessage(), "a b");
    Assert.assertEquals(appender.getLast().getLevel(), Level.WARN);
    Assert.assertSame(appender.getLast().getThrown(), boom);
  }

  public void testErrorOverloadsAllPublish () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);
    RuntimeException boom = new RuntimeException("boom");

    adapter.error("literal");
    adapter.error("one {}", "1");
    adapter.error("{} {}", "a", "b");
    adapter.error("{}-{}-{}", new Object[] {"x", "y", "z"});
    adapter.error("with throwable", boom);

    Assert.assertEquals(appender.size(), 5);
    Assert.assertEquals(appender.getRecords().get(3).getMessage(), "x-y-z");
    Assert.assertEquals(appender.getLast().getLevel(), Level.ERROR);
    Assert.assertSame(appender.getLast().getThrown(), boom);
  }

  public void testDebugAndInfoThrowableOverloads () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);
    RuntimeException debugBoom = new RuntimeException("debug-boom");
    RuntimeException infoBoom = new RuntimeException("info-boom");

    adapter.debug("debug failed", debugBoom);
    adapter.info("info failed", infoBoom);

    Assert.assertEquals(appender.size(), 2);
    Assert.assertSame(appender.getRecords().get(0).getThrown(), debugBoom);
    Assert.assertSame(appender.getRecords().get(1).getThrown(), infoBoom);
  }

  public void testFiveArgumentLocationAwareLogDelegatesWithThrowable () {

    Logger logger = uniqueLogger();
    CapturingAppender appender = attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);
    RuntimeException boom = new RuntimeException("boom");

    adapter.log(null, ScribeLoggerAdapter.class.getName(), LocationAwareLogger.ERROR_INT, "fatal path", boom);

    Assert.assertEquals(appender.size(), 1);
    Assert.assertEquals(appender.getLast().getLevel(), Level.ERROR);
    Assert.assertSame(appender.getLast().getThrown(), boom);
  }

  public void testLocationAwareLogMapsEveryLevelInt () {

    int[] levelInts = {LocationAwareLogger.TRACE_INT, LocationAwareLogger.DEBUG_INT, LocationAwareLogger.INFO_INT, LocationAwareLogger.WARN_INT, LocationAwareLogger.ERROR_INT};
    Level[] expected = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};

    for (int index = 0; index < levelInts.length; index++) {

      Logger logger = uniqueLogger();
      CapturingAppender appender = attach(logger, Level.TRACE);
      ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

      adapter.log(null, ScribeLoggerAdapter.class.getName(), levelInts[index], "msg", null, null);

      Assert.assertEquals(appender.getLast().getLevel(), expected[index]);
    }
  }

  @Test(expectedExceptions = UnknownSwitchCaseException.class)
  public void testLocationAwareLogRejectsUnknownLevelInt () {

    Logger logger = uniqueLogger();
    attach(logger, Level.TRACE);
    ScribeLoggerAdapter adapter = new ScribeLoggerAdapter(logger);

    adapter.log(null, ScribeLoggerAdapter.class.getName(), 999, "bad level", null, null);
  }
}
