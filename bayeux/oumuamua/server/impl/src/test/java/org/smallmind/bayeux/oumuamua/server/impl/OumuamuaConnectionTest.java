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
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.Transport;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit-level coverage for the default methods on {@link OumuamuaConnection}, exercising both the
 * matching and mismatching arms of {@link OumuamuaConnection#validateSession(Session)}
 * (which existing integration tests only hit on the matching arm).
 */
@Test(groups = "unit")
public class OumuamuaConnectionTest {

  @SuppressWarnings("unchecked")
  private OumuamuaConnection<OrthodoxValue> connection (String protocolName, String transportName) {

    Transport<OrthodoxValue> transport = Mockito.mock(Transport.class);
    Protocol<OrthodoxValue> protocol = Mockito.mock(Protocol.class);

    Mockito.when(transport.getName()).thenReturn(transportName);
    Mockito.when(transport.getProtocol()).thenReturn(protocol);
    Mockito.when(protocol.getName()).thenReturn(protocolName);

    return new TestConnection(transport);
  }

  @SuppressWarnings("unchecked")
  private OumuamuaSession<OrthodoxValue> mockSession (String protocolName, String transportName) {

    OumuamuaSession<OrthodoxValue> session = Mockito.mock(OumuamuaSession.class);
    Transport<OrthodoxValue> transport = Mockito.mock(Transport.class);
    Protocol<OrthodoxValue> protocol = Mockito.mock(Protocol.class);

    Mockito.when(transport.getName()).thenReturn(transportName);
    Mockito.when(transport.getProtocol()).thenReturn(protocol);
    Mockito.when(protocol.getName()).thenReturn(protocolName);
    Mockito.when(session.getTransport()).thenReturn(transport);

    return session;
  }

  public void testValidateSessionMatchingProtocolAndTransportReturnsTrue () {

    OumuamuaConnection<OrthodoxValue> conn = connection("websocket", "websocket");
    OumuamuaSession<OrthodoxValue> session = mockSession("websocket", "websocket");

    Assert.assertTrue(conn.validateSession(session));
  }

  public void testValidateSessionMismatchedProtocolReturnsFalse () {

    OumuamuaConnection<OrthodoxValue> conn = connection("servlet", "long-polling");
    OumuamuaSession<OrthodoxValue> session = mockSession("websocket", "long-polling");

    Assert.assertFalse(conn.validateSession(session), "Mismatched protocol name must invalidate the session");
  }

  public void testValidateSessionMismatchedTransportReturnsFalse () {

    OumuamuaConnection<OrthodoxValue> conn = connection("websocket", "websocket");
    OumuamuaSession<OrthodoxValue> session = mockSession("websocket", "long-polling");

    Assert.assertFalse(conn.validateSession(session), "Mismatched transport name must invalidate the session");
  }

  public void testHijackSessionDelegatesToSession () {

    OumuamuaConnection<OrthodoxValue> conn = connection("websocket", "websocket");

    @SuppressWarnings("unchecked")
    OumuamuaSession<OrthodoxValue> session = Mockito.mock(OumuamuaSession.class);

    conn.hijackSession(session);

    Mockito.verify(session).hijack(conn);
  }

  public void testUpdateSessionResetsLastContact () {

    OumuamuaConnection<OrthodoxValue> conn = connection("websocket", "websocket");

    @SuppressWarnings("unchecked")
    OumuamuaSession<OrthodoxValue> session = Mockito.mock(OumuamuaSession.class);

    conn.updateSession(session);

    Mockito.verify(session).contact();
  }

  private static class TestConnection implements OumuamuaConnection<OrthodoxValue> {

    private final Transport<OrthodoxValue> transport;

    private TestConnection (Transport<OrthodoxValue> transport) {

      this.transport = transport;
    }

    @Override
    public String getId () {

      return "test-connection";
    }

    @Override
    public Transport<OrthodoxValue> getTransport () {

      return transport;
    }

    @Override
    public void deliver (Packet<OrthodoxValue> packet) {

    }

    @Override
    public void onCleanup () {

    }
  }
}
