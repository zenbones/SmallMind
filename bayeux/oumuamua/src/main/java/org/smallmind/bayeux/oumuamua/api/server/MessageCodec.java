package org.smallmind.bayeux.oumuamua.api.server;

import java.nio.charset.Charset;
import org.smallmind.bayeux.oumuamua.api.Body;
import org.smallmind.bayeux.oumuamua.api.Message;
import org.smallmind.bayeux.oumuamua.api.MessageType;

public interface MessageCodec {

  byte[] encode (Body<?> body);

  byte[] encode (Body<?> body, Charset charset);

  String toJson (Body<?> body);

  Message decode (MessageType messageType, byte[] buffer);

  Message decode (MessageType messageType, byte[] buffer, Charset charset);

  Message createMessage (MessageType messageType);

  Message generateMessage (MessageType messageType, Object object);
}
