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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies the {@link AsynchronousAppender} decorator drains its buffer to the wrapped appender on
 * daemon worker threads and rejects publishing after {@code close()}.
 */
@Test(groups = "unit")
public class AsynchronousAppenderTest {

  private void awaitSize (CapturingAppender appender, int expected)
    throws InterruptedException {

    long deadline = System.nanoTime() + (5_000L * 1_000_000L);

    while (appender.size() < expected) {
      if (System.nanoTime() > deadline) {
        Assert.fail("timed out waiting for " + expected + " drained records, saw " + appender.size());
      }
      Thread.sleep(10);
    }
  }

  public void testAllPublishedRecordsReachTheWrappedAppender ()
    throws InterruptedException, LoggerException {

    CapturingAppender delegate = new CapturingAppender();
    AsynchronousAppender asynchronous = new AsynchronousAppender(delegate, 200, 3);

    for (int index = 0; index < 100; index++) {
      asynchronous.publish(new RecordFixture().setMessage("record-" + index));
    }

    awaitSize(delegate, 100);
    asynchronous.close();

    Assert.assertEquals(delegate.size(), 100);
  }

  public void testDefaultConstructorDrainsWithSingleWorker ()
    throws InterruptedException, LoggerException {

    CapturingAppender delegate = new CapturingAppender();
    AsynchronousAppender asynchronous = new AsynchronousAppender(delegate);

    asynchronous.publish(new RecordFixture().setMessage("only"));
    awaitSize(delegate, 1);
    asynchronous.close();

    Assert.assertEquals(delegate.size(), 1);
  }

  public void testPublishAfterCloseDoesNotReachDelegate ()
    throws InterruptedException, LoggerException {

    CapturingAppender delegate = new CapturingAppender();
    AsynchronousAppender asynchronous = new AsynchronousAppender(delegate, 50, 2);

    asynchronous.publish(new RecordFixture().setMessage("before"));
    awaitSize(delegate, 1);
    asynchronous.close();

    asynchronous.publish(new RecordFixture().setMessage("after-close"));
    Thread.sleep(100);

    Assert.assertEquals(delegate.size(), 1);
  }

  public void testBufferOverflowIsRoutedToErrorHandler ()
    throws Exception {

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    BlockingAppender delegate = new BlockingAppender(entered, release);
    AsynchronousAppender asynchronous = new AsynchronousAppender(delegate, 1, 1);
    CapturingErrorHandler errorHandler = new CapturingErrorHandler();

    asynchronous.setErrorHandler(errorHandler);

    // The worker pulls "first" and blocks inside handleOutput, leaving the queue (capacity 1) empty.
    asynchronous.publish(new RecordFixture().setMessage("first"));
    Assert.assertTrue(entered.await(5, TimeUnit.SECONDS), "worker never entered handleOutput");

    // "second" fills the single queue slot; "third" cannot be offered and must surface as an error.
    asynchronous.publish(new RecordFixture().setMessage("second"));
    asynchronous.publish(new RecordFixture().setMessage("third"));

    Assert.assertTrue(errorHandler.await(5, TimeUnit.SECONDS), "buffer overflow was never reported");
    Assert.assertTrue(errorHandler.getThrowable().getMessage().contains("Buffer exceeded"), "unexpected error: " + errorHandler.getThrowable().getMessage());

    release.countDown();
    asynchronous.close();
  }

  /**
   * A delegate that blocks inside {@link #handleOutput(Record)} until released, so the test can drive the
   * asynchronous appender's queue to capacity deterministically.
   */
  private static class BlockingAppender extends AbstractAppender {

    private final CountDownLatch entered;
    private final CountDownLatch release;

    private BlockingAppender (CountDownLatch entered, CountDownLatch release) {

      this.entered = entered;
      this.release = release;
    }

    @Override
    public void handleOutput (Record<?> record)
      throws InterruptedException {

      entered.countDown();
      release.await();
    }
  }

  /**
   * Captures the first reported failure and signals its arrival through a latch.
   */
  private static class CapturingErrorHandler implements ErrorHandler {

    private final CountDownLatch reported = new CountDownLatch(1);
    private volatile Throwable throwable;

    private boolean await (long timeout, TimeUnit unit)
      throws InterruptedException {

      return reported.await(timeout, unit);
    }

    private Throwable getThrowable () {

      return throwable;
    }

    @Override
    public void process (String loggerName, Throwable throwable, String errorMessage, Object... args) {

      this.throwable = throwable;
      reported.countDown();
    }

    @Override
    public void process (Record<?> record, Throwable throwable, String errorMessage, Object... args) {

      this.throwable = throwable;
      reported.countDown();
    }
  }
}
