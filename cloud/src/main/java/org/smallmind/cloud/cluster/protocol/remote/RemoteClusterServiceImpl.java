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
package org.smallmind.cloud.cluster.protocol.remote;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInstance;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterMember;
import org.smallmind.cloud.cluster.ClusterService;
import org.smallmind.cloud.cluster.broadcast.ServiceClusterBroadcast;
import org.smallmind.cloud.multicast.EventMessageException;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.MethodInvoker;

public class RemoteClusterServiceImpl extends UnicastRemoteObject implements RemoteClusterService, ClusterService<RemoteClusterProtocolDetails> {

   private static final String RMI_NAME_PREFIX = "org.smallmind.cloud.cluster.protocol.remote.";

   private ClusterHub clusterHub;
   private ClusterMember clusterMember = null;
   private ClusterInstance<RemoteClusterProtocolDetails> clusterInstance;
   private MethodInvoker methodInvoker;
   private boolean open = true;

   public RemoteClusterServiceImpl (ClusterHub clusterHub, ClusterInstance<RemoteClusterProtocolDetails> clusterInstance)
      throws RemoteException {

      this.clusterHub = clusterHub;
      this.clusterInstance = clusterInstance;
   }

   public ClusterInstance<RemoteClusterProtocolDetails> getClusterInstance () {

      return clusterInstance;
   }

   public void bindClusterMember (ClusterMember clusterMember)
      throws ClusterManagementException {

      this.clusterMember = clusterMember;

      try {
         methodInvoker = new MethodInvoker(clusterMember, new Class[] {clusterInstance.getClusterInterface().getClusterProtocolDetails().getServiceInterface()});
      }
      catch (NoSuchMethodException noSuchMethodException) {
         throw new ClusterManagementException(noSuchMethodException);
      }
   }

   public Object remoteInvocation (InvocationSignal invocationSignal)
      throws Exception {

      if (methodInvoker == null) {
         throw new IllegalStateException("No ClusterMember(" + clusterInstance.getClusterInterface().getClusterProtocolDetails().getServiceInterface().getCanonicalName() + ") has been bound to this service representation");
      }

      return methodInvoker.remoteInvocation(invocationSignal);
   }

   public synchronized void fireServiceBroadcast (ServiceClusterBroadcast serviceClusterBroadcast)
      throws EventMessageException {

      if (!open) {
         throw new IllegalStateException("The service has already been closed");
      }

      clusterHub.fireEvent(serviceClusterBroadcast);
   }

   public synchronized void handleServiceBroadcast (ServiceClusterBroadcast serviceClusterBroadcast) {

      if (!open) {
         throw new IllegalStateException("The service has already been closed");
      }

      clusterMember.handleServiceBroadcast(serviceClusterBroadcast);
   }

   public synchronized void start ()
      throws ClusterManagementException {

      if (!open) {
         try {
            Naming.rebind(RMI_NAME_PREFIX + clusterInstance.getClusterInterface().getClusterName() + ".instance." + clusterInstance.getInstanceId(), this);
         }
         catch (Exception exception) {
            throw new ClusterManagementException(exception);
         }

         clusterHub.addClusterService(this);
         open = true;
      }
   }

   public synchronized void stop () {

      if (open) {
         open = false;
         clusterHub.removeClusterService(getClusterInstance());

         try {
            Naming.unbind(RMI_NAME_PREFIX + clusterInstance.getClusterInterface().getClusterName() + ".instance." + clusterInstance.getInstanceId());
         }
         catch (Exception exception) {
            clusterHub.logError(exception);
         }
      }
   }

}
