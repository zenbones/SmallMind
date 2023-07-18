package org.smallmind.cometd.oumuamua.message;

import org.smallmind.cometd.oumuamua.OumuamuaServerSession;

public class OumuamuaPacket {

  private final OumuamuaServerSession sender;
  private final MapLike[] messages;

  public OumuamuaPacket (OumuamuaServerSession sender, MapLike... messages) {

    this.sender = sender;
    this.messages = messages;
  }

  public OumuamuaServerSession getSender () {

    return sender;
  }

  public MapLike[] getMessages () {

    return messages;
  }
}
