package org.smallmind.cloud.cluster.protocol.remote;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.smallmind.cloud.cluster.ClusterHandle;

public class RemoteClusterHandle implements ClusterHandle, InvocationHandler {

   private RemoteClusterManager remoteClusterManager;

   public RemoteClusterHandle (RemoteClusterManager remoteClusterManager) {

      this.remoteClusterManager = remoteClusterManager;
   }

   public Object invoke (Object proxy, Method method, Object[] args)
      throws Exception {

      return remoteClusterManager.invoke(method, args);
   }
}
