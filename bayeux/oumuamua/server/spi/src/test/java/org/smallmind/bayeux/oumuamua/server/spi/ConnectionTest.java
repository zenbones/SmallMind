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
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.SessionState;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the {@link Connection#process} default method, covering the branches that do not require
 * full meta-channel dispatch: missing-session-id, invalid-client-id, invalid-session-type,
 * disconnected-session, handshake-with-disconnected-created-session, suppressed-by-server-request,
 * and the hijack-on-connect path.
 */
@Test(groups = "unit")
public class ConnectionTest {

  private OrthodoxCodec codec;
  private Server<OrthodoxValue> server;

  @BeforeMethod
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    server = Mockito.mock(Server.class);

    Mockito.when(server.getCodec()).thenReturn(codec);
  }

  private Message<OrthodoxValue> messageWith (String channel, String sessionId) {

    Message<OrthodoxValue> message = codec.create();

    message.put(Message.CHANNEL, channel);

    if (sessionId != null) {
      message.put(Message.SESSION_ID, sessionId);
    }

    return message;
  }

  private StubConnection connection (Session<OrthodoxValue> createdSession, boolean validateResult) {

    return new StubConnection(createdSession, validateResult);
  }

  public void testMissingClientIdForNonHandshakeYieldsErrorPacket () {

    List<Packet<OrthodoxValue>> received = new ArrayList<>();
    StubConnection conn = connection(null, true);

    conn.process(server, (session, packet) -> received.add(packet), new Message[] {messageWith("/meta/connect", null)});

    Assert.assertEquals(received.size(), 1);
    Assert.assertEquals(received.get(0).getPacketType(), PacketType.RESPONSE);
  }

  public void testInvalidClientIdYieldsErrorPacket () {

    Mockito.when(server.getSession("unknown-id")).thenReturn(null);

    List<Packet<OrthodoxValue>> received = new ArrayList<>();
    StubConnection conn = connection(null, true);

    conn.process(server, (session, packet) -> received.add(packet), new Message[] {messageWith("/meta/connect", "unknown-id")});

    Assert.assertEquals(received.size(), 1);
    Assert.assertEquals(received.get(0).getPacketType(), PacketType.RESPONSE);
  }

  public void testInvalidSessionTypeYieldsErrorPacket () {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(server.getSession("sess-1")).thenReturn(session);
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);

    List<Packet<OrthodoxValue>> received = new ArrayList<>();

    connection(null, false).process(server, (s, packet) -> received.add(packet), new Message[] {messageWith("/meta/connect", "sess-1")});

    Assert.assertEquals(received.size(), 1);
    Assert.assertEquals(received.get(0).getPacketType(), PacketType.RESPONSE);
  }

  public void testDisconnectedSessionYieldsErrorPacket () {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(server.getSession("sess-2")).thenReturn(session);
    Mockito.when(session.getState()).thenReturn(SessionState.DISCONNECTED);

    List<Packet<OrthodoxValue>> received = new ArrayList<>();

    connection(null, true).process(server, (s, packet) -> received.add(packet), new Message[] {messageWith("/meta/connect", "sess-2")});

    Assert.assertEquals(received.size(), 1);
    Assert.assertEquals(received.get(0).getPacketType(), PacketType.RESPONSE);
  }

  public void testHandshakeWithDisconnectedCreatedSessionYieldsErrorPacket () {

    Session<OrthodoxValue> newSession = Mockito.mock(Session.class);

    Mockito.when(newSession.getState()).thenReturn(SessionState.DISCONNECTED);
    Mockito.when(newSession.getId()).thenReturn("new-sess");

    List<Packet<OrthodoxValue>> received = new ArrayList<>();

    connection(newSession, true).process(server, (s, packet) -> received.add(packet), new Message[] {messageWith("/meta/handshake", null)});

    Assert.assertEquals(received.size(), 1);
    Assert.assertEquals(received.get(0).getPacketType(), PacketType.RESPONSE);
  }

  public void testHandshakeWithValidSessionAndSuppressedServerRequestProducesNoResponse () {

    Session<OrthodoxValue> newSession = Mockito.mock(Session.class);

    Mockito.when(newSession.getState()).thenReturn(SessionState.INITIALIZED);
    Mockito.when(newSession.getId()).thenReturn("new-sess-2");
    Mockito.when(server.onRequest(Mockito.any(), Mockito.any())).thenReturn(null);

    List<Packet<OrthodoxValue>> received = new ArrayList<>();

    connection(newSession, true).process(server, (s, packet) -> received.add(packet), new Message[] {messageWith("/meta/handshake", null)});

    Assert.assertEquals(received.size(), 0, "Suppressed server request must not produce a response packet");
  }

  public void testConnectMetaTriggersHijackSession () {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(server.getSession("sess-3")).thenReturn(session);
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);
    Mockito.when(session.getId()).thenReturn("sess-3");
    Mockito.when(server.onRequest(Mockito.any(), Mockito.any())).thenReturn(null);

    StubConnection conn = connection(null, true);

    conn.process(server, (s, packet) -> {
    }, new Message[] {messageWith("/meta/connect", "sess-3")});

    Assert.assertTrue(conn.wasHijacked(), "CONNECT must trigger hijackSession");
    Assert.assertTrue(conn.wasUpdated(), "CONNECT must call updateSession");
  }

  public void testDisconnectMetaDoesNotTriggerHijackSession () {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(server.getSession("sess-4")).thenReturn(session);
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);
    Mockito.when(session.getId()).thenReturn("sess-4");
    Mockito.when(server.onRequest(Mockito.any(), Mockito.any())).thenReturn(null);

    StubConnection conn = connection(null, true);

    conn.process(server, (s, packet) -> {
    }, new Message[] {messageWith("/meta/disconnect", "sess-4")});

    Assert.assertFalse(conn.wasHijacked(), "DISCONNECT must not trigger hijackSession");
    Assert.assertTrue(conn.wasUpdated(), "DISCONNECT must still call updateSession");
  }

  public void testSubscribeMetaTriggersHijackSession () {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(server.getSession("sess-sub")).thenReturn(session);
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);
    Mockito.when(session.getId()).thenReturn("sess-sub");
    Mockito.when(server.onRequest(Mockito.any(), Mockito.any())).thenReturn(null);

    StubConnection conn = connection(null, true);

    conn.process(server, (s, packet) -> {
    }, new Message[] {messageWith("/meta/subscribe", "sess-sub")});

    Assert.assertTrue(conn.wasHijacked(), "SUBSCRIBE must trigger hijackSession");
    Assert.assertTrue(conn.wasUpdated(), "SUBSCRIBE must call updateSession");
  }

  public void testPublishMetaUsesPathBasedRoute () {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(server.getSession("sess-pub")).thenReturn(session);
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);
    Mockito.when(session.getId()).thenReturn("sess-pub");
    Mockito.when(server.onRequest(Mockito.any(), Mockito.any())).thenReturn(null);

    StubConnection conn = connection(null, true);

    conn.process(server, (s, packet) -> {
    }, new Message[] {messageWith("/foo/bar", "sess-pub")});

    Assert.assertFalse(conn.wasHijacked(), "PUBLISH must not trigger hijackSession");
    Assert.assertTrue(conn.wasUpdated(), "PUBLISH must still call updateSession");
  }

  public void testInvalidPathInPublishYieldsErrorPacket () {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(server.getSession("sess-bad")).thenReturn(session);
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED);

    List<Packet<OrthodoxValue>> received = new ArrayList<>();

    connection(null, true).process(server, (s, packet) -> received.add(packet), new Message[] {messageWith("/bad?path", "sess-bad")});

    Assert.assertEquals(received.size(), 1);
    Assert.assertEquals(received.get(0).getPacketType(), PacketType.RESPONSE);
  }

  public void testCycleInvokesOnDisconnectWhenSessionLeftDisconnected () {

    Session<OrthodoxValue> session = Mockito.mock(Session.class);

    Mockito.when(server.getSession("sess-dc")).thenReturn(session);
    Mockito.when(session.getState()).thenReturn(SessionState.CONNECTED).thenReturn(SessionState.DISCONNECTED);
    Mockito.when(session.getId()).thenReturn("sess-dc");
    Mockito.when(server.onRequest(Mockito.any(), Mockito.any())).thenReturn(null);

    StubConnection conn = connection(null, true);

    conn.process(server, (s, packet) -> {
    }, new Message[] {messageWith("/meta/disconnect", "sess-dc")});

    Assert.assertTrue(conn.wasDisconnected(), "Session transitioning to DISCONNECTED must trigger onDisconnect");
  }

  private class StubConnection implements Connection<OrthodoxValue> {

    private final Session<OrthodoxValue> createdSession;
    private final boolean validateResult;
    private final AtomicBoolean hijacked = new AtomicBoolean(false);
    private final AtomicBoolean updated = new AtomicBoolean(false);
    private final AtomicBoolean disconnected = new AtomicBoolean(false);
    private final Transport<OrthodoxValue> transport = Mockito.mock(Transport.class);

    StubConnection (Session<OrthodoxValue> createdSession, boolean validateResult) {

      this.createdSession = createdSession;
      this.validateResult = validateResult;
    }

    boolean wasHijacked () {

      return hijacked.get();
    }

    boolean wasUpdated () {

      return updated.get();
    }

    boolean wasDisconnected () {

      return disconnected.get();
    }

    Transport<OrthodoxValue> transport () {

      return transport;
    }

    @Override
    public String getId () {

      return "stub-connection";
    }

    @Override
    public Transport<OrthodoxValue> getTransport () {

      return transport;
    }

    @Override
    public Session<OrthodoxValue> createSession (Server<OrthodoxValue> server) {

      return createdSession;
    }

    @Override
    public boolean validateSession (Session<OrthodoxValue> session) {

      return validateResult;
    }

    @Override
    public void updateSession (Session<OrthodoxValue> session) {

      updated.set(true);
    }

    @Override
    public void hijackSession (Session<OrthodoxValue> session) {

      hijacked.set(true);
    }

    @Override
    public void onDisconnect (Server<OrthodoxValue> server, Session<OrthodoxValue> session) {

      disconnected.set(true);
    }

    @Override
    public void onCleanup () {

    }

    @Override
    public void deliver (Packet<OrthodoxValue> packet) {

    }
  }
}
