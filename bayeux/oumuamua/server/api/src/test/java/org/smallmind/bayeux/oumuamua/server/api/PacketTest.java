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
package org.smallmind.bayeux.oumuamua.server.api;

import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PacketTest {

  @SuppressWarnings("rawtypes")
  public void testSingleMessageConstructorWrapsInArray () {

    Packet packet = new Packet(PacketType.REQUEST, null, null, (Message)null);

    Assert.assertEquals(packet.getMessages().length, 1);
    Assert.assertNull(packet.getMessages()[0]);
  }

  @SuppressWarnings("rawtypes")
  public void testSingleMessageConstructorGetters () {

    Packet packet = new Packet(PacketType.REQUEST, "s1", null, (Message)null);

    Assert.assertSame(packet.getPacketType(), PacketType.REQUEST);
    Assert.assertEquals(packet.getSenderId(), "s1");
    Assert.assertNull(packet.getRoute());
  }

  @SuppressWarnings("rawtypes")
  public void testMultiMessageConstructorGetters () {

    Message[] messages = new Message[3];
    Packet packet = new Packet(PacketType.RESPONSE, null, null, messages);

    Assert.assertSame(packet.getPacketType(), PacketType.RESPONSE);
    Assert.assertSame(packet.getMessages(), messages);
    Assert.assertNull(packet.getSenderId());
  }

  @SuppressWarnings("rawtypes")
  public void testSenderIdNullIsStored () {

    Packet packet = new Packet(PacketType.DELIVERY, null, null, (Message)null);

    Assert.assertNull(packet.getSenderId());
  }

  @SuppressWarnings("rawtypes")
  public void testRouteIsStored () {

    Route route = new Route() {

      @Override
      public String getPath () {

        return "/test";
      }

      @Override
      public int size () {

        return 1;
      }

      @Override
      public int lastIndex () {

        return 0;
      }

      @Override
      public Segment getSegment (int index) {

        return null;
      }

      @Override
      public boolean isWild () {

        return false;
      }

      @Override
      public boolean isDeepWild () {

        return false;
      }

      @Override
      public boolean isMeta () {

        return false;
      }

      @Override
      public boolean isService () {

        return false;
      }

      @Override
      public boolean matchesPrefix (String... segments) {

        return false;
      }
    };

    Packet packet = new Packet(PacketType.REQUEST, null, route, (Message)null);

    Assert.assertSame(packet.getRoute(), route);
  }
}
