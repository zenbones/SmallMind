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
package org.smallmind.bayeux.oumuamua.server.impl.longpolling;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.Protocols;
import org.smallmind.bayeux.oumuamua.server.spi.Transports;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ServletProtocolTest {

  private ServletProtocol<OrthodoxValue> protocol;

  @BeforeMethod
  public void beforeMethod () {

    protocol = new ServletProtocol<>(30000L);
  }

  public void testGetNameReturnsServlet () {

    Assert.assertEquals(protocol.getName(), Protocols.SERVLET.getName());
    Assert.assertEquals(protocol.getName(), "servlet");
  }

  public void testIsLongPollingReturnsTrue () {

    Assert.assertTrue(protocol.isLongPolling());
  }

  public void testGetLongPollTimeoutReturnsConfiguredValue () {

    Assert.assertEquals(protocol.getLongPollTimeoutMilliseconds(), 30000L);
  }

  public void testGetTransportNamesReturnsSingleLongPollingEntry () {

    String[] names = protocol.getTransportNames();

    Assert.assertNotNull(names);
    Assert.assertEquals(names.length, 1);
    Assert.assertEquals(names[0], Transports.LONG_POLLING.getName());
  }

  public void testGetTransportReturnsLongPollingTransportForMatchingName () {

    Assert.assertNotNull(protocol.getTransport(Transports.LONG_POLLING.getName()));
  }

  public void testGetTransportReturnsNullForUnknownName () {

    Assert.assertNull(protocol.getTransport("websocket"));
    Assert.assertNull(protocol.getTransport("unknown"));
  }

  public void testGetTransportOwnerIsThisProtocol () {

    Assert.assertSame(protocol.getTransport(Transports.LONG_POLLING.getName()).getProtocol(), protocol);
  }

  public void testConstructorWithNullListenersSucceeds () {

    new ServletProtocol<OrthodoxValue>(5000L, null);
  }

  public void testConstructorRegistersSuppliedListeners () {

    Protocol.ProtocolListener<OrthodoxValue> first = new Protocol.ProtocolListener<OrthodoxValue>() {

      @Override
      public void onReceipt (Message<OrthodoxValue>[] incomingMessages) {

      }

      @Override
      public void onPublish (Message<OrthodoxValue> originatingMessage, Message<OrthodoxValue> outgoingMessage) {

      }

      @Override
      public void onDelivery (Packet<OrthodoxValue> outgoingPacket) {

      }
    };

    Protocol.ProtocolListener<OrthodoxValue> second = new Protocol.ProtocolListener<OrthodoxValue>() {

      @Override
      public void onReceipt (Message<OrthodoxValue>[] incomingMessages) {

      }

      @Override
      public void onPublish (Message<OrthodoxValue> originatingMessage, Message<OrthodoxValue> outgoingMessage) {

      }

      @Override
      public void onDelivery (Packet<OrthodoxValue> outgoingPacket) {

      }
    };

    ServletProtocol<OrthodoxValue> listenerProtocol = new ServletProtocol<>(1000L, new Protocol.ProtocolListener[] {first, second});

    Assert.assertNotNull(listenerProtocol.getTransport(Transports.LONG_POLLING.getName()), "Listener-array constructor must still produce a usable protocol");
  }
}
