package org.smallmind.cloud.multicast.event;

import java.net.UnknownHostException;

public class MessageMulticastEvent extends MulticastEvent {

   private String message;

   public MessageMulticastEvent (String message)
      throws UnknownHostException {

      super();

      this.message = message;
   }

   public String getMessage () {

      return message;
   }

}
