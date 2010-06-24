package org.smallmind.cloud.cluster;

public class ClusterEntity {

   private String clusterName;
   private String instanceId;

   public ClusterEntity (String clusterName, String instanceId) {

      this.clusterName = clusterName;
      this.instanceId = instanceId;
   }

   public String getClusterName () {

      return clusterName;
   }

   public String getInstanceId () {

      return instanceId;
   }
}
