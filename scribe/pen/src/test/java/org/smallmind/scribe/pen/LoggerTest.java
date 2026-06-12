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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the user-facing {@link Logger} delegation surface against the test-only recording backend:
 * level-threshold suppression, lazy {@code Supplier}/{@code Object} deferral, throwable-first ordering,
 * printf message formatting, and the per-logger parameter store. The full appender fan-out pipeline
 * (enhancers, filters, multiple appenders) is covered against the real backend in the ink-indigenous module.
 */
@Test(groups = "unit")
public class LoggerTest {

  @BeforeMethod
  public void resetSink () {

    RecordingLoggerAdapter.reset();
  }

  public void testPrintfMessageIsFormattedAndDelegated () {

    Logger logger = new Logger("test.logger.format");

    logger.info("count %d for %s", 7, "widget");

    List<RecordingLoggerAdapter.Event> events = RecordingLoggerAdapter.getEvents();
    Assert.assertEquals(events.size(), 1);
    Assert.assertEquals(events.get(0).getLevel(), Level.INFO);
    Assert.assertEquals(events.get(0).getMessage(), "count 7 for widget");
  }

  public void testLevelThresholdSuppressesLowerSeverity () {

    Logger logger = new Logger("test.logger.threshold");
    logger.setLevel(Level.WARN);

    logger.debug("dropped");
    logger.info("dropped");
    logger.error("kept");

    List<RecordingLoggerAdapter.Event> events = RecordingLoggerAdapter.getEvents();
    Assert.assertEquals(events.size(), 1);
    Assert.assertEquals(events.get(0).getMessage(), "kept");
  }

  public void testSuppressedSupplierIsNeverInvoked () {

    Logger logger = new Logger("test.logger.lazysupplier");
    logger.setLevel(Level.INFO);
    AtomicInteger invocations = new AtomicInteger();

    logger.debug(() -> {
      invocations.incrementAndGet();

      return "expensive";
    });

    Assert.assertEquals(invocations.get(), 0);
    Assert.assertTrue(RecordingLoggerAdapter.getEvents().isEmpty());
  }

  public void testEnabledSupplierIsInvokedOnce () {

    Logger logger = new Logger("test.logger.eagersupplier");
    logger.setLevel(Level.INFO);
    AtomicInteger invocations = new AtomicInteger();

    logger.warn(() -> {
      invocations.incrementAndGet();

      return "built";
    });

    Assert.assertEquals(invocations.get(), 1);
    Assert.assertEquals(RecordingLoggerAdapter.getEvents().get(0).getMessage(), "built");
  }

  public void testSuppressedObjectToStringIsNeverInvoked () {

    Logger logger = new Logger("test.logger.lazyobject");
    logger.setLevel(Level.INFO);
    AtomicInteger invocations = new AtomicInteger();

    Object holder = new Object() {

      @Override
      public String toString () {

        invocations.incrementAndGet();

        return "rendered";
      }
    };

    logger.debug(holder);

    Assert.assertEquals(invocations.get(), 0);
  }

  public void testThrowableFirstOrderingIsCarriedThrough () {

    Logger logger = new Logger("test.logger.throwable");
    RuntimeException boom = new RuntimeException("boom");

    logger.error(boom, "failed for %s", "id-1");

    RecordingLoggerAdapter.Event event = RecordingLoggerAdapter.getEvents().get(0);
    Assert.assertEquals(event.getMessage(), "failed for id-1");
    Assert.assertSame(event.getThrowable(), boom);
  }

  public void testOffLevelSuppressesEverything () {

    Logger logger = new Logger("test.logger.off");
    logger.setLevel(Level.OFF);

    logger.fatal("still dropped");

    Assert.assertTrue(RecordingLoggerAdapter.getEvents().isEmpty());
  }

  public void testParameterStoreRoundTrip () {

    Logger logger = new Logger("test.logger.params");

    logger.putParameter("requestId", "abc-123");
    logger.putParameter("tenant", "acme");

    Parameter[] parameters = logger.getParameters();
    Assert.assertEquals(parameters.length, 2);
    Assert.assertEquals(parameters[0].getKey(), "requestId");
    Assert.assertEquals(parameters[0].getValue(), "abc-123");

    logger.removeParameter("requestId");
    Assert.assertEquals(logger.getParameters().length, 1);

    logger.clearParameters();
    Assert.assertEquals(logger.getParameters().length, 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSettingNullLevelThrows () {

    new Logger("test.logger.nulllevel").setLevel(null);
  }

  public void testNameReflectsConstruction () {

    Assert.assertEquals(new Logger("an.explicit.name").getName(), "an.explicit.name");
    Assert.assertEquals(new Logger(LoggerTest.class).getName(), LoggerTest.class.getCanonicalName());
  }
}
