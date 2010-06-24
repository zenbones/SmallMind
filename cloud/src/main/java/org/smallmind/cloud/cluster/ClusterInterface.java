package org.smallmind.cloud.cluster;

import java.io.Serializable;
import org.smallmind.cloud.cluster.pivot.ClusterPivot;
import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;

public class ClusterInterface<D extends ClusterProtocolDetails> implements Serializable {

   private String clusterName;
   private ClusterPivot clusterPivot;
   private D clusterProtocolDetails;

   public ClusterInterface (String clusterName, ClusterPivot clusterPivot, D clusterProtocolDetails) {

      this.clusterName = clusterName;
      this.clusterPivot = clusterPivot;
      this.clusterProtocolDetails = clusterProtocolDetails;
   }

   public String getClusterName () {

      return clusterName;
   }

   public ClusterPivot getClusterPivot () {

      return clusterPivot;
   }

   public D getClusterProtocolDetails () {

      return clusterProtocolDetails;
   }

   public String toString () {

      StringBuilder interfaceBuilder;

      interfaceBuilder = new StringBuilder("ClusterInterface [");
      interfaceBuilder.append(clusterName);
      interfaceBuilder.append(':');
      interfaceBuilder.append(clusterProtocolDetails.getClusterProtocol());
      interfaceBuilder.append("]");

      return interfaceBuilder.toString();
   }

   public int hashCode () {

      return (clusterName.hashCode() ^ clusterProtocolDetails.getClass().hashCode());
   }

   public boolean equals (Object obj) {

      if (obj instanceof ClusterInterface) {
         if (clusterName.equals(((ClusterInterface)obj).getClusterName())) {
            if (clusterProtocolDetails.getClusterProtocol().equals(((ClusterInterface)obj).getClusterProtocolDetails().getClusterProtocol())) {
               return true;
            }
         }
      }

      return false;
   }
}
