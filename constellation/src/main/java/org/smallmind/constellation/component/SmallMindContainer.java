/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
