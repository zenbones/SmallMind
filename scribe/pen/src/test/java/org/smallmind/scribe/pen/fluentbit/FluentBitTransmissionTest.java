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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.smallmind.scribe.pen.ErrorHandler;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.RecordFixture;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Drives the {@code FluentBitWorker} send path end-to-end against a loopback {@link ServerSocket} that
 * stands in for a Fluent Bit TCP input. The worker only writes the MessagePack-encoded batch and never
 * reads a response, so a bare accepting socket is enough to exercise the real connect-and-transmit logic
 * without Docker — the same loopback strategy the syslog appender test uses for UDP.
 */
@Test(groups = "unit")
public class FluentBitTransmissionTest {

  private ServerSocket serverSocket;
  private int serverPort;

  @BeforeMethod
  public void openServerSocket ()
    throws IOException {

    serverSocket = new ServerSocket(0, 1, InetAddress.getByName("localhost"));
    serverPort = serverSocket.getLocalPort();
  }

  @AfterMethod
  public void closeServerSocket ()
    throws IOException {

    if (serverSocket != null) {
      serverSocket.close();
    }
  }

  public void testWorkerConnectsAndTransmitsBatch ()
    throws Exception {

    CountDownLatch received = new CountDownLatch(1);
    AtomicInteger byteCount = new AtomicInteger();

    Thread serverThread = new Thread(() -> {
      try (Socket client = serverSocket.accept()) {

        InputStream inputStream = client.getInputStream();
        byte[] buffer = new byte[8192];
        int read = inputStream.read(buffer);

        if (read > 0) {
          byteCount.set(read);
          received.countDown();
        }
      } catch (IOException ioException) {
        // the appender close races the accept on teardown; nothing to capture
      }
    });

    serverThread.setDaemon(true);
    serverThread.start();

    FluentBitAppender appender = new FluentBitAppender("loopback-fluentbit");

    appender.setHost("localhost");
    appender.setPort(serverPort);
    appender.setBatchSize(1);
    appender.afterPropertiesSet();

    appender.publish(new RecordFixture().setLevel(Level.INFO).setMessage("loopback-marker").setMillis(System.currentTimeMillis()));

    Assert.assertTrue(received.await(5, TimeUnit.SECONDS), "loopback server received no datagram from the worker");
    Assert.assertTrue(byteCount.get() > 0);

    appender.close();
  }

  public void testGracePeriodFlushTransmitsPartialBatchWithMetadata ()
    throws Exception {

    CountDownLatch received = new CountDownLatch(1);
    AtomicInteger byteCount = new AtomicInteger();

    Thread serverThread = new Thread(() -> {
      try (Socket client = serverSocket.accept()) {

        InputStream inputStream = client.getInputStream();
        byte[] buffer = new byte[8192];
        int read = inputStream.read(buffer);

        if (read > 0) {
          byteCount.set(read);
          received.countDown();
        }
      } catch (IOException ioException) {
        // teardown race; nothing to capture
      }
    });

    serverThread.setDaemon(true);
    serverThread.start();

    Map<String, String> additionalEventData = new HashMap<>();
    additionalEventData.put("env", "prod");

    FluentBitAppender appender = new FluentBitAppender("grace-fluentbit");

    appender.setHost("localhost");
    appender.setPort(serverPort);
    // A batch larger than the single record forces the grace-period branch to flush it instead.
    appender.setBatchSize(10);
    appender.setBatchGracePeriodMilliseconds(1000L);
    appender.setAdditionalEventData(additionalEventData);
    appender.afterPropertiesSet();

    appender.publish(new RecordFixture().setLevel(Level.INFO).setMessage("grace-marker").setMillis(System.currentTimeMillis()));

    Assert.assertTrue(received.await(8, TimeUnit.SECONDS), "grace-period flush never transmitted the partial batch");
    Assert.assertTrue(byteCount.get() > 0);

    appender.close();
  }

  public void testRetryExhaustionIsReportedToErrorHandler ()
    throws Exception {

    int refusedPort;

    try (ServerSocket throwaway = new ServerSocket(0, 1, InetAddress.getByName("localhost"))) {
      refusedPort = throwaway.getLocalPort();
    }
    // The socket is now closed, so connections to refusedPort are refused and every send attempt fails.

    CapturingErrorHandler errorHandler = new CapturingErrorHandler();
    FluentBitAppender appender = new FluentBitAppender("retry-fluentbit", errorHandler);

    appender.setHost("localhost");
    appender.setPort(refusedPort);
    appender.setRetryAttempts(1);
    appender.setBatchSize(1);
    appender.afterPropertiesSet();

    appender.publish(new RecordFixture().setLevel(Level.INFO).setMessage("undeliverable").setMillis(System.currentTimeMillis()));

    Assert.assertTrue(errorHandler.reported.await(10, TimeUnit.SECONDS), "retry exhaustion never reached the error handler");

    appender.close();
  }

  private static class CapturingErrorHandler implements ErrorHandler {

    private final CountDownLatch reported = new CountDownLatch(1);

    @Override
    public void process (String loggerName, Throwable throwable, String errorMessage, Object... args) {

      reported.countDown();
    }

    @Override
    public void process (Record<?> record, Throwable throwable, String errorMessage, Object... args) {

      reported.countDown();
    }
  }
}
