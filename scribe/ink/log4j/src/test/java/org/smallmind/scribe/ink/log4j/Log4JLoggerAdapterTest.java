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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Record;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises {@link Log4JLoggerAdapter} against real, uniquely named Log4j2 loggers. Log4j2 keeps a
 * process-wide static {@code LoggerContext}, so every test claims its own token in {@link #before()} —
 * pairing the logger name and capturing-appender name to the same value — and detaches every appender in
 * {@link #after()}. Without that isolation, appenders accumulate on the shared configuration and bleed
 * across methods, which previously surfaced as an intermittent {@code NullPointerException} from
 * {@code addLoggerAppender} during a full-module run.
 */
@Test(groups = "unit")
public class Log4JLoggerAdapterTest {

  private static final AtomicInteger LOGGER_COUNTER = new AtomicInteger();
  private static final Level[] LEVELS = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL, Level.OFF};

  private Logger logger;
  private int token;

  @BeforeMethod
  public void before () {

    token = LOGGER_COUNTER.incrementAndGet();
    logger = (Logger)LogManager.getLogger("log4j.test." + token);
  }

  @AfterMethod
  public void after () {

    for (Appender appender : logger.getAppenders().values()) {
      logger.removeAppender(appender);
    }
  }

  private Log4JLoggerAdapter newAdapter () {

    return new Log4JLoggerAdapter(logger);
  }

  private CapturingAppender newCapturingAppender () {

    CapturingAppender capturingAppender = new CapturingAppender();

    capturingAppender.setName("capturing." + token);

    return capturingAppender;
  }

  public void testNameReflectsUnderlyingLogger () {

    Assert.assertEquals(newAdapter().getName(), "log4j.test." + token);
  }

  public void testLevelRoundTripThroughUnderlyingLogger () {

    Log4JLoggerAdapter adapter = newAdapter();

    adapter.setLevel(Level.WARN);
    Assert.assertEquals(adapter.getLevel(), Level.WARN);
  }

  public void testAllLevelsRoundTripThroughTranslator () {

    Log4JLoggerAdapter adapter = newAdapter();

    for (Level level : LEVELS) {
      adapter.setLevel(level);
      Assert.assertEquals(adapter.getLevel(), level);
    }
  }

  public void testLogMessageRoutesToScribeAppender () {

    Log4JLoggerAdapter adapter = newAdapter();
    CapturingAppender capturingAppender = newCapturingAppender();

    adapter.addAppender(capturingAppender);
    adapter.setLevel(Level.TRACE);
    adapter.logMessage(Level.INFO, null, "hi %s", "there");

    Assert.assertEquals(capturingAppender.size(), 1);

    Record<?> record = capturingAppender.getRecords().get(0);
    Assert.assertEquals(record.getLevel(), Level.INFO);
    Assert.assertEquals(record.getMessage(), "hi there");
  }

  public void testLogMessageWithObjectRoutesToScribeAppender () {

    Log4JLoggerAdapter adapter = newAdapter();
    CapturingAppender capturingAppender = newCapturingAppender();

    adapter.addAppender(capturingAppender);
    adapter.setLevel(Level.TRACE);
    adapter.logMessage(Level.INFO, null, Integer.valueOf(42));

    Assert.assertEquals(capturingAppender.size(), 1);
    Assert.assertEquals(capturingAppender.getRecords().get(0).getMessage(), "42");
  }

  public void testLogMessageWithSupplierRoutesToScribeAppender () {

    Log4JLoggerAdapter adapter = newAdapter();
    CapturingAppender capturingAppender = newCapturingAppender();

    adapter.addAppender(capturingAppender);
    adapter.setLevel(Level.TRACE);
    adapter.logMessage(Level.INFO, null, () -> "lazy");

    Assert.assertEquals(capturingAppender.size(), 1);
    Assert.assertEquals(capturingAppender.getRecords().get(0).getMessage(), "lazy");
  }

  public void testLevelBelowThresholdIsSuppressed () {

    Log4JLoggerAdapter adapter = newAdapter();
    CapturingAppender capturingAppender = newCapturingAppender();

    adapter.addAppender(capturingAppender);
    adapter.setLevel(Level.ERROR);
    adapter.logMessage(Level.INFO, null, "ignored");

    Assert.assertEquals(capturingAppender.size(), 0);
  }

  public void testOffLevelRecordIsNeverPublished () {

    Log4JLoggerAdapter adapter = newAdapter();
    CapturingAppender capturingAppender = newCapturingAppender();

    adapter.addAppender(capturingAppender);
    adapter.setLevel(Level.TRACE);
    adapter.logMessage(Level.OFF, null, "ignored");

    Assert.assertEquals(capturingAppender.size(), 0);
  }

  public void testFilterVetoSuppressesPublish () {

    Log4JLoggerAdapter adapter = newAdapter();
    CapturingAppender capturingAppender = newCapturingAppender();

    adapter.addAppender(capturingAppender);
    adapter.setLevel(Level.TRACE);
    adapter.addFilter(record -> false);
    adapter.logMessage(Level.INFO, null, "vetoed");

    Assert.assertEquals(capturingAppender.size(), 0);

    adapter.clearFilters();
    adapter.logMessage(Level.INFO, null, "allowed");
    Assert.assertEquals(capturingAppender.size(), 1);
  }

  public void testEnhancerIsAppliedBeforePublish () {

    Log4JLoggerAdapter adapter = newAdapter();
    CapturingAppender capturingAppender = newCapturingAppender();
    AtomicBoolean enhanced = new AtomicBoolean(false);

    adapter.addAppender(capturingAppender);
    adapter.setLevel(Level.TRACE);
    adapter.addEnhancer(record -> enhanced.set(true));
    adapter.logMessage(Level.INFO, null, "enhance me");

    Assert.assertTrue(enhanced.get());
    Assert.assertEquals(capturingAppender.size(), 1);

    adapter.clearEnhancers();
    enhanced.set(false);
    adapter.logMessage(Level.INFO, null, "again");
    Assert.assertFalse(enhanced.get());
  }

  public void testAutoFillLoggerContextRoundTrip () {

    Log4JLoggerAdapter adapter = newAdapter();

    Assert.assertFalse(adapter.getAutoFillLoggerContext());
    adapter.setAutoFillLoggerContext(true);
    Assert.assertTrue(adapter.getAutoFillLoggerContext());
  }

  public void testClearAppendersStopsRouting () {

    Log4JLoggerAdapter adapter = newAdapter();
    CapturingAppender capturingAppender = newCapturingAppender();

    adapter.addAppender(capturingAppender);
    adapter.clearAppenders();
    adapter.setLevel(Level.TRACE);
    adapter.logMessage(Level.INFO, null, "ignored");

    Assert.assertEquals(capturingAppender.size(), 0);
  }
}
