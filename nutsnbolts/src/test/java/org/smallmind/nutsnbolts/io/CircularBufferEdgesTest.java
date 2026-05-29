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
import java.nio.channels.AsynchronousCloseException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CircularBufferEdgesTest {

  public void testReadWritePositionWrapsAroundCapacity ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(8);

    buffer.write("123456".getBytes(StandardCharsets.UTF_8));
    byte[] firstRead = new byte[4];
    buffer.read(firstRead);
    Assert.assertEquals(new String(firstRead, StandardCharsets.UTF_8), "1234");

    buffer.write("abcdef".getBytes(StandardCharsets.UTF_8));

    byte[] tail = new byte[8];
    int read = buffer.read(tail);

    Assert.assertEquals(read, 8);
    Assert.assertEquals(new String(tail, StandardCharsets.UTF_8), "56abcdef");
  }

  public void testWriteAvailableAtFullIsZero ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(4);

    buffer.write(new byte[] {1, 2, 3, 4});

    Assert.assertEquals(buffer.writeAvailable(), 0);
    Assert.assertEquals(buffer.readAvailable(), 4);
  }

  public void testReadAvailableAtEmptyAfterDrainIsZero ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(4);

    buffer.write(new byte[] {1, 2});
    byte[] sink = new byte[2];
    buffer.read(sink);

    Assert.assertEquals(buffer.readAvailable(), 0);
    Assert.assertEquals(buffer.writeAvailable(), 4);
  }

  public void testSkipDiscardsBytesAndAdvancesReadCursor ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(8);

    buffer.write("abcdef".getBytes(StandardCharsets.UTF_8));

    long skipped = buffer.skip(3);

    Assert.assertEquals(skipped, 3);

    byte[] sink = new byte[3];
    buffer.read(sink);
    Assert.assertEquals(new String(sink, StandardCharsets.UTF_8), "def");
  }

  public void testReadWithZeroLengthReturnsZero ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(4);

    Assert.assertEquals(buffer.read(new byte[4], 0, 0), 0);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testReadWithNullBufferThrows ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(4);

    buffer.read(null, 0, 1);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testReadWithNegativeOffsetThrows ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(4);

    buffer.read(new byte[4], -1, 1);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testReadBeyondBufferEndThrows ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(4);

    buffer.read(new byte[4], 2, 5);
  }

  public void testReadAvailableAfterCloseThrows () {

    CircularBuffer buffer = new CircularBuffer(4);

    buffer.close();

    try {
      buffer.readAvailable();
      Assert.fail("Expected SynchronousCloseException");
    } catch (IOException expected) {

      Assert.assertEquals(expected.getClass().getSimpleName(), "SynchronousCloseException");
    }
  }

  public void testCloseIsIdempotent () {

    CircularBuffer buffer = new CircularBuffer(4);

    buffer.close();
    buffer.close();

    Assert.assertTrue(buffer.isClosed());
  }

  public void testCloseUnblocksWaitingReaderWithAsynchronousCloseException ()
    throws InterruptedException {

    CircularBuffer buffer = new CircularBuffer(4);
    AtomicReference<Throwable> caught = new AtomicReference<>();

    Thread reader = new Thread(() -> {
      try {
        buffer.read(new byte[1]);
      } catch (Throwable throwable) {
        caught.set(throwable);
      }
    });
    reader.start();

    Thread.sleep(50);
    buffer.close();
    reader.join(2_000);

    Assert.assertTrue(caught.get() instanceof AsynchronousCloseException, "Expected AsynchronousCloseException, got " + caught.get());
  }

  public void testReadHonoursTimeoutWhenNoDataAvailable ()
    throws IOException, InterruptedException {

    CircularBuffer buffer = new CircularBuffer(4);

    long start = System.currentTimeMillis();
    int read = buffer.read(new byte[2], 100);
    long elapsed = System.currentTimeMillis() - start;

    Assert.assertEquals(read, 0);
    Assert.assertTrue(elapsed >= 90, "Timeout did not elapse, elapsed=" + elapsed);
  }
}
