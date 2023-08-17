package org.smallmind.bayeux.oumuamua.server.impl;

import org.smallmind.bayeux.oumuamua.common.api.Message;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.spi.json.BodyDouble;

public class PacketUtility {

  public static Packet freezePacket (Packet packet) {

    Message[] frozenMessages = new Message[packet.getMessages().length];
    int index = 0;

    for (Message message : packet.getMessages()) {
      frozenMessages[index++] = new Message(message.getMessageType(), new BodyDouble<>(message.getBody()));
    }

    return new Packet(packet.getChannel(), frozenMessages);
  }
}
