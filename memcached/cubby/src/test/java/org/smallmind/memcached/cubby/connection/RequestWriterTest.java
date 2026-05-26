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
package org.smallmind.memcached.cubby.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class RequestWriterTest {

  private static final int REQUESTED_SEND_BUFFER = 4096;
  private static final long DRAIN_DEADLINE_MILLIS = 5000L;

  private SocketChannel writerChannel;
  private SocketChannel peerChannel;
  private int sendBufferSize;

  @BeforeMethod
  public void openLoopbackPair ()
    throws IOException {

    ServerSocketChannel server = ServerSocketChannel.open();

    try {
      server.socket().bind(new InetSocketAddress("127.0.0.1", 0));

      writerChannel = SocketChannel.open();
      writerChannel.setOption(StandardSocketOptions.SO_SNDBUF, REQUESTED_SEND_BUFFER);
      writerChannel.connect(server.socket().getLocalSocketAddress());

      peerChannel = server.accept();
    } finally {
      server.close();
    }

    writerChannel.configureBlocking(false);
    peerChannel.configureBlocking(false);

    sendBufferSize = writerChannel.socket().getSendBufferSize();
  }

  @AfterMethod
  public void closeChannels ()
    throws IOException {

    if (writerChannel != null) {
      writerChannel.close();
    }
    if (peerChannel != null) {
      peerChannel.close();
    }
  }

  private byte[] drainPeer (int expectedBytes)
    throws IOException, InterruptedException {

    ByteBuffer buffer = ByteBuffer.allocate(expectedBytes);
    long deadline = System.currentTimeMillis() + DRAIN_DEADLINE_MILLIS;

    while (buffer.position() < expectedBytes) {

      int bytesRead = peerChannel.read(buffer);

      if (bytesRead < 0) {
        throw new IOException("Peer channel closed unexpectedly");
      } else if (bytesRead == 0) {
        if (System.currentTimeMillis() > deadline) {
          throw new IOException("Timed out waiting for peer bytes; got " + buffer.position() + "/" + expectedBytes);
        }
        Thread.sleep(5);
      }
    }

    byte[] received = new byte[expectedBytes];

    buffer.flip();
    buffer.get(received);

    return received;
  }

  public void testFreshWriterIsReadyForCommands ()
    throws IOException {

    RequestWriter writer = new RequestWriter(writerChannel);

    Assert.assertTrue(writer.prepare());
  }

  public void testCompleteCommandFlushesToPeerInOneCycle ()
    throws Exception {

    RequestWriter writer = new RequestWriter(writerChannel);
    byte[] payload = "mn\r\n".getBytes();

    Assert.assertTrue(writer.add(new CommandBuffer(0, payload)));
    writer.write();

    Assert.assertEquals(drainPeer(payload.length), payload);
  }

  public void testCommandLargerThanBufferCarriesOverToNextPrepareCycle ()
    throws Exception {

    RequestWriter writer = new RequestWriter(writerChannel);

    byte[] oversize = new byte[sendBufferSize + 1024];

    Arrays.fill(oversize, (byte)'X');

    Assert.assertFalse(writer.add(new CommandBuffer(0, oversize)));
    writer.write();

    Assert.assertTrue(writer.prepare());
    writer.write();

    Assert.assertEquals(drainPeer(oversize.length), oversize);
  }

  public void testFullBufferDefersFollowingCommandUntilNextCycle ()
    throws Exception {

    RequestWriter writer = new RequestWriter(writerChannel);

    byte[] filler = new byte[sendBufferSize];
    byte[] follow = "AFTER".getBytes();

    Arrays.fill(filler, (byte)'F');

    Assert.assertTrue(writer.add(new CommandBuffer(0, filler)));
    Assert.assertFalse(writer.add(new CommandBuffer(1, follow)));
    writer.write();

    Assert.assertTrue(writer.prepare());
    writer.write();

    byte[] received = drainPeer(filler.length + follow.length);

    Assert.assertEquals(Arrays.copyOfRange(received, 0, filler.length), filler);
    Assert.assertEquals(Arrays.copyOfRange(received, filler.length, received.length), follow);
  }

  public void testCommandSpanningMultipleBufferCyclesIsAssembledOnPeer ()
    throws Exception {

    RequestWriter writer = new RequestWriter(writerChannel);

    byte[] huge = new byte[(sendBufferSize * 2) + 256];

    for (int index = 0; index < huge.length; index++) {
      huge[index] = (byte)(index & 0xFF);
    }

    Assert.assertFalse(writer.add(new CommandBuffer(0, huge)));
    writer.write();

    Assert.assertFalse(writer.prepare());
    writer.write();

    Assert.assertTrue(writer.prepare());
    writer.write();

    Assert.assertEquals(drainPeer(huge.length), huge);
  }
}
