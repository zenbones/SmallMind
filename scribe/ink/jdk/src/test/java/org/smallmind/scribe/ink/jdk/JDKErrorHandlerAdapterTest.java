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

import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Record;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JDKErrorHandlerAdapterTest {

  private static class RecordingErrorManager extends ErrorManager {

    private String message;
    private Exception exception;
    private int code = Integer.MIN_VALUE;
    private boolean invoked = false;

    @Override
    public void error (String message, Exception exception, int code) {

      this.message = message;
      this.exception = exception;
      this.code = code;
      invoked = true;
    }

    private boolean wasInvoked () {

      return invoked;
    }

    private String getMessage () {

      return message;
    }

    private Exception getException () {

      return exception;
    }

    private int getCode () {

      return code;
    }
  }

  public void testGetNativeErrorManagerReturnsWrappedManager () {

    RecordingErrorManager errorManager = new RecordingErrorManager();
    JDKErrorHandlerAdapter adapter = new JDKErrorHandlerAdapter(errorManager);

    Assert.assertSame(adapter.getNativeErrorManager(), errorManager);
  }

  public void testProcessStringRoutesExceptionToErrorManager () {

    RecordingErrorManager errorManager = new RecordingErrorManager();
    JDKErrorHandlerAdapter adapter = new JDKErrorHandlerAdapter(errorManager);
    Exception exception = new RuntimeException("boom");

    adapter.process("jdk.error.logger", exception, "failed at %s", "stage");

    Assert.assertTrue(errorManager.wasInvoked());
    Assert.assertSame(errorManager.getException(), exception);
    Assert.assertEquals(errorManager.getMessage(), "failed at stage");
    Assert.assertEquals(errorManager.getCode(), 0);
  }

  public void testProcessStringDoesNotRouteNonException () {

    RecordingErrorManager errorManager = new RecordingErrorManager();
    JDKErrorHandlerAdapter adapter = new JDKErrorHandlerAdapter(errorManager);
    Error error = new AssertionError("not an exception");

    adapter.process("jdk.error.logger", error, "failed");

    Assert.assertFalse(errorManager.wasInvoked());
  }

  public void testProcessRecordForwardsLoggerName () {

    RecordingErrorManager errorManager = new RecordingErrorManager();
    JDKErrorHandlerAdapter adapter = new JDKErrorHandlerAdapter(errorManager);
    Exception exception = new IllegalStateException("bad");
    Record<LogRecord> record = new JDKRecordSubverter("jdk.error.record.logger", Level.ERROR, null, exception, "msg").getRecord();

    adapter.process(record, exception, "record failed");

    Assert.assertTrue(errorManager.wasInvoked());
    Assert.assertSame(errorManager.getException(), exception);
    Assert.assertEquals(errorManager.getMessage(), "record failed");
  }
}
