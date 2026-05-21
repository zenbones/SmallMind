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
package org.smallmind.bayeux.oumuamua.server.spi.extension;

import java.util.HashMap;
import java.util.Map;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.NumberValue;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxMessage;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TimesyncExtensionTest {

  private static final String STORED_ATTRIBUTE = "org.smallmind.bayeux.oumuamua.extension.timesync.value";

  private OrthodoxValueFactory factory;
  private Session<OrthodoxValue> session;
  private Map<String, Object> attrs;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    factory = new OrthodoxValueFactory();
    session = Mockito.mock(Session.class);
    attrs = new HashMap<>();

    Mockito.when(session.getId()).thenReturn("alice");
    Mockito.when(session.getAttribute(Mockito.anyString())).thenAnswer(inv -> attrs.get(inv.<String>getArgument(0)));
    Mockito.doAnswer(inv -> {
      attrs.put(inv.getArgument(0), inv.getArgument(1));

      return null;
    }).when(session).setAttribute(Mockito.anyString(), Mockito.any());
  }

  private Message<OrthodoxValue> message (String id) {

    Message<OrthodoxValue> message = new OrthodoxMessage(null, factory);

    if (id != null) {
      message.put(Message.ID, id);
    }

    return message;
  }

  private Message<OrthodoxValue> withTimesync (Message<OrthodoxValue> message, long tc, long l, long o) {

    ObjectValue<OrthodoxValue> sync = factory.objectValue();

    sync.put("tc", tc);
    sync.put("l", l);
    sync.put("o", o);
    message.getExt(true).put("timesync", sync);

    return message;
  }

  public void testRequestStoresTimesyncOnHandshake () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> handshake = withTimesync(message("m1"), 1000L, 5L, 0L);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, handshake));

    Assert.assertNotNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testRequestIgnoredWithoutTimesync () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, message("m1")));

    Assert.assertNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testRequestIgnoredForUnrelatedRoute ()
    throws Exception {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> message = withTimesync(message("m1"), 1000L, 5L, 0L);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", new DefaultRoute("/foo"), message));

    Assert.assertNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testResponseStampsTimesyncWhenIdMatches () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    Message<OrthodoxValue> handshakeRequest = withTimesync(message("m1"), 1000L, 5L, 0L);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, handshakeRequest));

    Message<OrthodoxValue> handshakeResponse = message("m1");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, handshakeResponse));

    ObjectValue<OrthodoxValue> ext_ = handshakeResponse.getExt();

    Assert.assertNotNull(ext_);

    ObjectValue<OrthodoxValue> sync = (ObjectValue<OrthodoxValue>)ext_.get("timesync");

    Assert.assertNotNull(sync);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)sync.get("tc")).asLong(), 1000L);
    Assert.assertNotNull(sync.get("ts"));
    Assert.assertNotNull(sync.get("p"));
    Assert.assertNotNull(sync.get("a"));
  }

  public void testResponseSkippedWhenIdMismatch () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    Message<OrthodoxValue> handshakeRequest = withTimesync(message("m1"), 1000L, 5L, 0L);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, handshakeRequest));

    Message<OrthodoxValue> handshakeResponse = message("different");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, handshakeResponse));
    Assert.assertNull(handshakeResponse.getExt());
  }

  public void testResponseSkippedWithoutStoredTimesync () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    Message<OrthodoxValue> handshakeResponse = message("m1");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, handshakeResponse));
    Assert.assertNull(handshakeResponse.getExt());
  }

  public void testRequestSkipsMessagesWithoutTimesyncEntry () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    Message<OrthodoxValue> firstNoTimesync = message("m1");

    firstNoTimesync.getExt(true).put("other", "marker");

    Message<OrthodoxValue> secondWithTimesync = withTimesync(message("m2"), 1000L, 5L, 0L);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, new Message[] {firstNoTimesync, secondWithTimesync}));

    // First message has ext but no timesync entry, so the loop continues past it; the second
    // message's timesync is found and stored.
    Message<OrthodoxValue> response = message("m2");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, response));

    ObjectValue<OrthodoxValue> sync = (ObjectValue<OrthodoxValue>)response.getExt().get("timesync");

    Assert.assertNotNull(sync);
    Assert.assertEquals(((NumberValue<OrthodoxValue>)sync.get("tc")).asLong(), 1000L);
  }

  public void testRequestStopsAtFirstTimesyncEntry () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> first = withTimesync(message("m1"), 1000L, 5L, 0L);
    Message<OrthodoxValue> second = withTimesync(message("m2"), 9999L, 7L, 1L);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, new Message[] {first, second}));

    // The loop breaks after the first message with a timesync entry, so the second message's
    // timesync is ignored even though its tc would otherwise have superseded the first.
    Message<OrthodoxValue> response = message("m1");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, response));

    ObjectValue<OrthodoxValue> sync = (ObjectValue<OrthodoxValue>)response.getExt().get("timesync");

    Assert.assertEquals(((NumberValue<OrthodoxValue>)sync.get("tc")).asLong(), 1000L);
  }

  public void testResponseStampsOnlyMessageWithMatchingId () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, withTimesync(message("m1"), 1000L, 5L, 0L)));

    Message<OrthodoxValue> noMatch = message("other");
    Message<OrthodoxValue> match = message("m1");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, new Message[] {noMatch, match}));

    Assert.assertNull(noMatch.getExt());
    Assert.assertNotNull(match.getExt());
    Assert.assertNotNull(match.getExt().get("timesync"));
  }

  public void testNewerRequestSupersedesOlder () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, withTimesync(message("m1"), 1000L, 5L, 0L)));

    Message<OrthodoxValue> newer = withTimesync(message("m2"), 2000L, 7L, 1L);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, newer));

    Message<OrthodoxValue> response = message("m2");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, response));

    ObjectValue<OrthodoxValue> sync = (ObjectValue<OrthodoxValue>)response.getExt().get("timesync");

    Assert.assertEquals(((NumberValue<OrthodoxValue>)sync.get("tc")).asLong(), 2000L);
  }

  public void testRequestIgnoredWhenMessageHasNoId () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> noId = withTimesync(message(null), 1000L, 5L, 0L);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, noId));

    Assert.assertNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testRequestIgnoredWhenTimesyncEntryIsNotObject () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> badType = message("m1");

    badType.getExt(true).put("timesync", "not-an-object");

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, badType));

    Assert.assertNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testRequestIgnoredWhenTcMissing () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> message = message("m1");
    ObjectValue<OrthodoxValue> sync = factory.objectValue();

    sync.put("l", 5L);
    sync.put("o", 0L);
    message.getExt(true).put("timesync", sync);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, message));

    Assert.assertNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testRequestIgnoredWhenTcWrongType () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> message = message("m1");
    ObjectValue<OrthodoxValue> sync = factory.objectValue();

    sync.put("tc", "not-a-number");
    sync.put("l", 5L);
    sync.put("o", 0L);
    message.getExt(true).put("timesync", sync);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, message));

    Assert.assertNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testRequestIgnoredWhenLMissing () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> message = message("m1");
    ObjectValue<OrthodoxValue> sync = factory.objectValue();

    sync.put("tc", 1000L);
    sync.put("o", 0L);
    message.getExt(true).put("timesync", sync);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, message));

    Assert.assertNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testRequestIgnoredWhenOMissing () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> message = message("m1");
    ObjectValue<OrthodoxValue> sync = factory.objectValue();

    sync.put("tc", 1000L);
    sync.put("l", 5L);
    message.getExt(true).put("timesync", sync);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, message));

    Assert.assertNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testOlderTcDoesNotOverwriteNewerStored () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, withTimesync(message("first"), 5000L, 5L, 0L)));
    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, withTimesync(message("second"), 4000L, 7L, 1L)));

    Message<OrthodoxValue> firstResponse = message("first");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.HANDSHAKE_ROUTE, firstResponse));

    ObjectValue<OrthodoxValue> sync = (ObjectValue<OrthodoxValue>)firstResponse.getExt().get("timesync");

    Assert.assertEquals(((NumberValue<OrthodoxValue>)sync.get("tc")).asLong(), 5000L);
  }

  public void testRequestStoresOnConnectRoute () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();
    Message<OrthodoxValue> connect = withTimesync(message("connect-1"), 1234L, 5L, 0L);

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.CONNECT_ROUTE, connect));

    Assert.assertNotNull(attrs.get(STORED_ATTRIBUTE));
  }

  public void testResponseStampsOnConnectRoute () {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.CONNECT_ROUTE, withTimesync(message("c1"), 1234L, 5L, 0L)));

    Message<OrthodoxValue> response = message("c1");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", DefaultRoute.CONNECT_ROUTE, response));

    Assert.assertNotNull(response.getExt());
    Assert.assertNotNull(response.getExt().get("timesync"));
  }

  public void testResponseSkippedOnUnrelatedRoute ()
    throws Exception {

    TimesyncExtension<OrthodoxValue> ext = new TimesyncExtension<>();

    ext.onRequest(session, new Packet<>(PacketType.REQUEST, "alice", DefaultRoute.HANDSHAKE_ROUTE, withTimesync(message("m1"), 1000L, 5L, 0L)));

    Message<OrthodoxValue> response = message("m1");

    ext.onResponse(session, new Packet<>(PacketType.RESPONSE, "alice", new DefaultRoute("/other"), response));

    Assert.assertNull(response.getExt());
  }
}
