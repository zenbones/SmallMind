/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.HashMap;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import org.smallmind.cloud.cluster.ClusterEndpoint;
import org.smallmind.cloud.cluster.ClusterHandle;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterManager;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.quorum.transport.FauxMethod;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.MissingInvocationException;

public class RemoteClusterManager implements ClusterManager<RemoteClusterProtocolDetails> {

  private static final String RMI_NAME_PREFIX = "org.smallmind.cloud.cluster.protocol.remote.";

  private final HashMap<ClusterEndpoint, RemoteClusterService> rmiServerMap;

  private ClusterHub clusterHub;
  private Proxy clusterProxy;
  private ClusterInterface<RemoteClusterProtocolDetails> clusterInterface;

  protected RemoteClusterManager (ClusterInterface<RemoteClusterProtocolDetails> clusterInterface) {

    RemoteClusterHandle clusterHandle;

    this.clusterInterface = clusterInterface;

    rmiServerMap = new HashMap<ClusterEndpoint, RemoteClusterService>();

    clusterHandle = new RemoteClusterHandle(this);
    clusterProxy = (Proxy)Proxy.newProxyInstance(clusterInterface.getClusterProtocolDetails().getServiceInterface().getClassLoader(), new Class[] {ClusterHandle.class, clusterInterface.getClusterProtocolDetails().getServiceInterface()}, clusterHandle);
  }

  public ClusterInterface<RemoteClusterProtocolDetails> getClusterInterface () {

    return clusterInterface;
  }

  public ClusterHandle getClusterHandle () {

    return (ClusterHandle)clusterProxy;
  }

  public void updateClusterStatus (ClusterEndpoint<RemoteClusterProtocolDetails> clusterEndpoint, int calibratedFreeCapacity)
    throws ClusterManagementException {

    InitialContext initContext;
    Context rmiContext;
    RemoteClusterService remoteHandle;

    synchronized (rmiServerMap) {
      if (!rmiServerMap.containsKey(clusterEndpoint)) {
        try {
          initContext = new InitialContext();
          rmiContext = (Context)initContext.lookup("rmi://" + clusterEndpoint.getHostAddress());
          remoteHandle = (RemoteClusterService)PortableRemoteObject.narrow(rmiContext.lookup(RMI_NAME_PREFIX + clusterInterface.getClusterName() + ".instance." + clusterEndpoint.getClusterInstance().getInstanceId()), RemoteClusterService.class);
          rmiContext.close();
          initContext.close();
        }
        catch (NamingException namingException) {
          throw new ClusterManagementException(namingException);
        }

        rmiServerMap.put(clusterEndpoint, remoteHandle);
      }
    }

    clusterInterface.getClusterPivot().updateClusterStatus(clusterEndpoint, calibratedFreeCapacity);
  }

  public void removeClusterMember (ClusterEndpoint<RemoteClusterProtocolDetails> clusterEndpoint) {

    synchronized (rmiServerMap) {
      if (rmiServerMap.containsKey(clusterEndpoint)) {
        rmiServerMap.remove(clusterEndpoint);
        clusterInterface.getClusterPivot().removeClusterMember(clusterEndpoint);
      }
    }
  }

  public Object invoke (Method method, Object[] args)
    throws Exception {

    Object[] pivotParameters;
    ClusterEndpoint clusterEndpoint = null;
    RemoteClusterService remoteHandle;

    if (args == null) {
      pivotParameters = new Object[1];
    }
    else {
      pivotParameters = new Object[args.length + 1];
    }

    pivotParameters[0] = method;
    if (args != null) {
      System.arraycopy(args, 0, pivotParameters, 1, args.length);
    }

    while (true) {
      synchronized (rmiServerMap) {
        if ((clusterEndpoint = clusterInterface.getClusterPivot().nextRequestAddress(pivotParameters, clusterEndpoint)) == null) {
          throw new ClusterManagementException("No server is currently available for requests to ClusterInterface (%s)", clusterInterface);
        }
        remoteHandle = rmiServerMap.get(clusterEndpoint);
      }

      if (remoteHandle != null) {
        try {
          return remoteHandle.remoteInvocation(new InvocationSignal(ContextFactory.getExpectedContexts(clusterInterface.getClusterProtocolDetails().getServiceInterface()), new FauxMethod(method), args));
        }
        catch (MissingInvocationException missingInvocationException) {
          throw new ClusterManagementException("Could not invoke method (%s) on the remote cluster", method.getName());
        }
        catch (RemoteException remoteException) {
          clusterHub.logError(remoteException);
          removeClusterMember(clusterEndpoint);
        }
      }
      else {
        clusterHub.logError(new ClusterManagementException("Pivot ClusterEndpoint/remote handle mismatch on cluster (%s)", clusterEndpoint));
        removeClusterMember(clusterEndpoint);
      }
    }
  }
}
