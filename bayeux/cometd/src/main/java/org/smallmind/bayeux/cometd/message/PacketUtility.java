/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.bayeux.cometd.message;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.ServerMessage;
import org.smallmind.bayeux.cometd.channel.ChannelIdCache;
import org.smallmind.bayeux.cometd.meta.DeliveryMessageSuccessOutView;
import org.smallmind.bayeux.cometd.session.SessionUtility;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class PacketUtility {

  public static OumuamuaPacket regenerate (OumuamuaPacket packet, MapLike... messages) {

    if ((messages == null) || (messages.length == 0)) {

      return null;
    } else if (PacketType.LAZY.equals(packet.getType())) {

      return new OumuamuaLazyPacket(packet.getSender(), packet.getChannelId(), ((OumuamuaLazyPacket)packet).getLazyTimestamp(), messages);
    } else {

      return new OumuamuaPacket(packet.getSender(), packet.getChannelId(), messages);
    }
  }

  public static OumuamuaPacket wrapDeliveryPacket (Session sender, String channel, Object data) {

    MapLike mapLike = new MapLike((ObjectNode)JsonCodec.writeAsJsonNode(new DeliveryMessageSuccessOutView().setChannel(channel)));

    mapLike.put(Message.DATA_FIELD, data);

    return new OumuamuaPacket(SessionUtility.from(sender), ChannelIdCache.generate(channel), mapLike);
  }

  public static OumuamuaPacket wrapDeliveryPacket (Session sender, ServerMessage.Mutable message) {

    MapLike mapLike = OumuamuaServerMessage.class.isAssignableFrom(message.getClass()) ? ((OumuamuaServerMessage)message).getMapLike() : new MapLike((ObjectNode)JsonCodec.writeAsJsonNode(message));

    return new OumuamuaPacket(SessionUtility.from(sender), ChannelIdCache.generate(message.getChannel()), mapLike);
  }
}
