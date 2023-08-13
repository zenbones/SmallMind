package org.smallmind.bayeux.oumuamua.api.server;

import org.smallmind.bayeux.oumuamua.api.Body;
import org.smallmind.bayeux.oumuamua.api.Message;
import org.smallmind.bayeux.oumuamua.api.MessageType;

// Valid json encodings are all versions of UTF, but this specification uses Java standard UTF-8
public interface MessageCodec {

  byte[] encode (Body<?> body);

  String toJson (Body<?> body);

  Message decode (MessageType messageType, byte[] buffer);

  Message createMessage (MessageType messageType);

  Message generateMessage (MessageType messageType, Object object);
}
