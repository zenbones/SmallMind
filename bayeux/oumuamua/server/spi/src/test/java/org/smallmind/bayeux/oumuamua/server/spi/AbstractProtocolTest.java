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
package org.smallmind.bayeux.oumuamua.server.spi;

import java.util.ArrayList;
import java.util.List;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxMessage;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AbstractProtocolTest {

  private OrthodoxValueFactory factory;
  private TestProtocol protocol;

  @BeforeMethod
  public void beforeMethod () {

    factory = new OrthodoxValueFactory();
    protocol = new TestProtocol();
  }

  private Message<OrthodoxValue> message () {

    return new OrthodoxMessage(null, factory);
  }

  private Message<OrthodoxValue>[] messages (int count) {

    Message<OrthodoxValue>[] array = new Message[count];

    for (int index = 0; index < count; index++) {
      array[index] = message();
    }

    return array;
  }

  public void testOnReceiptFansOutToRegisteredListener () {

    RecordingProtocolListener listener = new RecordingProtocolListener();
    Message<OrthodoxValue>[] incoming = messages(2);

    protocol.addListener(listener);
    protocol.onReceipt(incoming);

    Assert.assertEquals(listener.receiptCalls.size(), 1);
    Assert.assertSame(listener.receiptCalls.getFirst(), incoming);
  }

  public void testOnPublishFansOutToRegisteredListener () {

    RecordingProtocolListener listener = new RecordingProtocolListener();
    Message<OrthodoxValue> originating = message();
    Message<OrthodoxValue> outgoing = message();

    protocol.addListener(listener);
    protocol.onPublish(originating, outgoing);

    Assert.assertEquals(listener.publishCalls.size(), 1);
    Assert.assertSame(listener.publishCalls.getFirst()[0], originating);
    Assert.assertSame(listener.publishCalls.getFirst()[1], outgoing);
  }

  public void testOnDeliveryFansOutToRegisteredListener () {

    RecordingProtocolListener listener = new RecordingProtocolListener();
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, message());

    protocol.addListener(listener);
    protocol.onDelivery(packet);

    Assert.assertEquals(listener.deliveryCalls.size(), 1);
    Assert.assertSame(listener.deliveryCalls.getFirst(), packet);
  }

  public void testMultipleListenersAllReceiveEvents () {

    RecordingProtocolListener first = new RecordingProtocolListener();
    RecordingProtocolListener second = new RecordingProtocolListener();

    protocol.addListener(first);
    protocol.addListener(second);
    protocol.onReceipt(messages(1));

    Assert.assertEquals(first.receiptCalls.size(), 1);
    Assert.assertEquals(second.receiptCalls.size(), 1);
  }

  public void testBareListenerIsFilteredOut () {

    RecordingProtocolListener listener = new RecordingProtocolListener();

    protocol.addListener(new Protocol.Listener<>() {

    });
    protocol.addListener(listener);
    protocol.onReceipt(messages(1));
    protocol.onPublish(message(), message());
    protocol.onDelivery(new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, message()));

    Assert.assertEquals(listener.receiptCalls.size(), 1);
    Assert.assertEquals(listener.publishCalls.size(), 1);
    Assert.assertEquals(listener.deliveryCalls.size(), 1);
  }

  public void testRemoveListenerStopsDelivery () {

    RecordingProtocolListener listener = new RecordingProtocolListener();

    protocol.addListener(listener);
    protocol.onReceipt(messages(1));
    protocol.removeListener(listener);
    protocol.onReceipt(messages(1));

    Assert.assertEquals(listener.receiptCalls.size(), 1);
  }

  public void testRemoveUnregisteredListenerIsNoOp () {

    RecordingProtocolListener never = new RecordingProtocolListener();

    protocol.removeListener(never);
    protocol.onReceipt(messages(1));
  }

  public void testEmptyRegistryDoesNotThrow () {

    protocol.onReceipt(messages(1));
    protocol.onPublish(message(), message());
    protocol.onDelivery(new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, message()));
  }

  private static class TestProtocol extends AbstractProtocol<OrthodoxValue> {

    @Override
    public String getName () {

      return "test";
    }

    @Override
    public boolean isLongPolling () {

      return false;
    }

    @Override
    public long getLongPollTimeoutMilliseconds () {

      return 0L;
    }

    @Override
    public String[] getTransportNames () {

      return new String[0];
    }

    @Override
    public Transport<OrthodoxValue> getTransport (String name) {

      return null;
    }
  }

  private static class RecordingProtocolListener implements Protocol.ProtocolListener<OrthodoxValue> {

    private final List<Message<OrthodoxValue>[]> receiptCalls = new ArrayList<>();
    private final List<Message<OrthodoxValue>[]> publishCalls = new ArrayList<>();
    private final List<Packet<OrthodoxValue>> deliveryCalls = new ArrayList<>();

    @Override
    public void onReceipt (Message<OrthodoxValue>[] incomingMessages) {

      receiptCalls.add(incomingMessages);
    }

    @Override
    public void onPublish (Message<OrthodoxValue> originatingMessage, Message<OrthodoxValue> outgoingMessage) {

      publishCalls.add(new Message[] {originatingMessage, outgoingMessage});
    }

    @Override
    public void onDelivery (Packet<OrthodoxValue> outgoingPacket) {

      deliveryCalls.add(outgoingPacket);
    }
  }
}
