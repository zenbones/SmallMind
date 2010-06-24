package org.smallmind.cloud.multicast.event;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MulticastEvent implements java.io.Serializable {

   private InetAddress hostAddress;

   public MulticastEvent ()
      throws UnknownHostException {

      hostAddress = InetAddress.getLocalHost();
   }

   public InetAddress getHostAddress () {

      return hostAddress;
   }

}
