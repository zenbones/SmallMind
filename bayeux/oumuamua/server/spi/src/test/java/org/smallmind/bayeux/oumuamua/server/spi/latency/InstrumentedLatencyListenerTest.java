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
package org.smallmind.bayeux.oumuamua.server.spi.latency;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxMessage;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class InstrumentedLatencyListenerTest {

  private OrthodoxValueFactory factory;
  private InstrumentedLatencyListener<OrthodoxValue> listener;

  @BeforeMethod
  public void beforeMethod ()
    throws Exception {

    // Install an empty per-application context so Claxon's Instrument.with() returns the unplugged no-op instrumentation rather than throwing.
    new PerApplicationContext();
    factory = new OrthodoxValueFactory();
    listener = new InstrumentedLatencyListener<>();
  }

  private Message<OrthodoxValue> message () {

    return new OrthodoxMessage(null, factory);
  }

  private long latencyTimestamp (Message<OrthodoxValue> message) {

    ObjectValue<OrthodoxValue> ext = message.getExt();
    ObjectValue<OrthodoxValue> latency = (ObjectValue<OrthodoxValue>)ext.get("latency");

    return ((NumberValue<OrthodoxValue>)latency.get("timestamp")).asLong();
  }

  public void testOnReceiptStampsTimestampOnEachMessage () {

    Message<OrthodoxValue> first = message();
    Message<OrthodoxValue> second = message();
    long before = System.currentTimeMillis();

    listener.onReceipt(new Message[] {first, second});

    long after = System.currentTimeMillis();
    long firstStamp = latencyTimestamp(first);
    long secondStamp = latencyTimestamp(second);

    Assert.assertTrue(firstStamp >= before);
    Assert.assertTrue(firstStamp <= after);
    Assert.assertTrue(secondStamp >= before);
    Assert.assertTrue(secondStamp <= after);
  }

  public void testOnReceiptWithEmptyArrayIsNoOp () {

    listener.onReceipt(new Message[0]);
  }

  public void testOnPublishCopiesLatencyValue () {

    Message<OrthodoxValue> originating = message();
    Message<OrthodoxValue> outgoing = message();

    listener.onReceipt(new Message[] {originating});

    Value<OrthodoxValue> originatingLatency = originating.getExt().get("latency");

    listener.onPublish(originating, outgoing);

    Assert.assertSame(outgoing.getExt().get("latency"), originatingLatency);
  }

  public void testOnPublishIsNoOpWhenOriginatingHasNoExt () {

    Message<OrthodoxValue> originating = message();
    Message<OrthodoxValue> outgoing = message();

    listener.onPublish(originating, outgoing);

    Assert.assertNull(outgoing.getExt());
  }

  public void testOnPublishIsNoOpWhenLatencyMissingFromExt () {

    Message<OrthodoxValue> originating = message();
    Message<OrthodoxValue> outgoing = message();

    originating.getExt(true).put("other", "value");

    listener.onPublish(originating, outgoing);

    Assert.assertNull(outgoing.getExt());
  }

  public void testOnPublishIgnoresNonObjectLatency () {

    Message<OrthodoxValue> originating = message();
    Message<OrthodoxValue> outgoing = message();

    originating.getExt(true).put("latency", "not-an-object");

    listener.onPublish(originating, outgoing);

    Assert.assertNull(outgoing.getExt());
  }

  public void testOnDeliveryWithStampedMessagesDoesNotThrow () {

    Message<OrthodoxValue> first = message();
    Message<OrthodoxValue> second = message();

    listener.onReceipt(new Message[] {first, second});

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, new Message[] {first, second});

    listener.onDelivery(packet);
  }

  public void testOnDeliveryWithoutExtIsNoOp () {

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, message());

    listener.onDelivery(packet);
  }

  public void testOnDeliveryWithoutLatencyIsNoOp () {

    Message<OrthodoxValue> msg = message();

    msg.getExt(true).put("other", "value");

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, msg);

    listener.onDelivery(packet);
  }

  public void testOnDeliveryWithoutTimestampIsNoOp () {

    Message<OrthodoxValue> msg = message();

    msg.getExt(true).put("latency", factory.objectValue());

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, msg);

    listener.onDelivery(packet);
  }

  public void testOnDeliveryWithFutureTimestampIsNoOp () {

    Message<OrthodoxValue> msg = message();
    ObjectValue<OrthodoxValue> latency = factory.objectValue();

    latency.put("timestamp", System.currentTimeMillis() + 60_000L);
    msg.getExt(true).put("latency", latency);

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, msg);

    listener.onDelivery(packet);
  }

  public void testOnDeliveryWithBackboneRemoteFlagDoesNotThrow () {

    Message<OrthodoxValue> msg = message();

    listener.onReceipt(new Message[] {msg});

    ObjectValue<OrthodoxValue> backbone = factory.objectValue();

    backbone.put("remote", true);
    backbone.put("type", "kafka");
    msg.getExt().put("backbone", backbone);

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, msg);

    listener.onDelivery(packet);
  }

  public void testOnPublishGuardsAgainstWrongLatencyType () {

    Message<OrthodoxValue> originating = message();
    Message<OrthodoxValue> outgoing = message();

    ObjectValue<OrthodoxValue> ext = originating.getExt(true);

    ext.put("latency", factory.numberValue(42));

    Assert.assertEquals(ext.get("latency").getType(), ValueType.NUMBER);

    listener.onPublish(originating, outgoing);

    Assert.assertNull(outgoing.getExt());
  }

  public void testOnDeliveryGuardsAgainstNonObjectLatency () {

    Message<OrthodoxValue> msg = message();

    msg.getExt(true).put("latency", "not-an-object");

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, msg);

    listener.onDelivery(packet);
  }

  public void testOnDeliveryGuardsAgainstNonNumericTimestamp () {

    Message<OrthodoxValue> msg = message();
    ObjectValue<OrthodoxValue> latency = factory.objectValue();

    latency.put("timestamp", "not-a-number");
    msg.getExt(true).put("latency", latency);

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, msg);

    listener.onDelivery(packet);
  }

  public void testOnDeliveryWithBackboneRemoteFalseLogsLocal () {

    Message<OrthodoxValue> msg = message();

    listener.onReceipt(new Message[] {msg});

    ObjectValue<OrthodoxValue> backbone = factory.objectValue();

    backbone.put("remote", false);
    msg.getExt().put("backbone", backbone);

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, msg);

    listener.onDelivery(packet);
  }

  public void testOnDeliveryWithNonObjectBackboneIsIgnored () {

    Message<OrthodoxValue> msg = message();

    listener.onReceipt(new Message[] {msg});
    msg.getExt().put("backbone", "not-an-object");

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, msg);

    listener.onDelivery(packet);
  }

  public void testOnDeliveryWithNonBooleanRemoteFlagIsIgnored () {

    Message<OrthodoxValue> msg = message();

    listener.onReceipt(new Message[] {msg});

    ObjectValue<OrthodoxValue> backbone = factory.objectValue();

    backbone.put("remote", "not-a-boolean");
    msg.getExt().put("backbone", backbone);

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", DefaultRoute.HANDSHAKE_ROUTE, msg);

    listener.onDelivery(packet);
  }
}
