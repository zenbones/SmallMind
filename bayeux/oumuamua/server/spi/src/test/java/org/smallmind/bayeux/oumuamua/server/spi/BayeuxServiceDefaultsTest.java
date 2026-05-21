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

import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.BayeuxService;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Verifies {@link BayeuxService#createResponse(Route, Server, Session, Message)}'s default
 * implementation copies the originating channel/id/clientId onto the response message.
 */
@Test(groups = "unit")
public class BayeuxServiceDefaultsTest {

  private OrthodoxCodec codec;
  private Server<OrthodoxValue> server;
  private Session<OrthodoxValue> session;
  private BayeuxService<OrthodoxValue> service;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    server = Mockito.mock(Server.class);
    Mockito.when(server.getCodec()).thenReturn(codec);

    session = Mockito.mock(Session.class);
    Mockito.when(session.getId()).thenReturn("client-001");

    service = new BayeuxService<OrthodoxValue>() {

      @Override
      public Route[] boundRoutes () {

        return new Route[0];
      }

      @Override
      public Packet<OrthodoxValue> process (Protocol<OrthodoxValue> protocol, Route route, Server<OrthodoxValue> server, Session<OrthodoxValue> session, Message<OrthodoxValue> request) {

        return null;
      }
    };
  }

  public void testCreateResponseCopiesChannelIdAndClientId ()
    throws Exception {

    Route route = new DefaultRoute("/service/echo");
    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "abc-1");

    Message<OrthodoxValue> response = service.createResponse(route, server, session, request);

    Assert.assertEquals(response.getChannel(), "/service/echo");
    Assert.assertEquals(response.getId(), "abc-1");
    Assert.assertEquals(response.getSessionId(), "client-001");
  }

  public void testCreateResponseDoesNotMutateRequest ()
    throws Exception {

    Route route = new DefaultRoute("/service/echo");
    Message<OrthodoxValue> request = codec.create();

    request.put(Message.ID, "abc-2");

    int requestSizeBefore = request.size();

    service.createResponse(route, server, session, request);

    Assert.assertEquals(request.size(), requestSizeBefore);
    Assert.assertNull(request.getChannel());
  }

  public void testCreateResponseTolerantOfMissingRequestId ()
    throws Exception {

    Route route = new DefaultRoute("/service/echo");
    Message<OrthodoxValue> request = codec.create();

    Message<OrthodoxValue> response = service.createResponse(route, server, session, request);

    Assert.assertEquals(response.getChannel(), "/service/echo");
    Assert.assertNull(response.getId());
    Assert.assertEquals(response.getSessionId(), "client-001");
  }
}
