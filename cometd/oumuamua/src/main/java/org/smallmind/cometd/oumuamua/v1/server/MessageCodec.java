package org.smallmind.cometd.oumuamua.v1.server;

import org.smallmind.cometd.oumuamua.v1.Message;

public interface MessageCodec {

  Message Encode (byte[] buffer);

  Message decode (byte[] buffer);
}
