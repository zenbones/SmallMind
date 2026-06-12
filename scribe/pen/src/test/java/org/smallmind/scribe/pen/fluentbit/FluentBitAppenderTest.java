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
package org.smallmind.scribe.pen.fluentbit;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerException;
import org.smallmind.scribe.pen.RecordFixture;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class FluentBitAppenderTest {

  private long readLongField (FluentBitAppender appender, String fieldName)
    throws ReflectiveOperationException {

    Field field = FluentBitAppender.class.getDeclaredField(fieldName);

    field.setAccessible(true);

    return field.getLong(appender);
  }

  private int readIntField (FluentBitAppender appender, String fieldName)
    throws ReflectiveOperationException {

    Field field = FluentBitAppender.class.getDeclaredField(fieldName);

    field.setAccessible(true);

    return field.getInt(appender);
  }

  public void testSetterClamping ()
    throws ReflectiveOperationException {

    FluentBitAppender appender = new FluentBitAppender("test-fluentbit");

    appender.setRetryAttempts(0);
    appender.setConcurrencyLimit(0);
    appender.setBatchSize(0);
    appender.setBatchGracePeriodMilliseconds(10L);

    Assert.assertEquals(readIntField(appender, "retryAttempts"), 1);
    Assert.assertEquals(readIntField(appender, "concurrencyLimit"), 1);
    Assert.assertEquals(readIntField(appender, "batchSize"), 1);
    Assert.assertEquals(readLongField(appender, "batchGracePeriodMilliseconds"), 1000L);
  }

  public void testSetterClampingHonorsLargerValues ()
    throws ReflectiveOperationException {

    FluentBitAppender appender = new FluentBitAppender("test-fluentbit");

    appender.setRetryAttempts(5);
    appender.setConcurrencyLimit(3);
    appender.setBatchSize(50);
    appender.setBatchGracePeriodMilliseconds(5000L);

    Assert.assertEquals(readIntField(appender, "retryAttempts"), 5);
    Assert.assertEquals(readIntField(appender, "concurrencyLimit"), 3);
    Assert.assertEquals(readIntField(appender, "batchSize"), 50);
    Assert.assertEquals(readLongField(appender, "batchGracePeriodMilliseconds"), 5000L);
  }

  private void markClosed (FluentBitAppender appender)
    throws ReflectiveOperationException {

    Field field = FluentBitAppender.class.getDeclaredField("closed");

    field.setAccessible(true);
    ((AtomicBoolean)field.get(appender)).set(true);
  }

  @Test(expectedExceptions = LoggerException.class)
  public void testHandleOutputAfterCloseThrows ()
    throws Exception {

    FluentBitAppender appender = new FluentBitAppender("test-fluentbit");

    // We never call afterPropertiesSet(), which would start worker threads and open TCP sockets to a
    // live Fluent Bit collector. The closed-state guard is independent of that machinery, so we flip the
    // closed flag directly (close() itself would dereference the null finish latch without a started
    // worker pool) and assert that a closed appender rejects records.
    markClosed(appender);

    appender.handleOutput(new RecordFixture().setMessage("rejected"));
  }

  public void testCloseIsIdempotentOnceWorkersAreRunning ()
    throws Exception {

    FluentBitAppender appender = new FluentBitAppender("test-fluentbit");

    // afterPropertiesSet() starts one worker thread that polls the (empty) queue; because no record is
    // ever submitted, the worker performs no network I/O. close() signals the worker to exit and waits
    // for it, and a second close() must be a no-op.
    appender.afterPropertiesSet();
    appender.close();
    appender.close();
  }
}
