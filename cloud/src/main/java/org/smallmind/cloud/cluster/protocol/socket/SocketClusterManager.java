package org.smallmind.cloud.cluster.protocol.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import org.smallmind.cloud.cluster.ClusterEndpoint;
import org.smallmind.cloud.cluster.ClusterHandle;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterManager;

public class SocketClusterManager implements ClusterManager<SocketClusterProtocolDetails> {

   private ClusterHub clusterHub;
   private SocketClusterHandle socketClusterHandle;
   private ClusterInterface<SocketClusterProtocolDetails> clusterInterface;

   public SocketClusterManager (ClusterHub clusterHub, ClusterInterface<SocketClusterProtocolDetails> clusterInterface) {

      this.clusterHub = clusterHub;
      this.clusterInterface = clusterInterface;

      socketClusterHandle = new SocketClusterHandle(this);
   }

   public ClusterInterface<SocketClusterProtocolDetails> getClusterInterface () {

      return clusterInterface;
   }

   public ClusterHandle getClusterHandle () {

      return socketClusterHandle;
   }

   public void updateClusterStatus (ClusterEndpoint clusterEndpoint, int calibratedFreeCapacity) {

      clusterInterface.getClusterPivot().updateClusterStatus(clusterEndpoint, calibratedFreeCapacity);
   }

   public void removeClusterMember (ClusterEndpoint clusterEndpoint) {

      clusterInterface.getClusterPivot().removeClusterMember(clusterEndpoint);
   }

   private int getServicePort (String instanceId)
      throws ClusterManagementException {

      return clusterInterface.getClusterProtocolDetails().getPortMapper().mapPort(instanceId);
   }

   public SocketChannel connect (Object[] parameters)
      throws ClusterManagementException {

      ClusterEndpoint clusterEndpoint = null;
      SocketChannel serviceSocketChannel;
      SocketAddress serviceSocketAddress;

      while (true) {
         if ((clusterEndpoint = clusterInterface.getClusterPivot().nextRequestAddress(parameters, clusterEndpoint)) == null) {
            throw new ClusterManagementException("No server is currently available for requests to %s (%s)", ClusterInterface.class.getSimpleName(), clusterInterface);
         }

         serviceSocketAddress = new InetSocketAddress(clusterEndpoint.getHostAddress(), getServicePort(clusterEndpoint.getClusterInstance().getInstanceId()));
         try {
            serviceSocketChannel = SocketChannel.open(serviceSocketAddress);
            return serviceSocketChannel;
         }
         catch (IOException ioException) {
            clusterHub.logError(ioException);
            removeClusterMember(clusterEndpoint);
         }
      }
   }

}
