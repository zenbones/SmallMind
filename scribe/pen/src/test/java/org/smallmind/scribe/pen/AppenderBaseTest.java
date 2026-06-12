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

import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the appender skeletons that every concrete appender inherits: {@link AbstractAppender}'s
 * filter chain, active flag, error-handler routing and {@code publish} veto/dispatch logic; the
 * pure delegation in {@link AbstractWrappedAppender}; and {@link AbstractFormattedAppender}'s
 * format-then-emit bridge including the no-formatter failure.
 */
@Test(groups = "unit")
public class AppenderBaseTest {

  public void testNameAndActiveAccessors () {

    CapturingAppender appender = new CapturingAppender();

    Assert.assertNull(appender.getName());
    appender.setName("sink");
    Assert.assertEquals(appender.getName(), "sink");

    Assert.assertTrue(appender.isActive());
    appender.setActive(false);
    Assert.assertFalse(appender.isActive());
  }

  public void testFilterChainSetAddClearAndSetFilters () {

    CapturingAppender appender = new CapturingAppender();
    Filter first = record -> true;
    Filter second = record -> true;

    appender.addFilter(first);
    appender.addFilter(second);
    Assert.assertEquals(appender.getFilters().length, 2);

    appender.setFilter(first);
    Assert.assertEquals(appender.getFilters().length, 1);
    Assert.assertSame(appender.getFilters()[0], first);

    appender.setFilters(Arrays.asList(first, second));
    Assert.assertEquals(appender.getFilters().length, 2);

    appender.clearFilters();
    Assert.assertEquals(appender.getFilters().length, 0);
  }

  public void testPublishDeliversWhenFiltersPass () {

    CapturingAppender appender = new CapturingAppender();

    appender.addFilter(record -> true);
    appender.publish(new RecordFixture().setMessage("kept"));

    Assert.assertEquals(appender.size(), 1);
  }

  public void testPublishVetoedByFilterIsDropped () {

    CapturingAppender appender = new CapturingAppender();

    appender.addFilter(record -> false);
    appender.publish(new RecordFixture().setMessage("dropped"));

    Assert.assertEquals(appender.size(), 0);
  }

  public void testHandleOutputFailureIsRoutedToErrorHandler () {

    RecordingErrorHandler errorHandler = new RecordingErrorHandler();
    ThrowingAppender appender = new ThrowingAppender();

    appender.setErrorHandler(errorHandler);
    appender.publish(new RecordFixture().setMessage("boom"));

    Assert.assertSame(appender.getErrorHandler(), errorHandler);
    Assert.assertEquals(errorHandler.recordFailures, 1);
  }

  public void testHandleOutputFailureWithNoErrorHandlerDoesNotPropagate () {

    ThrowingAppender appender = new ThrowingAppender();

    // No error handler is set, so the failure falls to the default printStackTrace branch and must
    // not escape publish().
    appender.publish(new RecordFixture().setMessage("boom"));

    Assert.assertNull(appender.getErrorHandler());
  }

  public void testHandleErrorDefaultMethodsRouteToHandler () {

    RecordingErrorHandler errorHandler = new RecordingErrorHandler();
    CapturingAppender appender = new CapturingAppender();

    appender.setErrorHandler(errorHandler);
    appender.handleError("logger.name", new RuntimeException("named"));
    appender.handleError(new RecordFixture(), new RuntimeException("recorded"));

    Assert.assertEquals(errorHandler.loggerFailures, 1);
    Assert.assertEquals(errorHandler.recordFailures, 1);
  }

  public void testWrappedAppenderDelegatesEveryOperation ()
    throws Exception {

    CapturingAppender inner = new CapturingAppender();
    PassThroughWrappedAppender wrapper = new PassThroughWrappedAppender(inner);
    Filter filter = record -> true;
    RecordingErrorHandler errorHandler = new RecordingErrorHandler();

    wrapper.setName("wrapped");
    Assert.assertEquals(inner.getName(), "wrapped");
    Assert.assertEquals(wrapper.getName(), "wrapped");

    wrapper.addFilter(filter);
    Assert.assertEquals(wrapper.getFilters().length, 1);
    wrapper.setFilter(filter);
    Assert.assertEquals(inner.getFilters().length, 1);
    wrapper.setFilters(Arrays.asList(filter, filter));
    Assert.assertEquals(inner.getFilters().length, 2);
    wrapper.clearFilters();
    Assert.assertEquals(inner.getFilters().length, 0);

    wrapper.setErrorHandler(errorHandler);
    Assert.assertSame(inner.getErrorHandler(), errorHandler);
    Assert.assertSame(wrapper.getErrorHandler(), errorHandler);

    wrapper.setActive(false);
    Assert.assertFalse(inner.isActive());
    Assert.assertFalse(wrapper.isActive());

    wrapper.publishToWrappedAppender(new RecordFixture().setMessage("forwarded"));
    Assert.assertEquals(inner.size(), 1);

    wrapper.close();
  }

  public void testFormattedAppenderEmitsFormattedString ()
    throws Exception {

    RecordingFormattedAppender appender = new RecordingFormattedAppender();

    appender.setFormatter(new PatternFormatter("%m"));
    Assert.assertNotNull(appender.getFormatter());

    appender.handleOutput(new RecordFixture().setMessage("hello"));

    Assert.assertEquals(appender.lastOutput, "hello" + System.lineSeparator());
  }

  @Test(expectedExceptions = LoggerException.class)
  public void testFormattedAppenderWithoutFormatterThrows ()
    throws Exception {

    new RecordingFormattedAppender().handleOutput(new RecordFixture().setMessage("no-formatter"));
  }

  private static class ThrowingAppender extends AbstractAppender {

    @Override
    public void handleOutput (Record<?> record)
      throws Exception {

      throw new Exception("output failed");
    }
  }

  private static class PassThroughWrappedAppender extends AbstractWrappedAppender {

    private PassThroughWrappedAppender (Appender internalAppender) {

      super(internalAppender);
    }

    @Override
    public void publish (Record<?> record) {

      publishToWrappedAppender(record);
    }
  }

  private static class RecordingFormattedAppender extends AbstractFormattedAppender {

    private String lastOutput;

    @Override
    public void handleOutput (String output) {

      lastOutput = output;
    }
  }

  private static class RecordingErrorHandler implements ErrorHandler {

    private int loggerFailures;
    private int recordFailures;

    @Override
    public void process (String loggerName, Throwable throwable, String errorMessage, Object... args) {

      loggerFailures++;
    }

    @Override
    public void process (Record<?> record, Throwable throwable, String errorMessage, Object... args) {

      recordFailures++;
    }
  }
}
