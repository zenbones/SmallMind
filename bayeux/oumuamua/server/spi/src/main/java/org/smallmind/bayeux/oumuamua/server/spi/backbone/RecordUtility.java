package org.smallmind.bayeux.oumuamua.server.spi.backbone;

import java.io.IOException;
import org.smallmind.bayeux.oumuamua.common.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.spi.json.PacketUtility;

public class RecordUtility {

  public static <V extends Value<V>> byte[] toBytes (Packet<V> packet)
    throws IOException {

    StringBuilder packetBuilder = PacketUtility.encode(packet);

    return packetBuilder.insert(0, "\",\"messages\":").insert(0, packet.getRoute().getPath()).insert(0, "{\"path\":\"").append("}").toString().getBytes();
  }
}
