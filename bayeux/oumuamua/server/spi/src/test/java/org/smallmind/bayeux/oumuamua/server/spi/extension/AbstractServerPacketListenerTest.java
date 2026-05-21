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

import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Route;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Verifies that {@link AbstractServerPacketListener} passes inbound request, response, and
 * delivery packets through unchanged when none of the lifecycle hooks are overridden. This is the
 * contract every subclass relies on for selective extension.
 */
@Test(groups = "unit")
public class AbstractServerPacketListenerTest {

  private static final class PassThroughListener extends AbstractServerPacketListener<OrthodoxValue> {

  }

  private PassThroughListener listener;
  private Session<OrthodoxValue> session;
  private Route route;

  @SuppressWarnings("unchecked")
  @BeforeMethod
  public void beforeMethod () {

    listener = new PassThroughListener();
    session = Mockito.mock(Session.class);
    route = Mockito.mock(Route.class);
  }

  @SuppressWarnings("unchecked")
  private Packet<OrthodoxValue> newPacket (PacketType packetType) {

    return new Packet<>(packetType, "sender-id", route, new Message[0]);
  }

  public void testOnRequestReturnsSamePacketInstance () {

    Packet<OrthodoxValue> packet = newPacket(PacketType.REQUEST);

    Assert.assertSame(listener.onRequest(session, packet), packet);
  }

  public void testOnResponseReturnsSamePacketInstance () {

    Packet<OrthodoxValue> packet = newPacket(PacketType.RESPONSE);

    Assert.assertSame(listener.onResponse(session, packet), packet);
  }

  public void testOnDeliveryReturnsSamePacketInstance () {

    Packet<OrthodoxValue> packet = newPacket(PacketType.DELIVERY);

    Assert.assertSame(listener.onDelivery(session, packet), packet);
  }

  public void testOnRequestAcceptsNullSender () {

    Packet<OrthodoxValue> packet = newPacket(PacketType.REQUEST);

    Assert.assertSame(listener.onRequest(null, packet), packet);
  }

  public void testOnResponseAcceptsNullSender () {

    Packet<OrthodoxValue> packet = newPacket(PacketType.RESPONSE);

    Assert.assertSame(listener.onResponse(null, packet), packet);
  }

  public void testOnDeliveryAcceptsNullSender () {

    Packet<OrthodoxValue> packet = newPacket(PacketType.DELIVERY);

    Assert.assertSame(listener.onDelivery(null, packet), packet);
  }

  public void testOnRequestAcceptsNullPacket () {

    Assert.assertNull(listener.onRequest(session, null));
  }

  public void testOnResponseAcceptsNullPacket () {

    Assert.assertNull(listener.onResponse(session, null));
  }

  public void testOnDeliveryAcceptsNullPacket () {

    Assert.assertNull(listener.onDelivery(session, null));
  }
}
