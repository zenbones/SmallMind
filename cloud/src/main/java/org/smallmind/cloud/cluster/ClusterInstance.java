package org.smallmind.cloud.cluster;

import java.io.Serializable;
import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;

public class ClusterInstance<D extends ClusterProtocolDetails> implements Serializable {

   private ClusterInterface<D> clusterInterface;
   private String instanceId;

   public ClusterInstance (ClusterInterface<D> clusterInterface, String instanceId) {

      this.clusterInterface = clusterInterface;
      this.instanceId = instanceId;
   }

   public ClusterInterface<D> getClusterInterface () {

      return clusterInterface;
   }

   public String getInstanceId () {

      return instanceId;
   }

   public String toString () {

      StringBuilder idBuilder;

      idBuilder = new StringBuilder("ClusterInstance [");
      idBuilder.append(clusterInterface.toString());
      idBuilder.append(':');
      idBuilder.append(instanceId);
      idBuilder.append("]");

      return idBuilder.toString();
   }

   public int hashCode () {

      return (clusterInterface.hashCode() ^ instanceId.hashCode());
   }

   public boolean equals (Object obj) {

      if (obj instanceof ClusterInstance) {
         if (clusterInterface.equals(((ClusterInstance)obj).getClusterInterface())) {
            if (instanceId.equals(((ClusterInstance)obj).getInstanceId())) {
               return true;
            }
         }
      }

      return false;
   }

}
