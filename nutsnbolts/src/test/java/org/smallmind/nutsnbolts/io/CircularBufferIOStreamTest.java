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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CircularBufferIOStreamTest {

  public void testWriteThenReadDeliversBytes ()
    throws IOException, InterruptedException {

    try (CircularBufferIOStream pipe = new CircularBufferIOStream(64)) {

      OutputStream out = pipe.asOutputStream();
      InputStream in = pipe.asInputStream();

      AtomicReference<byte[]> received = new AtomicReference<>();
      Thread consumer = new Thread(() -> {
        try {
          received.set(in.readNBytes(5));
        } catch (IOException ioException) {
          throw new RuntimeException(ioException);
        }
      });
      consumer.start();

      out.write("hello".getBytes(StandardCharsets.UTF_8));
      consumer.join(2_000);

      Assert.assertEquals(received.get(), "hello".getBytes(StandardCharsets.UTF_8));
    }
  }

  public void testCloseWakesBlockedReader ()
    throws InterruptedException {

    CircularBufferIOStream pipe = new CircularBufferIOStream(8);

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread consumer = new Thread(() -> {
      try {
        pipe.asInputStream().read();
      } catch (IOException ioException) {
        failure.set(ioException);
      }
    });
    consumer.start();

    Thread.sleep(50);
    pipe.close();
    consumer.join(2_000);

    Assert.assertNotNull(failure.get(), "Reader should have observed close");
  }

  public void testCircularBufferReadWriteRoundTrip ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(8);
    byte[] payload = "abcdefg".getBytes(StandardCharsets.UTF_8);

    buffer.write(payload);

    byte[] received = new byte[payload.length];
    int read = buffer.read(received);

    Assert.assertEquals(read, payload.length);
    Assert.assertEquals(received, payload);
  }

  public void testCircularBufferAvailability ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(8);

    Assert.assertEquals(buffer.writeAvailable(), 8);
    Assert.assertEquals(buffer.readAvailable(), 0);

    buffer.write(new byte[] {1, 2, 3});

    Assert.assertEquals(buffer.writeAvailable(), 5);
    Assert.assertEquals(buffer.readAvailable(), 3);
  }
}
