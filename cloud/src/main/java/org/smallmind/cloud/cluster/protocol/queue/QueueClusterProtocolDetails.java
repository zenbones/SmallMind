package org.smallmind.cloud.cluster.protocol.queue;

import org.smallmind.cloud.cluster.protocol.ClusterProtocol;
import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;
import org.smallmind.cloud.transport.messaging.MessagingConnectionDetails;

public class QueueClusterProtocolDetails extends ClusterProtocolDetails {

   private Class serviceInterface;
   private MessagingConnectionDetails messagingConnectionDetails;

   public QueueClusterProtocolDetails (Class serviceInterface, MessagingConnectionDetails messagingConnectionDetails) {

      super(ClusterProtocol.QUEUE);

      this.serviceInterface = serviceInterface;
      this.messagingConnectionDetails = messagingConnectionDetails;
   }

   public Class getServiceInterface () {

      return serviceInterface;
   }

   public MessagingConnectionDetails getConnectionDetails () {

      return messagingConnectionDetails;
   }
}
