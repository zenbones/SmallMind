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

import org.apache.logging.log4j.core.Appender;
import org.smallmind.scribe.pen.DefaultLoggerContext;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers {@link Log4JAppenderWrapper}: the permanently-started lifecycle reporting, the active/inactive
 * {@code append} delegation to the wrapped scribe appender, the {@code setHandler} adapter install, the
 * unsupported native accessors, and the equals/hashCode delegation that unwraps the inner appender.
 */
@Test(groups = "unit")
public class Log4JAppenderWrapperTest {

  private Log4JRecordSubverter subverter (String message) {

    return new Log4JRecordSubverter("log4j.wrapper", "log4j.wrapper.Class", Level.INFO, new DefaultLoggerContext(), null, message);
  }

  public void testNameDelegatesToInnerAppender () {

    CapturingAppender capturingAppender = new CapturingAppender();
    capturingAppender.setName("inner.name");

    Assert.assertEquals(new Log4JAppenderWrapper(capturingAppender).getName(), "inner.name");
  }

  public void testLifecycleReportsPermanentlyStarted () {

    Log4JAppenderWrapper wrapper = new Log4JAppenderWrapper(new CapturingAppender());

    Assert.assertFalse(wrapper.ignoreExceptions());
    Assert.assertEquals(wrapper.getState(), Appender.State.STARTED);
    Assert.assertTrue(wrapper.isStarted());
    Assert.assertFalse(wrapper.isStopped());

    wrapper.initialize();
    wrapper.start();
    wrapper.stop();

    Assert.assertTrue(wrapper.isStarted());
  }

  public void testAppendPublishesToActiveScribeAppender () {

    CapturingAppender capturingAppender = new CapturingAppender();
    Log4JAppenderWrapper wrapper = new Log4JAppenderWrapper(capturingAppender);

    wrapper.append(subverter("routed"));

    Assert.assertEquals(capturingAppender.size(), 1);
    Assert.assertEquals(capturingAppender.getRecords().get(0).getMessage(), "routed");
  }

  public void testAppendDropsRecordWhenScribeAppenderInactive () {

    CapturingAppender capturingAppender = new CapturingAppender();
    capturingAppender.setActive(false);
    Log4JAppenderWrapper wrapper = new Log4JAppenderWrapper(capturingAppender);

    wrapper.append(subverter("dropped"));

    Assert.assertEquals(capturingAppender.size(), 0);
  }

  public void testSetHandlerInstallsErrorHandlerAdapterOnInnerAppender () {

    CapturingAppender capturingAppender = new CapturingAppender();
    Log4JAppenderWrapper wrapper = new Log4JAppenderWrapper(capturingAppender);

    wrapper.setHandler(new org.apache.logging.log4j.core.ErrorHandler() {

      @Override
      public void error (String message) {

      }

      @Override
      public void error (String message, Throwable throwable) {

      }

      @Override
      public void error (String message, org.apache.logging.log4j.core.LogEvent event, Throwable throwable) {

      }
    });

    Assert.assertTrue(capturingAppender.getErrorHandler() instanceof Log4JErrorHandlerAdapter);
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testGetHandlerUnsupported () {

    new Log4JAppenderWrapper(new CapturingAppender()).getHandler();
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testGetLayoutUnsupported () {

    new Log4JAppenderWrapper(new CapturingAppender()).getLayout();
  }

  public void testEqualsAndHashCodeUnwrapInnerAppender () {

    CapturingAppender capturingAppender = new CapturingAppender();
    Log4JAppenderWrapper wrapper = new Log4JAppenderWrapper(capturingAppender);

    Assert.assertTrue(wrapper.equals(new Log4JAppenderWrapper(capturingAppender)));
    Assert.assertTrue(wrapper.equals(capturingAppender));
    Assert.assertFalse(wrapper.equals(new Log4JAppenderWrapper(new CapturingAppender())));
    Assert.assertEquals(wrapper.hashCode(), capturingAppender.hashCode());
  }
}
