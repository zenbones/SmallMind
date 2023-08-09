package org.smallmind.cometd.oumuamua.v1;

public interface Message {

  MessageType getType ();

  Body getBody ();
}
