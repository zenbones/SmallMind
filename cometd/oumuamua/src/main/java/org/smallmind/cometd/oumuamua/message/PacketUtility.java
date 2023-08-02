package org.smallmind.cometd.oumuamua.message;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.Session;
import org.cometd.bayeux.server.ServerMessage;
import org.smallmind.cometd.oumuamua.SessionUtility;
import org.smallmind.cometd.oumuamua.channel.ChannelIdCache;
import org.smallmind.cometd.oumuamua.meta.DeliveryMessageSuccessOutView;
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
