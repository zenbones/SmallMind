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
package org.smallmind.bayeux.oumuamua.server.spi.meta;

import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MetaDisconnectTest {

  private OrthodoxCodec codec;
  private Server<OrthodoxValue> server;
  private Session<OrthodoxValue> session;
  private Protocol<OrthodoxValue> protocol;

  @BeforeMethod
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    server = Mockito.mock(Server.class);
    session = Mockito.mock(Session.class);
    protocol = Mockito.mock(Protocol.class);

    Mockito.when(server.getCodec()).thenReturn(codec);
    Mockito.when(session.getId()).thenReturn("alice");
  }

  private Message<OrthodoxValue> request () {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.SESSION_ID, "alice");
    request.put(Message.CHANNEL, DefaultRoute.DISCONNECT_ROUTE.getPath());

    return request;
  }

  public void testProcessAdvancesSessionToDisconnected ()
    throws Exception {

    Meta.DISCONNECT.process(protocol, DefaultRoute.DISCONNECT_ROUTE, server, session, request());

    Mockito.verify(session).completeDisconnect();
  }

  public void testProcessReturnsResponsePacket ()
    throws Exception {

    Packet<OrthodoxValue> packet = Meta.DISCONNECT.process(protocol, DefaultRoute.DISCONNECT_ROUTE, server, session, request());

    Assert.assertEquals(packet.getPacketType(), PacketType.RESPONSE);
    Assert.assertSame(packet.getRoute(), DefaultRoute.DISCONNECT_ROUTE);
    Assert.assertEquals(packet.getSenderId(), "alice");
    Assert.assertEquals(packet.getMessages().length, 1);
  }

  public void testProcessResponseEchoesRequestFields ()
    throws Exception {

    Packet<OrthodoxValue> packet = Meta.DISCONNECT.process(protocol, DefaultRoute.DISCONNECT_ROUTE, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.CHANNEL)).asText(), DefaultRoute.DISCONNECT_ROUTE.getPath());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ID)).asText(), "42");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.SESSION_ID)).asText(), "alice");
    Assert.assertTrue(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testProcessResponseCarriesReconnectNoneAdvice ()
    throws Exception {

    Packet<OrthodoxValue> packet = Meta.DISCONNECT.process(protocol, DefaultRoute.DISCONNECT_ROUTE, server, session, request());
    ObjectValue<OrthodoxValue> advice = packet.getMessages()[0].getAdvice();

    Assert.assertNotNull(advice);
    Assert.assertEquals(((StringValue<OrthodoxValue>)advice.get(Advice.RECONNECT.getField())).asText(), Reconnect.NONE.getCode());
  }
}
