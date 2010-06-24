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
