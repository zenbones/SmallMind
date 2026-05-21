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
package org.smallmind.bayeux.oumuamua.server.spi.json;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxMessage;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PacketUtilityTest {

  private OrthodoxValueFactory factory;

  @BeforeMethod
  public void beforeMethod () {

    factory = new OrthodoxValueFactory();
  }

  private Message<OrthodoxValue> message (String channel) {

    Message<OrthodoxValue> message = new OrthodoxMessage(null, factory);

    message.put(Message.CHANNEL, channel);

    return message;
  }

  public void testMergeAppendsByDefault ()
    throws Exception {

    Packet<OrthodoxValue> base = new Packet<>(PacketType.RESPONSE, "sender", DefaultRoute.CONNECT_ROUTE, message("/meta/connect"));
    Packet<OrthodoxValue> other = new Packet<>(PacketType.RESPONSE, "sender", new DefaultRoute("/foo"), new Message[] {message("/foo"), message("/bar")});

    Packet<OrthodoxValue> merged = PacketUtility.merge(base, other, null, false);

    Assert.assertEquals(merged.getMessages().length, 3);
    Assert.assertEquals(merged.getMessages()[0].getChannel(), "/meta/connect");
    Assert.assertEquals(merged.getMessages()[1].getChannel(), "/foo");
    Assert.assertEquals(merged.getMessages()[2].getChannel(), "/bar");
  }

  public void testMergePrependsAfterSoleBaseMessage ()
    throws Exception {

    Packet<OrthodoxValue> base = new Packet<>(PacketType.RESPONSE, "sender", DefaultRoute.CONNECT_ROUTE, message("/meta/connect"));
    Packet<OrthodoxValue> other = new Packet<>(PacketType.RESPONSE, "sender", new DefaultRoute("/foo"), new Message[] {message("/foo"), message("/bar")});

    Packet<OrthodoxValue> merged = PacketUtility.merge(base, other, null, true);

    Assert.assertEquals(merged.getMessages().length, 3);
    Assert.assertEquals(merged.getMessages()[0].getChannel(), "/meta/connect");
    Assert.assertEquals(merged.getMessages()[1].getChannel(), "/foo");
    Assert.assertEquals(merged.getMessages()[2].getChannel(), "/bar");
  }

  public void testMergePrependsAfterFirstBaseMessage ()
    throws Exception {

    // Per the merge() Javadoc, prepend=true "inserts other messages after position 0 of base".
    // With base=[m0, m1] and other=[o0], the expected layout is [m0, o0, m1].
    Packet<OrthodoxValue> base = new Packet<>(PacketType.RESPONSE, "sender", DefaultRoute.CONNECT_ROUTE, new Message[] {message("/meta/connect"), message("/x")});
    Packet<OrthodoxValue> other = new Packet<>(PacketType.RESPONSE, "sender", new DefaultRoute("/foo"), message("/foo"));

    Packet<OrthodoxValue> merged = PacketUtility.merge(base, other, null, true);

    Assert.assertEquals(merged.getMessages().length, 3);
    Assert.assertEquals(merged.getMessages()[0].getChannel(), "/meta/connect");
    Assert.assertEquals(merged.getMessages()[1].getChannel(), "/foo");
    Assert.assertEquals(merged.getMessages()[2].getChannel(), "/x");
  }

  public void testMergeFiltersByRoute ()
    throws Exception {

    Packet<OrthodoxValue> base = new Packet<>(PacketType.RESPONSE, "sender", DefaultRoute.CONNECT_ROUTE, message("/meta/connect"));
    Packet<OrthodoxValue> other = new Packet<>(PacketType.RESPONSE, "sender", new DefaultRoute("/foo"), new Message[] {message("/meta/connect"), message("/foo")});

    Packet<OrthodoxValue> merged = PacketUtility.merge(base, other, DefaultRoute.CONNECT_ROUTE, false);

    Assert.assertEquals(merged.getMessages().length, 2);
    Assert.assertEquals(merged.getMessages()[0].getChannel(), "/meta/connect");
    Assert.assertEquals(merged.getMessages()[1].getChannel(), "/foo");
  }

  public void testMergeReturnsBaseWhenAllFiltered ()
    throws Exception {

    Packet<OrthodoxValue> base = new Packet<>(PacketType.RESPONSE, "sender", DefaultRoute.CONNECT_ROUTE, message("/meta/connect"));
    Packet<OrthodoxValue> other = new Packet<>(PacketType.RESPONSE, "sender", new DefaultRoute("/foo"), message("/meta/connect"));

    Packet<OrthodoxValue> merged = PacketUtility.merge(base, other, DefaultRoute.CONNECT_ROUTE, false);

    Assert.assertSame(merged, base);
  }

  public void testMergePreservesMetadata ()
    throws Exception {

    Packet<OrthodoxValue> base = new Packet<>(PacketType.RESPONSE, "sender", DefaultRoute.CONNECT_ROUTE, message("/meta/connect"));
    Packet<OrthodoxValue> other = new Packet<>(PacketType.RESPONSE, "another", new DefaultRoute("/foo"), message("/foo"));

    Packet<OrthodoxValue> merged = PacketUtility.merge(base, other, null, false);

    Assert.assertEquals(merged.getPacketType(), base.getPacketType());
    Assert.assertEquals(merged.getSenderId(), base.getSenderId());
    Assert.assertSame(merged.getRoute(), base.getRoute());
  }

  public void testFreezePacketWrapsMessagesInMessageDouble ()
    throws Exception {

    Message<OrthodoxValue> original = message("/foo");
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.RESPONSE, "sender", new DefaultRoute("/foo"), original);

    Packet<OrthodoxValue> frozen = PacketUtility.freezePacket(packet);

    Assert.assertNotSame(frozen, packet);
    Assert.assertTrue(frozen.getMessages()[0] instanceof MessageDouble);
    Assert.assertEquals(frozen.getMessages()[0].getChannel(), "/foo");
  }

  public void testFreezePacketPreservesMetadata ()
    throws Exception {

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.RESPONSE, "sender", new DefaultRoute("/foo"), message("/foo"));
    Packet<OrthodoxValue> frozen = PacketUtility.freezePacket(packet);

    Assert.assertEquals(frozen.getPacketType(), packet.getPacketType());
    Assert.assertEquals(frozen.getSenderId(), packet.getSenderId());
    Assert.assertSame(frozen.getRoute(), packet.getRoute());
    Assert.assertEquals(frozen.getMessages().length, packet.getMessages().length);
  }

  public void testFreezePacketMutationDoesNotAffectOriginal ()
    throws Exception {

    Message<OrthodoxValue> original = message("/foo");
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.RESPONSE, "sender", new DefaultRoute("/foo"), original);

    Packet<OrthodoxValue> frozen = PacketUtility.freezePacket(packet);

    frozen.getMessages()[0].put("added", factory.textValue("v"));

    Assert.assertNull(original.get("added"));
  }

  public void testEncodeProducesJsonArray ()
    throws Exception {

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.RESPONSE, "sender", new DefaultRoute("/foo"), new Message[] {message("/foo"), message("/bar")});

    String encoded = PacketUtility.encode(packet);

    Assert.assertTrue(encoded.startsWith("["));
    Assert.assertTrue(encoded.endsWith("]"));
    Assert.assertTrue(encoded.contains("\"/foo\""));
    Assert.assertTrue(encoded.contains("\"/bar\""));
  }

  public void testEncodeEmptyMessagesProducesEmptyArray ()
    throws Exception {

    Packet<OrthodoxValue> packet = new Packet<>(PacketType.RESPONSE, "s", new DefaultRoute("/foo"), new Message[0]);

    Assert.assertEquals(PacketUtility.encode(packet), "[]");
  }
}
