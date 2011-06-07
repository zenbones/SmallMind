/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.cloud.cluster.protocol;

import org.smallmind.cloud.cluster.ClusterManagerBuilder;
import org.smallmind.cloud.cluster.ClusterServiceBuilder;
import org.smallmind.cloud.cluster.protocol.queue.QueueClusterManagerBuilder;
import org.smallmind.cloud.cluster.protocol.queue.QueueClusterServiceBuilder;
import org.smallmind.cloud.cluster.protocol.remote.RemoteClusterManagerBuilder;
import org.smallmind.cloud.cluster.protocol.remote.RemoteClusterServiceBuilder;
import org.smallmind.cloud.cluster.protocol.socket.SocketClusterManagerBuilder;
import org.smallmind.cloud.cluster.protocol.socket.SocketClusterServiceBuilder;

public enum ClusterProtocol {

   REMOTE(RemoteClusterServiceBuilder.class, RemoteClusterManagerBuilder.class), QUEUE(QueueClusterServiceBuilder.class, QueueClusterManagerBuilder.class), SOCKET(SocketClusterServiceBuilder.class, SocketClusterManagerBuilder.class);

   private Class<? extends ClusterServiceBuilder<? extends ClusterProtocolDetails>> serviceBuilderClass;
   private Class<? extends ClusterManagerBuilder<? extends ClusterProtocolDetails>> managerBuilderClass;

   private ClusterProtocol (Class<? extends ClusterServiceBuilder<? extends ClusterProtocolDetails>> serviceBuilderClass, Class<? extends ClusterManagerBuilder<? extends ClusterProtocolDetails>> managerBuilderClass) {

      this.serviceBuilderClass = serviceBuilderClass;
      this.managerBuilderClass = managerBuilderClass;
   }

   public Class<? extends ClusterServiceBuilder<? extends ClusterProtocolDetails>> getServiceBuilderClass () {

      return serviceBuilderClass;
   }

   public Class<? extends ClusterManagerBuilder<? extends ClusterProtocolDetails>> getManagerBuilderClass () {

      return managerBuilderClass;
   }
}
