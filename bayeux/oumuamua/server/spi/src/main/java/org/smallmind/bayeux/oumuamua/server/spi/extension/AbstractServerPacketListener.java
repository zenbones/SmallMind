package org.smallmind.bayeux.oumuamua.server.spi.extension;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;

public abstract class AbstractServerPacketListener<V extends Value<V>> implements Server.PacketListener<V> {

  @Override
  public void onRequest (Session<V> sender, Packet<V> packet) {

  }

  @Override
  public void onResponse (Session<V> sender, Packet<V> packet) {

  }

  @Override
  public void onDelivery (Session<V> sender, Packet<V> packet) {

  }
}
