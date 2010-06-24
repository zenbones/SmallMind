package org.smallmind.cloud.multicast.event;

public enum MessageStatus {

   MULTICAST, BROADCAST;

   public static MessageStatus getMessageStatus (int ordinal) {

      return MessageStatus.values()[ordinal];
   }

}
