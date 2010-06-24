package org.smallmind.cloud.cluster.protocol.remote;

import java.rmi.Remote;
import org.smallmind.cloud.transport.InvocationSignal;

public interface RemoteClusterService extends Remote {

   public abstract Object remoteInvocation (InvocationSignal invocationSignal)
      throws Exception;

}
