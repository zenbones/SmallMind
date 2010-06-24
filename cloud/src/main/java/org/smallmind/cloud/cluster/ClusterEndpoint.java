package org.smallmind.cloud.cluster;

import java.net.InetAddress;

public class ClusterEndpoint implements java.io.Serializable {

   private InetAddress inetAddress;
   private ClusterInstance clusterInstance;

   public ClusterEndpoint (InetAddress inetAddress, ClusterInstance clusterInstance) {

      this.inetAddress = inetAddress;
      this.clusterInstance = clusterInstance;
   }

   public InetAddress getHostAddress () {

      return inetAddress;
   }

   public String getHostName () {

      return inetAddress.getHostName();
   }

   public ClusterInstance getClusterInstance () {

      return clusterInstance;
   }

   public String toString () {

      StringBuilder endpointBuilder;

      endpointBuilder = new StringBuilder("ClusterEndpoint [");
      endpointBuilder.append(inetAddress.toString());
      endpointBuilder.append(":");
      endpointBuilder.append(clusterInstance.toString());
      endpointBuilder.append("]");

      return endpointBuilder.toString();
   }

   public int hashCode () {

      return (inetAddress.hashCode() ^ clusterInstance.hashCode());
   }

   public boolean equals (Object obj) {

      if (obj instanceof ClusterEndpoint) {
         if (inetAddress.equals(((ClusterEndpoint)obj).getHostAddress())) {
            if (clusterInstance.equals(((ClusterEndpoint)obj).getClusterInstance())) {
               return true;
            }
         }
      }

      return false;
   }
}
