package org.smallmind.bayeux.oumuamua.server.impl;

import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Session;
import org.smallmind.bayeux.oumuamua.server.spi.AbstractAttributed;

public class OumuamuaSession extends AbstractAttributed implements Session {

  @Override
  public void addListener (Listener listener) {

  }

  @Override
  public void removeListener (Listener listener) {

  }

  @Override
  public String getId () {

    return null;
  }

  @Override
  public boolean isHandshook () {

    return false;
  }

  @Override
  public boolean isConnected () {

    return false;
  }

  @Override
  public void deliver (Packet packet) {

    Packet frozenPacket = PacketUtility.freezePacket(packet);
  }
}
