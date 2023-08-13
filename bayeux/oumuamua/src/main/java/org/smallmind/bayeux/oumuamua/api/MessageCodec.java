package org.smallmind.bayeux.oumuamua.api;

public interface MessageCodec {

  String encodeMessage (Message message);

  Message decodeMessage (String json);

  Message createMessage ();

  Message generateMessage (Object object);
}
