package org.smallmind.cloud.multicast.event;

public class EventMessageHeader extends EventMessage {

   public EventMessageHeader (byte[] messageId, int messageLength) {

      super(messageId, MessageType.HEADER, messageLength, 0);
   }

}
