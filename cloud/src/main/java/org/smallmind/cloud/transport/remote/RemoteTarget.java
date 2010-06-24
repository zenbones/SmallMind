package org.smallmind.cloud.transport.remote;

import java.rmi.Remote;
import org.smallmind.cloud.transport.InvocationSignal;

public interface RemoteTarget extends Remote {

   public abstract Object remoteInvocation (InvocationSignal invocationSignal)
      throws Exception;

}
