package org.smallmind.constellation.component;

import java.net.UnknownHostException;
import org.smallmind.cloud.cluster.ClusterEntity;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterMember;
import org.smallmind.cloud.cluster.broadcast.GossipClusterBroadcast;
import org.smallmind.cloud.multicast.EventMessageException;
import org.smallmind.quorum.pool.ConnectionPool;

public abstract class SmallMindContainer extends SmallMindComponent implements ClusterMember {

   private ClusterHub clusterHub;
   private ClusterEntity clusterEntity;
   private String globalPath;
   private String localPath;

   public SmallMindContainer (ClusterHub clusterHub, ClusterEntity clusterEntity, ConnectionPool contextPool)
      throws UnknownHostException {

      super(contextPool);

      this.clusterHub = clusterHub;
      this.clusterEntity = clusterEntity;

      globalPath = "container/" + clusterEntity.getClusterName() + "/global";
      localPath = "container/" + clusterEntity.getClusterName() + "/local/" + getHostName() + "/" + clusterEntity.getInstanceId();
   }

   public abstract void startUp ()
      throws Exception;

   public abstract void shutDown ()
      throws Exception;

   public ClusterEntity getClusterEntity () {

      return clusterEntity;
   }

   public String getGlobalNamespacePath () {

      return globalPath;
   }

   public String getLocalNamespacePath () {

      return localPath;
   }

   public void fireGossipBroadcast (GossipClusterBroadcast gossipClusterBroadcast)
      throws EventMessageException {

      clusterHub.fireEvent(gossipClusterBroadcast);
   }
}
