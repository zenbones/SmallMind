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

import org.apache.logging.log4j.core.LogEvent;
import org.smallmind.scribe.pen.DefaultLoggerContext;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Record;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies that {@link Log4JErrorHandlerAdapter} forwards scribe error reports to the native Log4j2
 * {@link org.apache.logging.log4j.core.ErrorHandler}: the logger-name overload passes a {@code null}
 * event, while the record overload passes the record's native {@link LogEvent}. Both translate the
 * message template before delegating.
 */
@Test(groups = "unit")
public class Log4JErrorHandlerAdapterTest {

  public void testNativeErrorHandlerIsExposed () {

    RecordingErrorHandler nativeHandler = new RecordingErrorHandler();

    Assert.assertSame(new Log4JErrorHandlerAdapter(nativeHandler).getNativeErrorHandler(), nativeHandler);
  }

  public void testLoggerNameOverloadPassesNullEvent () {

    RecordingErrorHandler nativeHandler = new RecordingErrorHandler();
    RuntimeException thrown = new RuntimeException("boom");

    new Log4JErrorHandlerAdapter(nativeHandler).process("logger.name", thrown, "failed %s", "id-1");

    Assert.assertEquals(nativeHandler.calls, 1);
    Assert.assertEquals(nativeHandler.message, "failed id-1");
    Assert.assertNull(nativeHandler.event);
    Assert.assertSame(nativeHandler.throwable, thrown);
  }

  public void testRecordOverloadPassesNativeLogEvent () {

    RecordingErrorHandler nativeHandler = new RecordingErrorHandler();
    RuntimeException thrown = new RuntimeException("boom");
    Log4JRecordSubverter subverter = new Log4JRecordSubverter("logger.name", "logger.class.Name", Level.ERROR, new DefaultLoggerContext(), null, "msg");
    Record<LogEvent> record = subverter.getRecord();

    new Log4JErrorHandlerAdapter(nativeHandler).process(record, thrown, "broke");

    Assert.assertEquals(nativeHandler.calls, 1);
    Assert.assertEquals(nativeHandler.message, "broke");
    Assert.assertSame(nativeHandler.event, record.getNativeLogEntry());
    Assert.assertSame(nativeHandler.throwable, thrown);
  }

  /**
   * Records the most recent three-argument {@code error} call so the delegation contract can be asserted.
   */
  private static class RecordingErrorHandler implements org.apache.logging.log4j.core.ErrorHandler {

    private LogEvent event;
    private Throwable throwable;
    private String message;
    private int calls;

    @Override
    public void error (String message) {

      this.message = message;
      calls++;
    }

    @Override
    public void error (String message, Throwable throwable) {

      this.message = message;
      this.throwable = throwable;
      calls++;
    }

    @Override
    public void error (String message, LogEvent event, Throwable throwable) {

      this.message = message;
      this.event = event;
      this.throwable = throwable;
      calls++;
    }
  }
}
