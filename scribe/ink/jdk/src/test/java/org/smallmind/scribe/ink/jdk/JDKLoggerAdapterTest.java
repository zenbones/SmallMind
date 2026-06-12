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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.ErrorHandler;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Record;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JDKLoggerAdapterTest {

  private static class CapturingHandler extends Handler {

    private final List<LogRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void publish (LogRecord record) {

      records.add(record);
    }

    @Override
    public void flush () {

    }

    @Override
    public void close () {

    }

    private List<LogRecord> getRecords () {

      return records;
    }
  }

  private static class CapturingAppender implements Appender {

    private final List<Record<?>> records = new ArrayList<>();
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

      records.add(record);
    }

    @Override
    public void close () {

    }

    private List<Record<?>> getRecords () {

      return records;
    }
  }

  private Logger julLogger;
  private CapturingHandler handler;
  private JDKLoggerAdapter adapter;

  @BeforeMethod
  public void before () {

    julLogger = Logger.getLogger("jdk.test." + System.nanoTime());
    julLogger.setUseParentHandlers(false);
    julLogger.setLevel(java.util.logging.Level.ALL);

    handler = new CapturingHandler();
    julLogger.addHandler(handler);

    adapter = new JDKLoggerAdapter(julLogger);
    adapter.setLevel(Level.TRACE);
  }

  public void testGetNameDelegatesToUnderlyingLogger () {

    Assert.assertEquals(adapter.getName(), julLogger.getName());
  }

  public void testSetLevelTranslatesToJULLevel () {

    adapter.setLevel(Level.WARN);
    Assert.assertEquals(julLogger.getLevel(), java.util.logging.Level.WARNING);
    Assert.assertEquals(adapter.getLevel(), Level.WARN);
  }

  public void testLogMessageFormatRoutesToHandler () {

    adapter.logMessage(Level.INFO, null, "hi %s", "there");

    Assert.assertEquals(handler.getRecords().size(), 1);

    LogRecord captured = handler.getRecords().get(0);
    Assert.assertEquals(captured.getLoggerName(), julLogger.getName());
    Assert.assertEquals(captured.getMessage(), "hi %s");
    Assert.assertEquals(captured.getLevel(), java.util.logging.Level.INFO);
  }

  public void testLogMessageObjectRoutesToHandler () {

    adapter.logMessage(Level.ERROR, null, Integer.valueOf(42));

    Assert.assertEquals(handler.getRecords().size(), 1);

    LogRecord captured = handler.getRecords().get(0);
    Assert.assertEquals(captured.getMessage(), "42");
    Assert.assertEquals(captured.getLevel(), java.util.logging.Level.SEVERE);
  }

  public void testLogMessageSupplierRoutesToHandler () {

    adapter.logMessage(Level.DEBUG, null, () -> "lazy message");

    Assert.assertEquals(handler.getRecords().size(), 1);
    Assert.assertEquals(handler.getRecords().get(0).getMessage(), "lazy message");
  }

  public void testOffLevelDoesNotRoute () {

    adapter.logMessage(Level.OFF, null, "should not appear");

    Assert.assertTrue(handler.getRecords().isEmpty());
  }

  public void testBelowThresholdDoesNotRoute () {

    adapter.setLevel(Level.WARN);
    adapter.logMessage(Level.INFO, null, "below threshold");

    Assert.assertTrue(handler.getRecords().isEmpty());
  }

  public void testClearAppendersRemovesHandlers () {

    adapter.clearAppenders();

    Assert.assertEquals(julLogger.getHandlers().length, 0);
  }

  public void testGetLevelDefaultsToInfoWhenJULLevelNull () {

    julLogger.setLevel(null);

    Assert.assertEquals(adapter.getLevel(), Level.INFO);
  }

  public void testSetLevelTraceRoundTrips () {

    adapter.setLevel(Level.TRACE);

    Assert.assertEquals(julLogger.getLevel(), java.util.logging.Level.FINER);
    Assert.assertEquals(adapter.getLevel(), Level.TRACE);
  }

  public void testSetLevelDebugRoundTrips () {

    adapter.setLevel(Level.DEBUG);

    Assert.assertEquals(julLogger.getLevel(), java.util.logging.Level.FINE);
    Assert.assertEquals(adapter.getLevel(), Level.DEBUG);
  }

  public void testSetLevelInfoRoundTrips () {

    adapter.setLevel(Level.INFO);

    Assert.assertEquals(julLogger.getLevel(), java.util.logging.Level.INFO);
    Assert.assertEquals(adapter.getLevel(), Level.INFO);
  }

  public void testSetLevelErrorRoundTrips () {

    adapter.setLevel(Level.ERROR);

    Assert.assertEquals(julLogger.getLevel(), java.util.logging.Level.SEVERE);
    Assert.assertEquals(adapter.getLevel(), Level.ERROR);
  }

  public void testSetLevelFatalMapsToSevere () {

    adapter.setLevel(Level.FATAL);

    Assert.assertEquals(julLogger.getLevel(), java.util.logging.Level.SEVERE);
    Assert.assertEquals(adapter.getLevel(), Level.ERROR);
  }

  public void testSetLevelOffRoundTrips () {

    adapter.setLevel(Level.OFF);

    Assert.assertEquals(julLogger.getLevel(), java.util.logging.Level.OFF);
    Assert.assertEquals(adapter.getLevel(), Level.OFF);
  }

  public void testAddAppenderRegistersHandler () {

    adapter.clearAppenders();
    adapter.addAppender(new CapturingAppender());

    Assert.assertEquals(julLogger.getHandlers().length, 1);
    Assert.assertTrue(julLogger.getHandlers()[0] instanceof JDKAppenderWrapper);
  }

  public void testAddedAppenderReceivesPublishedRecord () {

    CapturingAppender capturingAppender = new CapturingAppender();

    adapter.clearAppenders();
    adapter.addAppender(capturingAppender);
    adapter.logMessage(Level.INFO, null, "to scribe appender");

    Assert.assertEquals(capturingAppender.getRecords().size(), 1);
    Assert.assertEquals(capturingAppender.getRecords().get(0).getMessage(), "to scribe appender");
  }

  public void testAddFilterInstallsJULFilterWrapper () {

    adapter.addFilter((record) -> true);

    Assert.assertTrue(julLogger.getFilter() instanceof JDKFilterWrapper);
  }

  public void testClearFiltersRemovesJULFilter () {

    adapter.addFilter((record) -> true);
    adapter.clearFilters();

    Assert.assertNull(julLogger.getFilter());
  }

  public void testVetoingScribeFilterSuppressesRecord () {

    adapter.addFilter((record) -> false);
    adapter.logMessage(Level.INFO, null, "vetoed");

    Assert.assertTrue(handler.getRecords().isEmpty());
  }

  public void testEnhancerIsInvokedBeforePublish () {

    boolean[] enhanced = new boolean[1];

    adapter.addEnhancer((record) -> enhanced[0] = true);
    adapter.logMessage(Level.INFO, null, "original");

    Assert.assertTrue(enhanced[0]);
    Assert.assertEquals(handler.getRecords().size(), 1);
  }

  public void testClearEnhancersStopsInvocation () {

    boolean[] enhanced = new boolean[1];

    adapter.addEnhancer((record) -> enhanced[0] = true);
    adapter.clearEnhancers();
    adapter.logMessage(Level.INFO, null, "original");

    Assert.assertFalse(enhanced[0]);
    Assert.assertEquals(handler.getRecords().size(), 1);
  }

  public void testAutoFillLoggerContextToggle () {

    Assert.assertFalse(adapter.getAutoFillLoggerContext());
    adapter.setAutoFillLoggerContext(true);
    Assert.assertTrue(adapter.getAutoFillLoggerContext());
  }
}
