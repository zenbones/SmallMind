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
package org.smallmind.scribe.apache;

import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies that {@link CommonsLogWrapper} faithfully bridges the Apache Commons Logging {@link
 * org.apache.commons.logging.Log} contract onto a scribe {@link Logger} obtained from {@link
 * LoggerManager}. Each test uses a unique logger name to avoid the process-wide static logger cache
 * leaking state across methods.
 */
@Test(groups = "unit")
public class CommonsLogWrapperTest {

  private static final AtomicLong NAME_SEQUENCE = new AtomicLong(0);

  private String uniqueName () {

    return "org.smallmind.scribe.apache.test." + getClass().getSimpleName() + "." + NAME_SEQUENCE.incrementAndGet();
  }

  private CapturingAppender attachCapture (String name, Level level) {

    CapturingAppender capturingAppender = new CapturingAppender();
    Logger logger = LoggerManager.getLogger(name);

    logger.setLevel(level);
    logger.clearAppenders();
    logger.addAppender(capturingAppender);

    return capturingAppender;
  }

  public void testInfoDelegatesMessageAndLevel () {

    String name = uniqueName();
    CapturingAppender capturingAppender = attachCapture(name, Level.TRACE);
    CommonsLogWrapper wrapper = new CommonsLogWrapper(name);

    wrapper.info("a message");

    Assert.assertEquals(capturingAppender.size(), 1);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getLevel(), Level.INFO);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getMessage(), "a message");
    Assert.assertNull(capturingAppender.getOnlyRecord().getThrown());
    Assert.assertEquals(capturingAppender.getOnlyRecord().getLoggerName(), name);
  }

  public void testErrorWithThrowableDelegatesMessageLevelAndThrowable () {

    String name = uniqueName();
    CapturingAppender capturingAppender = attachCapture(name, Level.TRACE);
    CommonsLogWrapper wrapper = new CommonsLogWrapper(name);
    RuntimeException thrown = new RuntimeException("explosion");

    wrapper.error("boom", thrown);

    Assert.assertEquals(capturingAppender.size(), 1);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getLevel(), Level.ERROR);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getMessage(), "boom");
    Assert.assertSame(capturingAppender.getOnlyRecord().getThrown(), thrown);
  }

  public void testDebugDelegatesMessageAndLevel () {

    String name = uniqueName();
    CapturingAppender capturingAppender = attachCapture(name, Level.TRACE);
    CommonsLogWrapper wrapper = new CommonsLogWrapper(name);

    wrapper.debug("debug detail");

    Assert.assertEquals(capturingAppender.size(), 1);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getLevel(), Level.DEBUG);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getMessage(), "debug detail");
  }

  public void testWarnAndFatalDelegateAtTheirLevels () {

    String name = uniqueName();
    CapturingAppender capturingAppender = attachCapture(name, Level.TRACE);
    CommonsLogWrapper wrapper = new CommonsLogWrapper(name);

    wrapper.warn("careful");
    wrapper.fatal("doomed");

    Assert.assertEquals(capturingAppender.size(), 2);
    Assert.assertEquals(capturingAppender.getRecords().get(0).getLevel(), Level.WARN);
    Assert.assertEquals(capturingAppender.getRecords().get(0).getMessage(), "careful");
    Assert.assertEquals(capturingAppender.getRecords().get(1).getLevel(), Level.FATAL);
    Assert.assertEquals(capturingAppender.getRecords().get(1).getMessage(), "doomed");
  }

  public void testTraceWithThrowableDelegatesThrowable () {

    String name = uniqueName();
    CapturingAppender capturingAppender = attachCapture(name, Level.TRACE);
    CommonsLogWrapper wrapper = new CommonsLogWrapper(name);
    IllegalStateException thrown = new IllegalStateException("trace trouble");

    wrapper.trace("traced", thrown);

    Assert.assertEquals(capturingAppender.size(), 1);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getLevel(), Level.TRACE);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getMessage(), "traced");
    Assert.assertSame(capturingAppender.getOnlyRecord().getThrown(), thrown);
  }

  public void testIsEnabledMethodsReflectInfoThreshold () {

    String name = uniqueName();
    LoggerManager.getLogger(name).setLevel(Level.INFO);
    CommonsLogWrapper wrapper = new CommonsLogWrapper(name);

    // Effective level INFO: anything as severe or more severe than INFO is enabled; finer is not.
    Assert.assertFalse(wrapper.isTraceEnabled());
    Assert.assertFalse(wrapper.isDebugEnabled());
    Assert.assertTrue(wrapper.isInfoEnabled());
    Assert.assertTrue(wrapper.isWarnEnabled());
    Assert.assertTrue(wrapper.isErrorEnabled());
    Assert.assertTrue(wrapper.isFatalEnabled());
  }

  public void testIsEnabledMethodsReflectTraceThreshold () {

    String name = uniqueName();
    LoggerManager.getLogger(name).setLevel(Level.TRACE);
    CommonsLogWrapper wrapper = new CommonsLogWrapper(name);

    Assert.assertTrue(wrapper.isTraceEnabled());
    Assert.assertTrue(wrapper.isDebugEnabled());
    Assert.assertTrue(wrapper.isInfoEnabled());
    Assert.assertTrue(wrapper.isWarnEnabled());
    Assert.assertTrue(wrapper.isErrorEnabled());
    Assert.assertTrue(wrapper.isFatalEnabled());
  }

  public void testIsEnabledMethodsReflectErrorThreshold () {

    String name = uniqueName();
    LoggerManager.getLogger(name).setLevel(Level.ERROR);
    CommonsLogWrapper wrapper = new CommonsLogWrapper(name);

    Assert.assertFalse(wrapper.isTraceEnabled());
    Assert.assertFalse(wrapper.isDebugEnabled());
    Assert.assertFalse(wrapper.isInfoEnabled());
    Assert.assertFalse(wrapper.isWarnEnabled());
    Assert.assertTrue(wrapper.isErrorEnabled());
    Assert.assertTrue(wrapper.isFatalEnabled());
  }

  public void testLevelThresholdSuppressesFinerRecords () {

    String name = uniqueName();
    CapturingAppender capturingAppender = attachCapture(name, Level.ERROR);
    CommonsLogWrapper wrapper = new CommonsLogWrapper(name);

    wrapper.info("filtered out");
    wrapper.error("kept");

    Assert.assertEquals(capturingAppender.size(), 1);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getLevel(), Level.ERROR);
    Assert.assertEquals(capturingAppender.getOnlyRecord().getMessage(), "kept");
  }
}
