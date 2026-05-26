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
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.json.BooleanValue;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.StringValue;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.MetaProcessingException;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MetaStaticsTest {

  private Server<OrthodoxValue> server;

  @BeforeMethod
  public void beforeMethod () {

    server = Mockito.mock(Server.class);
    Mockito.when(server.getCodec()).thenReturn(new OrthodoxCodec(new JaxbDeserializer<>()));
  }

  @Test(expectedExceptions = MetaProcessingException.class)
  public void testFromRejectsNullPath ()
    throws MetaProcessingException {

    Meta.from(null);
  }

  public void testFromResolvesHandshakeRoute ()
    throws MetaProcessingException {

    Assert.assertSame(Meta.from(DefaultRoute.HANDSHAKE_ROUTE.getPath()), Meta.HANDSHAKE);
  }

  public void testFromResolvesConnectRoute ()
    throws MetaProcessingException {

    Assert.assertSame(Meta.from(DefaultRoute.CONNECT_ROUTE.getPath()), Meta.CONNECT);
  }

  public void testFromResolvesDisconnectRoute ()
    throws MetaProcessingException {

    Assert.assertSame(Meta.from(DefaultRoute.DISCONNECT_ROUTE.getPath()), Meta.DISCONNECT);
  }

  public void testFromResolvesSubscribeRoute ()
    throws MetaProcessingException {

    Assert.assertSame(Meta.from(DefaultRoute.SUBSCRIBE_ROUTE.getPath()), Meta.SUBSCRIBE);
  }

  public void testFromResolvesUnsubscribeRoute ()
    throws MetaProcessingException {

    Assert.assertSame(Meta.from(DefaultRoute.UNSUBSCRIBE_ROUTE.getPath()), Meta.UNSUBSCRIBE);
  }

  @Test(expectedExceptions = MetaProcessingException.class)
  public void testFromRejectsUnknownMetaPath ()
    throws MetaProcessingException {

    Meta.from("/meta/unknown");
  }

  public void testFromRoutesServicePathsToServiceConstant ()
    throws MetaProcessingException {

    Assert.assertSame(Meta.from("/service/foo"), Meta.SERVICE);
    Assert.assertSame(Meta.from("/service/nested/path"), Meta.SERVICE);
  }

  public void testFromRoutesUserChannelToPublishConstant ()
    throws MetaProcessingException {

    Assert.assertSame(Meta.from("/foo/bar"), Meta.PUBLISH);
    Assert.assertSame(Meta.from("/anything"), Meta.PUBLISH);
  }

  public void testConstructErrorResponsePopulatesCoreFields () {

    Message<OrthodoxValue> response = Meta.constructErrorResponse(server, "/meta/handshake", "42", "alice", "boom", null);

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.CHANNEL)).asText(), "/meta/handshake");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ID)).asText(), "42");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.SESSION_ID)).asText(), "alice");
    Assert.assertEquals(((StringValue<OrthodoxValue>)response.get(Message.ERROR)).asText(), "boom");
    Assert.assertFalse(((BooleanValue<OrthodoxValue>)response.get(Message.SUCCESSFUL)).asBoolean());
  }

  public void testConstructErrorResponseOmitsAdviceWhenReconnectNull () {

    Message<OrthodoxValue> response = Meta.constructErrorResponse(server, "/meta/handshake", "42", "alice", "boom", null);

    Assert.assertNull(response.get(Message.ADVICE));
  }

  public void testConstructErrorResponseEncodesReconnectAdvice () {

    Message<OrthodoxValue> response = Meta.constructErrorResponse(server, "/meta/handshake", "42", "alice", "boom", Reconnect.HANDSHAKE);

    ObjectValue<OrthodoxValue> advice = response.getAdvice();

    Assert.assertNotNull(advice);
    Assert.assertEquals(((StringValue<OrthodoxValue>)advice.get(Advice.RECONNECT.getField())).asText(), "handshake");
  }

  public void testConstructErrorResponseUsesNoneCodeForReconnectNone () {

    Message<OrthodoxValue> response = Meta.constructErrorResponse(server, "/meta/connect", "1", "alice", "denied", Reconnect.NONE);

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), "none");
  }

  public void testConstructErrorResponseUsesRetryCodeForReconnectRetry () {

    Message<OrthodoxValue> response = Meta.constructErrorResponse(server, "/meta/subscribe", "1", "alice", "later", Reconnect.RETRY);

    Assert.assertEquals(((StringValue<OrthodoxValue>)response.getAdvice().get(Advice.RECONNECT.getField())).asText(), "retry");
  }
}
