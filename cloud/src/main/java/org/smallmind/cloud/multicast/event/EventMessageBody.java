package org.smallmind.cloud.multicast.event;

public class EventMessageBody extends EventMessage {

   public EventMessageBody (byte[] messageId, int messageIndex, byte[] messageData) {

      super(messageId, MessageType.DATA, messageData.length, messageData.length + 4);

      putInt(messageIndex);
      put(messageData);
   }

}
