package org.smallmind.cloud.multicast.event;

public enum MessageType {

   HEADER, DATA;

   public static MessageType getMessageType (int ordinal) {

      return MessageType.values()[ordinal];
   }

}
