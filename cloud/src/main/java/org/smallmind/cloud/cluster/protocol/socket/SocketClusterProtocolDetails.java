package org.smallmind.cloud.cluster.protocol.socket;

import org.smallmind.cloud.cluster.protocol.ClusterProtocol;
import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;

public class SocketClusterProtocolDetails extends ClusterProtocolDetails {

   private PortMapper portMapper;

   public SocketClusterProtocolDetails (PortMapper portMapper) {

      super(ClusterProtocol.SOCKET);

      this.portMapper = portMapper;
   }

   public PortMapper getPortMapper () {

      return portMapper;
   }
}
