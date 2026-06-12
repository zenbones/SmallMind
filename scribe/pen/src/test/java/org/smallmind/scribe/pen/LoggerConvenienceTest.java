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
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Sweeps the full convenience surface of {@link Logger} — the six per-level methods (trace/debug/info/warn/
 * error/fatal) across every argument shape (throwable, printf message, object, supplier, and their
 * throwable-carrying combinations), the generic {@code log} overloads including the null-level fallback, and
 * the pure delegation methods (filters, appenders, enhancers, auto-fill). Each per-level family is driven at
 * {@code Level.TRACE} so nothing is suppressed and the recorded {@link Level} of each call can be verified.
 */
@Test(groups = "unit")
public class LoggerConvenienceTest {

  private static final Level[] LEVELS = {Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL};

  @BeforeMethod
  public void resetSink () {

    RecordingLoggerAdapter.reset();
  }

  private Logger openLogger (String name) {

    Logger logger = new Logger(name);

    logger.setLevel(Level.TRACE);

    return logger;
  }

  private void assertRecordedLevelsInOrder () {

    List<RecordingLoggerAdapter.Event> events = RecordingLoggerAdapter.getEvents();

    Assert.assertEquals(events.size(), LEVELS.length);
    for (int index = 0; index < LEVELS.length; index++) {
      Assert.assertEquals(events.get(index).getLevel(), LEVELS[index]);
    }
  }

  public void testThrowableOnlyOverloadsAcrossLevels () {

    Logger logger = openLogger("convenience.throwable");
    RuntimeException boom = new RuntimeException("boom");

    logger.trace(boom);
    logger.debug(boom);
    logger.info(boom);
    logger.warn(boom);
    logger.error(boom);
    logger.fatal(boom);

    assertRecordedLevelsInOrder();
    for (RecordingLoggerAdapter.Event event : RecordingLoggerAdapter.getEvents()) {
      Assert.assertSame(event.getThrowable(), boom);
    }
  }

  public void testPrintfMessageOverloadsAcrossLevels () {

    Logger logger = openLogger("convenience.message");

    logger.trace("v %d", 1);
    logger.debug("v %d", 2);
    logger.info("v %d", 3);
    logger.warn("v %d", 4);
    logger.error("v %d", 5);
    logger.fatal("v %d", 6);

    assertRecordedLevelsInOrder();
    Assert.assertEquals(RecordingLoggerAdapter.getEvents().get(2).getMessage(), "v 3");
  }

  public void testThrowableMessageOverloadsAcrossLevels () {

    Logger logger = openLogger("convenience.throwablemessage");
    RuntimeException boom = new RuntimeException("boom");

    logger.trace(boom, "id %s", "a");
    logger.debug(boom, "id %s", "b");
    logger.info(boom, "id %s", "c");
    logger.warn(boom, "id %s", "d");
    logger.error(boom, "id %s", "e");
    logger.fatal(boom, "id %s", "f");

    assertRecordedLevelsInOrder();
    RecordingLoggerAdapter.Event event = RecordingLoggerAdapter.getEvents().get(0);
    Assert.assertEquals(event.getMessage(), "id a");
    Assert.assertSame(event.getThrowable(), boom);
  }

  public void testObjectOverloadsAcrossLevels () {

    Logger logger = openLogger("convenience.object");

    logger.trace((Object)"o1");
    logger.debug((Object)"o2");
    logger.info((Object)"o3");
    logger.warn((Object)"o4");
    logger.error((Object)"o5");
    logger.fatal((Object)"o6");

    assertRecordedLevelsInOrder();
    Assert.assertEquals(RecordingLoggerAdapter.getEvents().get(4).getMessage(), "o5");
  }

  public void testSupplierOverloadsAcrossLevels () {

    Logger logger = openLogger("convenience.supplier");

    logger.trace(() -> "s1");
    logger.debug(() -> "s2");
    logger.info(() -> "s3");
    logger.warn(() -> "s4");
    logger.error(() -> "s5");
    logger.fatal(() -> "s6");

    assertRecordedLevelsInOrder();
    Assert.assertEquals(RecordingLoggerAdapter.getEvents().get(5).getMessage(), "s6");
  }

  public void testThrowableObjectOverloadsAcrossLevels () {

    Logger logger = openLogger("convenience.throwableobject");
    RuntimeException boom = new RuntimeException("boom");

    logger.trace(boom, (Object)"o1");
    logger.debug(boom, (Object)"o2");
    logger.info(boom, (Object)"o3");
    logger.warn(boom, (Object)"o4");
    logger.error(boom, (Object)"o5");
    logger.fatal(boom, (Object)"o6");

    assertRecordedLevelsInOrder();
    RecordingLoggerAdapter.Event event = RecordingLoggerAdapter.getEvents().get(1);
    Assert.assertEquals(event.getMessage(), "o2");
    Assert.assertSame(event.getThrowable(), boom);
  }

  public void testThrowableSupplierOverloadsAcrossLevels () {

    Logger logger = openLogger("convenience.throwablesupplier");
    RuntimeException boom = new RuntimeException("boom");

    logger.trace(boom, () -> "s1");
    logger.debug(boom, () -> "s2");
    logger.info(boom, () -> "s3");
    logger.warn(boom, () -> "s4");
    logger.error(boom, () -> "s5");
    logger.fatal(boom, () -> "s6");

    assertRecordedLevelsInOrder();
    RecordingLoggerAdapter.Event event = RecordingLoggerAdapter.getEvents().get(3);
    Assert.assertEquals(event.getMessage(), "s4");
    Assert.assertSame(event.getThrowable(), boom);
  }

  public void testGenericLogOverloadsCarryExplicitLevel () {

    Logger logger = openLogger("convenience.log");
    RuntimeException boom = new RuntimeException("boom");

    logger.log(Level.WARN, boom);
    logger.log(Level.INFO, "msg %d", 7);
    logger.log(Level.ERROR, boom, "msg %s", "x");
    logger.log(Level.DEBUG, (Object)"obj");
    logger.log(Level.FATAL, () -> "sup");
    logger.log(Level.INFO, boom, (Object)"obj2");
    logger.log(Level.WARN, boom, () -> "sup2");

    List<RecordingLoggerAdapter.Event> events = RecordingLoggerAdapter.getEvents();
    Assert.assertEquals(events.size(), 7);
    Assert.assertEquals(events.get(0).getLevel(), Level.WARN);
    Assert.assertEquals(events.get(1).getMessage(), "msg 7");
    Assert.assertEquals(events.get(2).getLevel(), Level.ERROR);
    Assert.assertEquals(events.get(4).getMessage(), "sup");
  }

  public void testNullLevelFallsBackToLoggerLevel () {

    Logger logger = openLogger("convenience.nulllevel");
    logger.setLevel(Level.ERROR);

    logger.log((Level)null, "promoted");

    List<RecordingLoggerAdapter.Event> events = RecordingLoggerAdapter.getEvents();
    Assert.assertEquals(events.size(), 1);
    Assert.assertEquals(events.get(0).getLevel(), Level.ERROR);
  }

  public void testAutoFillLoggerContextRoundTrip () {

    Logger logger = openLogger("convenience.autofill");

    Assert.assertFalse(logger.getAutoFillLoggerContext());
    logger.setAutoFillLoggerContext(true);
    Assert.assertTrue(logger.getAutoFillLoggerContext());
  }

  public void testFilterAppenderEnhancerDelegationDoesNotThrow () {

    Logger logger = openLogger("convenience.delegation");

    logger.addFilter(record -> true);
    logger.addFilters(new Filter[] {record -> true, record -> false});
    logger.clearFilters();

    CapturingAppender appender = new CapturingAppender();
    logger.addAppender(appender);
    logger.addAppenders(new Appender[] {new CapturingAppender()});
    logger.clearAppenders();

    logger.addEnhancer(record -> {
    });
    logger.clearEnhancers();
  }

  public void testUnknownReturnsLiteral () {

    Assert.assertEquals(Logger.unknown(), "unknown");
  }
}
