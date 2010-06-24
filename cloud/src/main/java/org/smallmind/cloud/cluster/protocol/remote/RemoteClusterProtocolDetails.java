package org.smallmind.cloud.cluster.protocol.remote;

import org.smallmind.cloud.cluster.protocol.ClusterProtocol;
import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;

public class RemoteClusterProtocolDetails extends ClusterProtocolDetails {

   private Class serviceInterface;

   public RemoteClusterProtocolDetails (Class serviceInterface) {

      super(ClusterProtocol.REMOTE);

      this.serviceInterface = serviceInterface;
   }

   public Class getServiceInterface () {

      return serviceInterface;
   }
}
