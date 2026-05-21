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
package org.smallmind.bayeux.oumuamua.server.impl;

import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Channel;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ChannelRootTest {

  @SuppressWarnings("unchecked")
  private Server<OrthodoxValue> mockServer () {

    return Mockito.mock(Server.class);
  }

  public void testGetBackboneDelegates () {

    Server<OrthodoxValue> server = mockServer();
    @SuppressWarnings("unchecked") Backbone<OrthodoxValue> backbone = Mockito.mock(Backbone.class);

    Mockito.when(server.getBackbone()).thenReturn(backbone);

    Assert.assertSame(new ChannelRoot<>(server).getBackbone(), backbone);
  }

  public void testGetCodecDelegates () {

    Server<OrthodoxValue> server = mockServer();
    Codec<OrthodoxValue> codec = new OrthodoxCodec(new JaxbDeserializer<>());

    Mockito.when(server.getCodec()).thenReturn(codec);

    Assert.assertSame(new ChannelRoot<>(server).getCodec(), codec);
  }

  public void testIsReflectingDelegates ()
    throws Exception {

    Server<OrthodoxValue> server = mockServer();
    DefaultRoute route = new DefaultRoute("/foo");

    Mockito.when(server.isReflecting(route)).thenReturn(true);

    Assert.assertTrue(new ChannelRoot<>(server).isReflecting(route));
    Mockito.verify(server).isReflecting(route);
  }

  public void testIsStreamingDelegates ()
    throws Exception {

    Server<OrthodoxValue> server = mockServer();
    DefaultRoute route = new DefaultRoute("/foo");

    Mockito.when(server.isStreaming(route)).thenReturn(true);

    Assert.assertTrue(new ChannelRoot<>(server).isStreaming(route));
    Mockito.verify(server).isStreaming(route);
  }

  public void testForwardDelegates ()
    throws Exception {

    Server<OrthodoxValue> server = mockServer();
    @SuppressWarnings("unchecked") Channel<OrthodoxValue> channel = Mockito.mock(Channel.class);
    DefaultRoute route = new DefaultRoute("/foo");
    @SuppressWarnings("unchecked") Message<OrthodoxValue> message = Mockito.mock(Message.class);
    Packet<OrthodoxValue> packet = new Packet<>(PacketType.DELIVERY, null, route, message);

    new ChannelRoot<>(server).forward(channel, packet);

    Mockito.verify(server).forward(channel, packet);
  }
}
