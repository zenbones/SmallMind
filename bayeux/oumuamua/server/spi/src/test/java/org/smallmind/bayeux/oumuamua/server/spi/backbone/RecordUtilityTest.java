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
package org.smallmind.bayeux.oumuamua.server.spi.backbone;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxMessage;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class RecordUtilityTest {

  private OrthodoxCodec codec;
  private OrthodoxValueFactory factory;

  @BeforeMethod
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    factory = new OrthodoxValueFactory();
  }

  private Message<OrthodoxValue> message (String channel) {

    Message<OrthodoxValue> message = new OrthodoxMessage(null, factory);

    message.put(Message.CHANNEL, channel);

    return message;
  }

  public void testRoundTripPreservesNodeNameAndPath ()
    throws Exception {

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", new DefaultRoute("/foo/bar"), message("/foo/bar"));

    byte[] buffer = RecordUtility.serialize("node-one", packet);
    DebonedPacket<OrthodoxValue> deboned = RecordUtility.deserialize(codec, buffer);

    Assert.assertEquals(deboned.getNodeName(), "node-one");
    Assert.assertEquals(deboned.getPacket().getRoute().getPath(), "/foo/bar");
    Assert.assertEquals(deboned.getPacket().getPacketType(), PacketType.DELIVERY);
  }

  public void testRoundTripPreservesAllMessages ()
    throws Exception {

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", new DefaultRoute("/foo"), new Message[] {message("/foo"), message("/foo")});

    byte[] buffer = RecordUtility.serialize("node-one", packet);
    DebonedPacket<OrthodoxValue> deboned = RecordUtility.deserialize(codec, buffer);

    Assert.assertEquals(deboned.getPacket().getMessages().length, 2);
    Assert.assertEquals(deboned.getPacket().getMessages()[0].getChannel(), "/foo");
    Assert.assertEquals(deboned.getPacket().getMessages()[1].getChannel(), "/foo");
  }

  public void testDeserializedMessagesCarryBackboneAnnotation ()
    throws Exception {

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", new DefaultRoute("/foo"), message("/foo"));

    byte[] buffer = RecordUtility.serialize("node-one", packet);
    DebonedPacket<OrthodoxValue> deboned = RecordUtility.deserialize(codec, buffer);

    Message<OrthodoxValue> decoded = deboned.getPacket().getMessages()[0];
    ObjectValue<OrthodoxValue> ext = decoded.getExt();

    Assert.assertNotNull(ext);

    ObjectValue<OrthodoxValue> backbone = (ObjectValue<OrthodoxValue>)ext.get("backbone");

    Assert.assertNotNull(backbone);
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)backbone.get("remote")).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)backbone.get("type")).asText(), "kafka");
  }

  public void testRoundTripPreservesMessageContent ()
    throws Exception {

    Message<OrthodoxValue> original = message("/foo");

    original.put("greeting", factory.textValue("hello"));

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", new DefaultRoute("/foo"), original);

    byte[] buffer = RecordUtility.serialize("node-one", packet);
    DebonedPacket<OrthodoxValue> deboned = RecordUtility.deserialize(codec, buffer);

    Message<OrthodoxValue> decoded = deboned.getPacket().getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)decoded.get("greeting")).asText(), "hello");
  }

  public void testRoundTripHandlesNonAsciiNodeNameAndPath ()
    throws Exception {

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", new DefaultRoute("/r~oot"), message("/r~oot"));

    byte[] buffer = RecordUtility.serialize("node-\u00fc", packet);
    DebonedPacket<OrthodoxValue> deboned = RecordUtility.deserialize(codec, buffer);

    Assert.assertEquals(deboned.getNodeName(), "node-\u00fc");
    Assert.assertEquals(deboned.getPacket().getRoute().getPath(), "/r~oot");
  }

  public void testSerializedLayoutBeginsWithNodeNameLength ()
    throws Exception {

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, "alice", new DefaultRoute("/foo"), message("/foo"));

    byte[] buffer = RecordUtility.serialize("xy", packet);

    Assert.assertEquals(buffer[0], 0);
    Assert.assertEquals(buffer[1], 0);
    Assert.assertEquals(buffer[2], 0);
    Assert.assertEquals(buffer[3], 2);
    Assert.assertEquals(buffer[4], (byte)'x');
    Assert.assertEquals(buffer[5], (byte)'y');
  }
}
