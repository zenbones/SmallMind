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
import org.smallmind.bayeux.oumuamua.server.api.BayeuxService;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MetaServiceTest {

  private OrthodoxCodec codec;
  private Server<OrthodoxValue> server;
  private Session<OrthodoxValue> session;
  private Protocol<OrthodoxValue> protocol;
  private Route route;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod ()
    throws Exception {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    server = Mockito.mock(Server.class);
    session = Mockito.mock(Session.class);
    protocol = Mockito.mock(Protocol.class);
    route = new DefaultRoute("/service/foo");

    Mockito.when(server.getCodec()).thenReturn(codec);
    Mockito.when(session.getId()).thenReturn("alice");
  }

  private Message<OrthodoxValue> request () {

    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "42");
    request.put(Message.SESSION_ID, "alice");
    request.put(Message.CHANNEL, route.getPath());

    return request;
  }

  public void testProcessReturnsErrorPacketWhenServiceMissing ()
    throws Exception {

    Mockito.when(server.getService(route)).thenReturn(null);

    Packet<OrthodoxValue> packet = Meta.SERVICE.process(protocol, route, server, session, request());
    Message<OrthodoxValue> response = packet.getMessages()[0];

    Assert.assertEquals(packet.getPacketType(), PacketType.RESPONSE);
    Assert.assertEquals(packet.getSenderId(), "alice");
    Assert.assertFalse(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "Unknown service");
  }

  public void testProcessDelegatesToRegisteredService ()
    throws Exception {

    @SuppressWarnings("unchecked")
    BayeuxService<OrthodoxValue> service = Mockito.mock(BayeuxService.class);
    Message<OrthodoxValue> incoming = request();
    Packet<OrthodoxValue> serviceResponse = new Packet<>(PacketType.RESPONSE, "alice", route, codec.create());

    Mockito.when(server.getService(route)).thenReturn(service);
    Mockito.when(service.process(protocol, route, server, session, incoming)).thenReturn(serviceResponse);

    Packet<OrthodoxValue> result = Meta.SERVICE.process(protocol, route, server, session, incoming);

    Assert.assertSame(result, serviceResponse);
    Mockito.verify(service).process(protocol, route, server, session, incoming);
  }

  public void testProcessReturnsServicePacketUnchanged ()
    throws Exception {

    @SuppressWarnings("unchecked")
    BayeuxService<OrthodoxValue> service = Mockito.mock(BayeuxService.class);
    Packet<OrthodoxValue> serviceResponse = new Packet<>(PacketType.RESPONSE, "alice", route, codec.create());

    Mockito.when(server.getService(route)).thenReturn(service);
    Mockito.when(service.process(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(serviceResponse);

    Packet<OrthodoxValue> result = Meta.SERVICE.process(protocol, route, server, session, request());

    Assert.assertSame(result, serviceResponse);
  }
}
