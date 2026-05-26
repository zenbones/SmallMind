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
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.ServerClosedException;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseCode;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ResponseReaderTest {

  private static final long READ_DEADLINE_MILLIS = 5000L;

  private SocketChannel readerChannel;
  private SocketChannel peerChannel;

  @BeforeMethod
  public void openLoopbackPair ()
    throws IOException {

    ServerSocketChannel server = ServerSocketChannel.open();

    try {
      server.socket().bind(new InetSocketAddress("127.0.0.1", 0));

      readerChannel = SocketChannel.open(server.socket().getLocalSocketAddress());
      peerChannel = server.accept();
    } finally {
      server.close();
    }

    readerChannel.configureBlocking(false);
    peerChannel.configureBlocking(false);
  }

  @AfterMethod
  public void closeChannels ()
    throws IOException {

    if (readerChannel != null) {
      readerChannel.close();
    }
    if (peerChannel != null && peerChannel.isOpen()) {
      peerChannel.close();
    }
  }

  private void writeFromPeer (String text)
    throws IOException {

    ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

    while (buffer.hasRemaining()) {
      peerChannel.write(buffer);
    }
  }

  private void awaitReadableBytes (ResponseReader reader)
    throws IOException, InterruptedException {

    long deadline = System.currentTimeMillis() + READ_DEADLINE_MILLIS;

    while (System.currentTimeMillis() < deadline) {
      if (reader.read()) {

        return;
      }
      Thread.sleep(5);
    }

    Assert.fail("Reader saw no bytes within deadline");
  }

  public void testHeaderOnlyResponseIsParsedFromSingleRead ()
    throws Exception {

    ResponseReader reader = new ResponseReader(readerChannel);

    writeFromPeer("MN\r\n");
    awaitReadableBytes(reader);

    Response response = reader.extract();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getCode(), ResponseCode.MN);
  }

  public void testHeaderPlusBodyResponseIsParsedFromSingleRead ()
    throws Exception {

    ResponseReader reader = new ResponseReader(readerChannel);

    writeFromPeer("VA 7\r\npayload\r\n");
    awaitReadableBytes(reader);

    Response response = reader.extract();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "payload");
  }

  public void testHeaderArrivingInFragmentsIsAssembledAcrossReadCycles ()
    throws Exception {

    ResponseReader reader = new ResponseReader(readerChannel);

    writeFromPeer("HD c4");
    awaitReadableBytes(reader);
    Assert.assertNull(reader.extract());

    writeFromPeer("2\r\n");
    awaitReadableBytes(reader);

    Response response = reader.extract();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);
    Assert.assertEquals(response.getCas(), 42L);
  }

  public void testValueBodyArrivingInFragmentsIsAssembledAcrossReadCycles ()
    throws Exception {

    ResponseReader reader = new ResponseReader(readerChannel);

    writeFromPeer("VA 5\r\n");
    awaitReadableBytes(reader);
    Assert.assertNull(reader.extract());

    writeFromPeer("he");
    awaitReadableBytes(reader);
    Assert.assertNull(reader.extract());

    writeFromPeer("llo\r\n");
    awaitReadableBytes(reader);

    Response response = reader.extract();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "hello");
  }

  public void testMultipleResponsesInOneReadAreExtractedSequentially ()
    throws Exception {

    ResponseReader reader = new ResponseReader(readerChannel);

    writeFromPeer("MN\r\nHD c7\r\n");
    awaitReadableBytes(reader);

    Response first = reader.extract();
    Response second = reader.extract();

    Assert.assertEquals(first.getCode(), ResponseCode.MN);
    Assert.assertEquals(second.getCode(), ResponseCode.HD);
    Assert.assertEquals(second.getCas(), 7L);
  }

  public void testPeerClosureSurfacesServerClosedException ()
    throws Exception {

    ResponseReader reader = new ResponseReader(readerChannel);

    peerChannel.close();

    long deadline = System.currentTimeMillis() + READ_DEADLINE_MILLIS;
    boolean observed = false;

    while (System.currentTimeMillis() < deadline) {
      try {
        reader.read();
      } catch (ServerClosedException expected) {
        observed = true;

        break;
      }
      Thread.sleep(5);
    }

    Assert.assertTrue(observed, "Reader did not surface ServerClosedException after peer close");
  }
}
