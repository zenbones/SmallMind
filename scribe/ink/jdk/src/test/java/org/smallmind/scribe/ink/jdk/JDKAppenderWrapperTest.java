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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.ErrorHandler;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Formatter;
import org.smallmind.scribe.pen.FormattedAppender;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerException;
import org.smallmind.scribe.pen.Record;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JDKAppenderWrapperTest {

  private static class RecordingAppender implements FormattedAppender {

    private final List<Filter> filterList = new ArrayList<>();
    private final List<Record<?>> published = new ArrayList<>();
    private ErrorHandler errorHandler;
    private Formatter formatter;
    private String name;
    private boolean active = true;
    private boolean closed = false;
    private boolean failOnClose = false;
    private boolean interruptOnClose = false;

    @Override
    public String getName () {

      return name;
    }

    @Override
    public void setName (String name) {

      this.name = name;
    }

    @Override
    public Formatter getFormatter () {

      return formatter;
    }

    @Override
    public void setFormatter (Formatter formatter) {

      this.formatter = formatter;
    }

    @Override
    public void setFilter (Filter filter) {

      filterList.clear();
      filterList.add(filter);
    }

    @Override
    public void clearFilters () {

      filterList.clear();
    }

    @Override
    public void addFilter (Filter filter) {

      filterList.add(filter);
    }

    @Override
    public Filter[] getFilters () {

      return filterList.toArray(new Filter[0]);
    }

    @Override
    public void setFilters (List<Filter> filterList) {

      this.filterList.clear();
      this.filterList.addAll(filterList);
    }

    @Override
    public ErrorHandler getErrorHandler () {

      return errorHandler;
    }

    @Override
    public void setErrorHandler (ErrorHandler errorHandler) {

      this.errorHandler = errorHandler;
    }

    @Override
    public boolean isActive () {

      return active;
    }

    @Override
    public void setActive (boolean active) {

      this.active = active;
    }

    @Override
    public void publish (Record<?> record) {

      published.add(record);
    }

    @Override
    public void close ()
      throws InterruptedException, LoggerException {

      if (interruptOnClose) {
        throw new InterruptedException("interrupted");
      }
      if (failOnClose) {
        throw new LoggerException("boom");
      }
      closed = true;
    }

    private List<Record<?>> getPublished () {

      return published;
    }

    private boolean isClosed () {

      return closed;
    }
  }

  private static class PlainAppender implements Appender {

    private final List<Filter> filterList = new ArrayList<>();
    private ErrorHandler errorHandler;
    private String name;
    private boolean active = true;

    @Override
    public String getName () {

      return name;
    }

    @Override
    public void setName (String name) {

      this.name = name;
    }

    @Override
    public void setFilter (Filter filter) {

      filterList.clear();
      filterList.add(filter);
    }

    @Override
    public void clearFilters () {

      filterList.clear();
    }

    @Override
    public void addFilter (Filter filter) {

      filterList.add(filter);
    }

    @Override
    public Filter[] getFilters () {

      return filterList.toArray(new Filter[0]);
    }

    @Override
    public void setFilters (List<Filter> filterList) {

      this.filterList.clear();
      this.filterList.addAll(filterList);
    }

    @Override
    public ErrorHandler getErrorHandler () {

      return errorHandler;
    }

    @Override
    public void setErrorHandler (ErrorHandler errorHandler) {

      this.errorHandler = errorHandler;
    }

    @Override
    public boolean isActive () {

      return active;
    }

    @Override
    public void setActive (boolean active) {

      this.active = active;
    }

    @Override
    public void publish (Record<?> record) {

    }

    @Override
    public void close () {

    }
  }

  private Record<LogRecord> buildRecord (String message) {

    return new JDKRecordSubverter("jdk.wrapper.logger", Level.INFO, null, null, message).getRecord();
  }

  public void testPublishForwardsUnwrappedRecordWhenActive () {

    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);
    Record<LogRecord> record = buildRecord("hi");

    wrapper.publish((LogRecord)record.getNativeLogEntry());

    Assert.assertEquals(appender.getPublished().size(), 1);
    Assert.assertSame(appender.getPublished().get(0), record);
  }

  public void testPublishSuppressedWhenInactive () {

    RecordingAppender appender = new RecordingAppender();
    appender.setActive(false);
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    wrapper.publish((LogRecord)buildRecord("hi").getNativeLogEntry());

    Assert.assertTrue(appender.getPublished().isEmpty());
  }

  public void testFlushIsNoOp () {

    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    wrapper.flush();

    Assert.assertTrue(appender.getPublished().isEmpty());
    Assert.assertFalse(appender.isClosed());
  }

  public void testCloseDelegatesToAppender () {

    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    wrapper.close();

    Assert.assertTrue(appender.isClosed());
  }

  public void testCloseWrapsLoggerExceptionInSecurityException () {

    RecordingAppender appender = new RecordingAppender();
    appender.failOnClose = true;
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    try {
      wrapper.close();
      Assert.fail("Expected SecurityException");
    } catch (SecurityException securityException) {
      Assert.assertTrue(securityException.getCause() instanceof LoggerException);
    }
  }

  public void testCloseWrapsInterruptedExceptionInSecurityException () {

    RecordingAppender appender = new RecordingAppender();
    appender.interruptOnClose = true;
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    try {
      wrapper.close();
      Assert.fail("Expected SecurityException");
    } catch (SecurityException securityException) {
      Assert.assertTrue(securityException.getCause() instanceof InterruptedException);
    }
  }

  public void testIsLoggableTrueWhenAllFiltersPass () {

    RecordingAppender appender = new RecordingAppender();
    appender.addFilter(new JDKFilterAdapter((record) -> true));
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    Assert.assertTrue(wrapper.isLoggable((LogRecord)buildRecord("hi").getNativeLogEntry()));
  }

  public void testIsLoggableTrueWhenNoFilters () {

    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    Assert.assertTrue(wrapper.isLoggable((LogRecord)buildRecord("hi").getNativeLogEntry()));
  }

  public void testIsLoggableFalseWhenFilterVetoes () {

    RecordingAppender appender = new RecordingAppender();
    appender.addFilter(new JDKFilterAdapter((record) -> false));
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    Assert.assertFalse(wrapper.isLoggable((LogRecord)buildRecord("hi").getNativeLogEntry()));
  }

  public void testIsLoggableThrowsForNonJDKFilter () {

    RecordingAppender appender = new RecordingAppender();
    appender.addFilter((record) -> true);
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    try {
      wrapper.isLoggable((LogRecord)buildRecord("hi").getNativeLogEntry());
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
    }
  }

  public void testGetFormatterReturnsNativeWhenJDKAdapterInstalled () {

    java.util.logging.Formatter nativeFormatter = new java.util.logging.SimpleFormatter();
    RecordingAppender appender = new RecordingAppender();
    appender.setFormatter(new JDKFormatterAdapter(nativeFormatter));
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    Assert.assertSame(wrapper.getFormatter(), nativeFormatter);
  }

  public void testGetFormatterReturnsNullWhenNoneConfigured () {

    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    Assert.assertNull(wrapper.getFormatter());
  }

  public void testGetFormatterReturnsNullWhenAppenderNotFormatted () {

    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(new PlainAppender());

    Assert.assertNull(wrapper.getFormatter());
  }

  public void testGetFormatterThrowsForNonJDKFormatter () {

    RecordingAppender appender = new RecordingAppender();
    appender.setFormatter(new Formatter() {

      @Override
      public String format (Record<?> record) {

        return record.getMessage();
      }
    });
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    try {
      wrapper.getFormatter();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
    }
  }

  public void testSetFormatterInstallsAdapter () {

    java.util.logging.Formatter nativeFormatter = new java.util.logging.SimpleFormatter();
    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    wrapper.setFormatter(nativeFormatter);

    Assert.assertTrue(appender.getFormatter() instanceof JDKFormatterAdapter);
    Assert.assertSame(wrapper.getFormatter(), nativeFormatter);
  }

  public void testSetFormatterThrowsWhenAppenderNotFormatted () {

    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(new PlainAppender());

    try {
      wrapper.setFormatter(new java.util.logging.SimpleFormatter());
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
    }
  }

  public void testGetFilterReturnsNativeWhenJDKAdapterInstalled () {

    java.util.logging.Filter nativeFilter = (record) -> true;
    RecordingAppender appender = new RecordingAppender();
    appender.addFilter(new JDKFilterAdapter(nativeFilter));
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    Assert.assertSame(wrapper.getFilter(), nativeFilter);
  }

  public void testGetFilterReturnsNullWhenNoneConfigured () {

    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    Assert.assertNull(wrapper.getFilter());
  }

  public void testGetFilterThrowsForNonJDKFilter () {

    RecordingAppender appender = new RecordingAppender();
    appender.addFilter((record) -> true);
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    try {
      wrapper.getFilter();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
    }
  }

  public void testSetFilterReplacesFiltersWithAdapter () {

    java.util.logging.Filter nativeFilter = (record) -> true;
    RecordingAppender appender = new RecordingAppender();
    appender.addFilter(new JDKFilterAdapter((record) -> false));
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    wrapper.setFilter(nativeFilter);

    Assert.assertEquals(appender.getFilters().length, 1);
    Assert.assertTrue(appender.getFilters()[0] instanceof JDKFilterAdapter);
    Assert.assertSame(wrapper.getFilter(), nativeFilter);
  }

  public void testGetErrorManagerReturnsNativeWhenJDKAdapterInstalled () {

    ErrorManager nativeErrorManager = new ErrorManager();
    RecordingAppender appender = new RecordingAppender();
    appender.setErrorHandler(new JDKErrorHandlerAdapter(nativeErrorManager));
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    Assert.assertSame(wrapper.getErrorManager(), nativeErrorManager);
  }

  public void testGetErrorManagerReturnsNullWhenNoneConfigured () {

    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    Assert.assertNull(wrapper.getErrorManager());
  }

  public void testGetErrorManagerThrowsForNonJDKErrorHandler () {

    RecordingAppender appender = new RecordingAppender();
    appender.setErrorHandler(new ErrorHandler() {

      @Override
      public void process (String loggerName, Throwable throwable, String errorMessage, Object... args) {

      }

      @Override
      public void process (Record<?> record, Throwable throwable, String errorMessage, Object... args) {

      }
    });
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    try {
      wrapper.getErrorManager();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
    }
  }

  public void testSetErrorManagerInstallsAdapter () {

    ErrorManager nativeErrorManager = new ErrorManager();
    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(appender);

    wrapper.setErrorManager(nativeErrorManager);

    Assert.assertTrue(appender.getErrorHandler() instanceof JDKErrorHandlerAdapter);
    Assert.assertSame(wrapper.getErrorManager(), nativeErrorManager);
  }

  public void testGetEncodingUnsupported () {

    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(new RecordingAppender());

    try {
      wrapper.getEncoding();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
    }
  }

  public void testSetEncodingUnsupported () {

    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(new RecordingAppender());

    try {
      wrapper.setEncoding("UTF-8");
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
    }
  }

  public void testGetLevelUnsupported () {

    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(new RecordingAppender());

    try {
      wrapper.getLevel();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
    }
  }

  public void testSetLevelUnsupported () {

    JDKAppenderWrapper wrapper = new JDKAppenderWrapper(new RecordingAppender());

    try {
      wrapper.setLevel(java.util.logging.Level.INFO);
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException unsupportedOperationException) {
    }
  }

  public void testEqualsAndHashCodeDelegateToInnerAppender () {

    RecordingAppender appender = new RecordingAppender();
    JDKAppenderWrapper wrapperOne = new JDKAppenderWrapper(appender);
    JDKAppenderWrapper wrapperTwo = new JDKAppenderWrapper(appender);

    Assert.assertEquals(wrapperOne, wrapperTwo);
    Assert.assertEquals(wrapperOne.hashCode(), appender.hashCode());
    Assert.assertTrue(wrapperOne.equals(appender));
  }
}
