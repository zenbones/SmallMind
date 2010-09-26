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
package org.smallmind.cloud.cluster.protocol.queue;

import java.lang.reflect.Proxy;
import javax.jms.JMSException;
import org.smallmind.cloud.cluster.ClusterEndpoint;
import org.smallmind.cloud.cluster.ClusterHandle;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterManager;
import org.smallmind.cloud.transport.messaging.MessagingInvocationHandler;
import org.smallmind.cloud.transport.messaging.MessagingTransmitter;

public class QueueClusterManager implements ClusterManager<QueueClusterProtocolDetails> {

   private ClusterHub clusterHub;
   private Proxy clusterProxy;
   private ClusterInterface<QueueClusterProtocolDetails> clusterInterface;
   private MessagingTransmitter messagingTransmitter;

   public QueueClusterManager (ClusterHub clusterHub, ClusterInterface<QueueClusterProtocolDetails> clusterInterface)
      throws ClusterManagementException {

      QueueClusterHandle clusterHandle;

      this.clusterHub = clusterHub;
      this.clusterInterface = clusterInterface;

      try {
         messagingTransmitter = new MessagingTransmitter(clusterInterface.getClusterProtocolDetails().getConnectionDetails());
      }
      catch (Exception exception) {
         throw new ClusterManagementException(exception);
      }

      clusterHandle = new QueueClusterHandle(new MessagingInvocationHandler(messagingTransmitter, clusterInterface.getClusterProtocolDetails().getServiceInterface()));
      clusterProxy = (Proxy)Proxy.newProxyInstance(clusterInterface.getClusterProtocolDetails().getServiceInterface().getClassLoader(), new Class[] {ClusterHandle.class, clusterInterface.getClusterProtocolDetails().getServiceInterface()}, clusterHandle);
   }

   public ClusterInterface<QueueClusterProtocolDetails> getClusterInterface () {

      return clusterInterface;
   }

   public ClusterHandle getClusterHandle ()
      throws ClusterManagementException {

      return (ClusterHandle)clusterProxy;
   }

   public void updateClusterStatus (ClusterEndpoint clusterEndpoint, int calibratedFreeCapacity) {
   }

   public void removeClusterMember (ClusterEndpoint clusterEndpoint) {
   }

   public void finalize () {

      try {
         messagingTransmitter.close();
      }
      catch (JMSException j) {
         clusterHub.logError(j);
      }
   }
}
